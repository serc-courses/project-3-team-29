package com.iiit.oms.repository.inmemory;

import com.iiit.oms.db.inmemory.InMemoryBulkOrderMappingDatabase;
import com.iiit.oms.repository.BulkOrderMappingRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InMemoryBulkOrderMappingRepository implements BulkOrderMappingRepository {
    private final InMemoryBulkOrderMappingDatabase database;

    public InMemoryBulkOrderMappingRepository(InMemoryBulkOrderMappingDatabase database) {
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
        return database.getAll();
    }
}
