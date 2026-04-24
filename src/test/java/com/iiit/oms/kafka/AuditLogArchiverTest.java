package com.iiit.oms.kafka;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import static java.sql.DriverManager.getConnection;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuditLogArchiver using an in-process H2 database.
 *
 * H2 is used in PostgreSQL compatibility mode so the SQL matches production.
 */
class AuditLogArchiverTest {

    @TempDir
    Path tempDir;

    private Connection h2;
    private TestConnectionFactory connFactory;
    private AuditLogArchiver archiver;

    private String h2Url;

    @BeforeEach
    void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        // Named in-memory DB with CLOSE_DELAY=-1 so it persists between connections
        h2Url = "jdbc:h2:mem:audit_test_" + System.nanoTime() +
                ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        h2 = DriverManager.getConnection(h2Url, "sa", "");

        try (Statement s = h2.createStatement()) {
            s.execute("CREATE TABLE order_audit_log (" +
                    "  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "  order_id   VARCHAR(64)," +
                    "  from_status VARCHAR(32)," +
                    "  to_status   VARCHAR(32)," +
                    "  occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()," +
                    "  actor       VARCHAR(64)," +
                    "  details     VARCHAR(512)," +
                    "  archived_at TIMESTAMP WITH TIME ZONE" +
                    ")");
            s.execute("CREATE TABLE audit_archive_runs (" +
                    "  run_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "  archived_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()," +
                    "  records_count INT NOT NULL," +
                    "  file_path     TEXT NOT NULL," +
                    "  checksum_sha256 TEXT NOT NULL" +
                    ")");
        }

        connFactory = new TestConnectionFactory(h2Url);
        archiver = new AuditLogArchiver(connFactory);
        setArchiveDir(archiver, tempDir);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (h2 != null && !h2.isClosed()) h2.close();
    }

    // --- helpers ---

    private static void setArchiveDir(AuditLogArchiver archiver, Path dir) throws Exception {
        Field f = AuditLogArchiver.class.getDeclaredField("archiveDir");
        f.setAccessible(true);
        f.set(archiver, dir);
    }

    private void insertRecord(String orderId, int daysAgo) throws SQLException {
        String sql = "INSERT INTO order_audit_log " +
                "(order_id, from_status, to_status, occurred_at, actor, details) VALUES " +
                "(?, 'PLANNED', 'CONFIRMED', NOW() - INTERVAL '" + daysAgo + "' DAY, 'test', 'ok')";
        try (PreparedStatement ps = h2.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.executeUpdate();
        }
    }

    private int countArchiveRuns() throws SQLException {
        try (Statement s = h2.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM audit_archive_runs")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int countArchivedRows() throws SQLException {
        try (Statement s = h2.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT COUNT(*) FROM order_audit_log WHERE archived_at IS NOT NULL")) {
            rs.next();
            return rs.getInt(1);
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

    // --- tests ---

    @Test
    void archiver_exportsOnlyRecordsOlderThan7Days() throws Exception {
        insertRecord("OLD-001", 10); // older than 7 days — should be archived
        insertRecord("OLD-002", 8);  // older than 7 days — should be archived
        insertRecord("NEW-001", 3);  // recent — should NOT be archived

        archiver.runWeeklyArchive();

        assertEquals(1, countArchiveRuns(), "Should have one archive run");
        assertEquals(2, countArchivedRows(), "Only old records should be archived");

        // Verify recent record is untouched
        try (PreparedStatement ps = h2.prepareStatement(
                "SELECT archived_at FROM order_audit_log WHERE order_id = 'NEW-001'")) {
            ResultSet rs = ps.executeQuery();
            rs.next();
            assertNull(rs.getTimestamp("archived_at"), "Recent record should not be archived");
        }
    }

    @Test
    void archiver_doesNotExport_recentRecords() throws Exception {
        insertRecord("NEW-001", 2);
        insertRecord("NEW-002", 6);

        archiver.runWeeklyArchive();

        assertEquals(0, countArchiveRuns(), "No archive run if no old records");
        assertEquals(0, countArchivedRows());
    }

    @Test
    void archiver_checksumMatches_writtenFile() throws Exception {
        insertRecord("ORD-001", 10);
        archiver.runWeeklyArchive();

        // Get file path from archive_runs
        try (Statement s = h2.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT file_path, checksum_sha256 FROM audit_archive_runs LIMIT 1")) {
            assertTrue(rs.next());
            Path file = Paths.get(rs.getString("file_path"));
            String stored = rs.getString("checksum_sha256");
            String computed = sha256Hex(file);
            assertEquals(computed, stored, "Stored checksum must match actual file content");
        }
    }

    @Test
    void archiver_fileIsGzipCompressed() throws Exception {
        insertRecord("ORD-001", 10);
        archiver.runWeeklyArchive();

        try (Statement s = h2.createStatement();
             ResultSet rs = s.executeQuery("SELECT file_path FROM audit_archive_runs LIMIT 1")) {
            assertTrue(rs.next());
            Path file = Paths.get(rs.getString("file_path"));
            assertTrue(file.toString().endsWith(".json.gz"), "Archive must be .json.gz");
            // Try decompressing — throws if not valid gzip
            try (GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(file));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(gzip))) {
                String firstLine = reader.readLine();
                assertNotNull(firstLine, "GZip file must have at least one line of JSON");
                assertTrue(firstLine.startsWith("{"), "First line should be a JSON object");
            }
        }
    }

    @Test
    void archiver_idempotent_doesNotReexportArchivedRecords() throws Exception {
        insertRecord("ORD-001", 10);

        archiver.runWeeklyArchive(); // first run
        archiver.runWeeklyArchive(); // second run — nothing left to archive

        assertEquals(1, countArchiveRuns(), "Second run should not create another archive run");
    }

    @Test
    void archiver_recordsMarkedArchivedAt_afterExport() throws Exception {
        insertRecord("ORD-A", 15);
        insertRecord("ORD-B", 20);

        archiver.runWeeklyArchive();

        try (Statement s = h2.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT COUNT(*) FROM order_audit_log WHERE archived_at IS NOT NULL")) {
            rs.next();
            assertEquals(2, rs.getInt(1), "Both old records should have archived_at set");
        }
    }

    @Test
    void archiver_multipleRuns_produceDistinctFiles() throws Exception {
        insertRecord("ORD-1", 10);
        archiver.runWeeklyArchive();

        // Simulate new old records for second run
        insertRecord("ORD-2", 8);
        archiver.runWeeklyArchive();

        assertEquals(2, countArchiveRuns(), "Each run with records should produce one archive");

        // Files should be different
        try (Statement s = h2.createStatement();
             ResultSet rs = s.executeQuery("SELECT file_path FROM audit_archive_runs")) {
            List<String> files = new ArrayList<>();
            while (rs.next()) files.add(rs.getString("file_path"));
            assertEquals(2, new HashSet<>(files).size(), "Archive files must have distinct paths");
        }
    }

    // --- Minimal connection factory backed by shared H2 connection ---

    static class TestConnectionFactory extends com.iiit.oms.db.util.PostgresConnectionFactory {
        private final String url;

        TestConnectionFactory(String url) {
            super(url, "sa", "");
            this.url = url;
        }

        @Override
        public Connection getConnection() {
            try {
                return DriverManager.getConnection(url, "sa", "");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
