package com.iiit.oms.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iiit.oms.db.util.PostgresConnectionFactory;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * Weekly WORM archiver for the order_audit_log table.
 *
 * On each Sunday 02:00 UTC run:
 *   1. Queries records older than 7 days with archived_at IS NULL
 *   2. Serialises to newline-delimited JSON, GZip-compressed
 *   3. Computes SHA-256 checksum of the written file
 *   4. Inserts run metadata into audit_archive_runs
 *   5. Stamps archived_at on processed rows
 *
 * A separate monthly task emits a WARNING for archive runs older than 7 years
 * (WORM: records are never deleted, only flagged for operator review).
 */
public class AuditLogArchiver {

    private static final Logger LOGGER = Logger.getLogger(AuditLogArchiver.class.getName());
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter
            .ofPattern("yyyyMMdd_HHmmssSSS").withZone(ZoneOffset.UTC);
    private static final long SEVEN_YEARS_DAYS = 365L * 7;

    private final PostgresConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;
    private final Path archiveDir;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "audit-archiver");
        t.setDaemon(true);
        return t;
    });

    public AuditLogArchiver(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        this.objectMapper = new ObjectMapper();
        String dir = System.getenv("OMS_ARCHIVE_DIR");
        this.archiveDir = Paths.get(dir != null && !dir.isBlank() ? dir : "audit-archives");
    }

    public void start() {
        long weeklyDelay  = secondsUntilNextSundayAt2Am();
        long monthlyDelay = secondsUntilNextFirstOfMonth();

        scheduler.scheduleAtFixedRate(this::runWeeklyArchive,
                weeklyDelay, TimeUnit.DAYS.toSeconds(7), TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(this::runMonthlyRetentionAlert,
                monthlyDelay, TimeUnit.DAYS.toSeconds(30), TimeUnit.SECONDS);

        LOGGER.info(String.format(
                "AuditLogArchiver started — first archive in %ds (next Sunday 02:00 UTC), " +
                "retention alert in %ds", weeklyDelay, monthlyDelay));
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    // Visible for testing
    public void runWeeklyArchive() {
        try {
            Files.createDirectories(archiveDir);
            List<Map<String, Object>> records = fetchUnarchived();
            if (records.isEmpty()) {
                LOGGER.info("AuditLogArchiver: no unarchived records older than 7 days — nothing to do.");
                return;
            }

            String filename = "audit_" + FILE_TS.format(Instant.now()) + ".json.gz";
            Path outPath = archiveDir.resolve(filename);

            writeGzipNdjson(outPath, records);
            String checksum = sha256Hex(outPath);
            List<Long> ids = extractIds(records);

            insertArchiveRun(records.size(), outPath.toString(), checksum);
            markArchived(ids);

            LOGGER.info(String.format(
                    "AuditLogArchiver: archived %d records → %s (SHA-256: %s)",
                    records.size(), outPath, checksum));
        } catch (Exception ex) {
            LOGGER.severe("AuditLogArchiver weekly run failed: " + ex.getMessage());
        }
    }

    private void runMonthlyRetentionAlert() {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT run_id, archived_at, file_path FROM audit_archive_runs " +
                     "WHERE archived_at < NOW() - INTERVAL '" + SEVEN_YEARS_DAYS + "' DAY")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LOGGER.warning(String.format(
                            "WORM RETENTION: Archive run %d (file: %s, archived: %s) exceeds 7-year retention. " +
                            "Operator review required — records must NOT be deleted per WORM policy.",
                            rs.getLong("run_id"), rs.getString("file_path"), rs.getTimestamp("archived_at")));
                }
            }
        } catch (Exception ex) {
            LOGGER.severe("AuditLogArchiver monthly retention check failed: " + ex.getMessage());
        }
    }

    private List<Map<String, Object>> fetchUnarchived() throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        String sql = "SELECT id, order_id, from_status, to_status, occurred_at, actor, details " +
                     "FROM order_audit_log " +
                     "WHERE occurred_at < NOW() - INTERVAL '7' DAY " +
                     "AND archived_at IS NULL " +
                     "ORDER BY occurred_at ASC";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id",          rs.getLong("id"));
                row.put("orderId",     rs.getString("order_id"));
                row.put("fromStatus",  rs.getString("from_status"));
                row.put("toStatus",    rs.getString("to_status"));
                row.put("occurredAt",  rs.getTimestamp("occurred_at").toInstant().toString());
                row.put("actor",       rs.getString("actor"));
                row.put("details",     rs.getString("details"));
                results.add(row);
            }
        }
        return results;
    }

    private void writeGzipNdjson(Path out, List<Map<String, Object>> records) throws IOException {
        try (OutputStream fos = Files.newOutputStream(out);
             GZIPOutputStream gzip = new GZIPOutputStream(fos);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzip))) {
            for (Map<String, Object> record : records) {
                writer.write(objectMapper.writeValueAsString(record));
                writer.newLine();
            }
        }
    }

    private String sha256Hex(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) md.update(buf, 0, read);
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private List<Long> extractIds(List<Map<String, Object>> records) {
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> r : records) ids.add((Long) r.get("id"));
        return ids;
    }

    private void insertArchiveRun(int count, String filePath, String checksum) throws SQLException {
        String sql = "INSERT INTO audit_archive_runs(records_count, file_path, checksum_sha256) VALUES(?,?,?)";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, count);
            stmt.setString(2, filePath);
            stmt.setString(3, checksum);
            stmt.executeUpdate();
        }
    }

    private void markArchived(List<Long> ids) throws SQLException {
        if (ids.isEmpty()) return;
        String inClause = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "UPDATE order_audit_log SET archived_at = NOW() WHERE id IN (" + inClause + ")";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) stmt.setLong(i + 1, ids.get(i));
            stmt.executeUpdate();
        }
    }

    private static long secondsUntilNextSundayAt2Am() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime next = now.toLocalDate().atTime(2, 0).atZone(ZoneOffset.UTC);
        // Advance to Sunday (DayOfWeek.SUNDAY = 7)
        while (next.getDayOfWeek() != DayOfWeek.SUNDAY || !next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return Duration.between(now, next).getSeconds();
    }

    private static long secondsUntilNextFirstOfMonth() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime next = now.toLocalDate().withDayOfMonth(1)
                .plusMonths(1).atStartOfDay(ZoneOffset.UTC);
        return Duration.between(now, next).getSeconds();
    }
}
