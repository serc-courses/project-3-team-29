package com.iiit.oms.db.inmemory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBulkOrderMappingDatabase {
    private final Map<String, List<String>> mappings = new ConcurrentHashMap<>();

    public void upsert(String bulkOrderId, List<String> individualOrderIds) {
        mappings.put(bulkOrderId, Collections.unmodifiableList(new ArrayList<>(individualOrderIds)));
    }

    public Optional<List<String>> getById(String bulkOrderId) {
        return Optional.ofNullable(mappings.get(bulkOrderId));
    }

    public Map<String, List<String>> getAll() {
        return Collections.unmodifiableMap(mappings);
    }

    public boolean exists(String bulkOrderId) {
        return mappings.containsKey(bulkOrderId);
    }

    public void deleteById(String bulkOrderId) {
        mappings.remove(bulkOrderId);
    }

    public void clear() {
        mappings.clear();
    }
}
