package com.iiit.oms.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BulkOrderMappingRepository {
    void save(String bulkOrderId, List<String> individualOrderIds);

    Optional<List<String>> findIndividualOrderIds(String bulkOrderId);

    Map<String, List<String>> findAll();
}
