package com.iiit.oms.repository.postgres;

import com.iiit.oms.db.postgres.PostgresBulkOrderMappingDatabase;
import com.iiit.oms.repository.BulkOrderMappingRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class PostgresBulkOrderMappingRepository implements BulkOrderMappingRepository {
    private final PostgresBulkOrderMappingDatabase database;

    public PostgresBulkOrderMappingRepository(PostgresBulkOrderMappingDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    @Override
    public void save(String bulkOrderId, List<String> individualOrderIds) {
        Objects.requireNonNull(bulkOrderId, "bulkOrderId must not be null");
        Objects.requireNonNull(individualOrderIds, "individualOrderIds must not be null");
        database.upsert(bulkOrderId, individualOrderIds);
    }

    @Override
    public Optional<List<String>> findIndividualOrderIds(String bulkOrderId) {
        return database.getById(bulkOrderId);
    }

    @Override
    public Map<String, List<String>> findAll() {
        return Collections.unmodifiableMap(database.getAll());
    }
}
