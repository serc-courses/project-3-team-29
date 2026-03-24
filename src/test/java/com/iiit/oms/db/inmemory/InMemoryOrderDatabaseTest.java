package com.iiit.oms.db.inmemory;

import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderSide;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryOrderDatabaseTest {

    @Test
    void shouldStoreAndRetrieveOrders() {
        InMemoryOrderDatabase database = new InMemoryOrderDatabase();

        database.upsert(new Order("ORD001", "FND001", BigDecimal.TEN, BigDecimal.valueOf(1000), "ACCT00001", OrderSide.BUY));

        List<Order> orders = database.getAll();

        assertEquals(1, orders.size());
        assertTrue(database.exists("ORD001"));
        assertEquals(OrderSide.BUY, orders.get(0).getOrderSide());
        assertEquals(false, orders.get(0).isProcessed());
    }
}
