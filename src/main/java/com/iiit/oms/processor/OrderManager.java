package com.iiit.oms.processor;

import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderSide;
import com.iiit.oms.repository.AccountRepository;
import com.iiit.oms.repository.FundRepository;
import com.iiit.oms.util.UniqueIdGenerator;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.logging.Logger;

public class OrderManager {
    private static final Logger logger = Logger.getLogger(OrderManager.class.getName());
    private final AccountRepository accountRepository;
    private final FundRepository fundRepository;

    public OrderManager(AccountRepository accountRepository, FundRepository fundRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository must not be null");
        this.fundRepository = Objects.requireNonNull(fundRepository, "fundRepository must not be null");
    }

    public void validate(Order order) {
        logger.info("Validating order: " + order.getOrderID());
        
        // Validation 1: productID should exist in funds table
        if (!fundRepository.existsByFundId(order.getProductID())) {
            throw new IllegalStateException("Fund ID " + order.getProductID() + " does not exist");
        }
        
        // Validation 2: accountID should exist in accounts table
        if (!accountRepository.existsByAccountId(order.getAccountID())) {
            throw new IllegalStateException("Account ID " + order.getAccountID() + " does not exist");
        }
        
        // Validation 3: amount should be greater than 0
        if (order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Order amount must be greater than 0, got: " + order.getAmount());
        }
        
        // Validation 4: orderSide should be BUY or SELL (enum ensures this, but explicit check for clarity)
        if (order.getOrderSide() != OrderSide.BUY && order.getOrderSide() != OrderSide.SELL) {
            throw new IllegalStateException("Order side must be BUY or SELL, got: " + order.getOrderSide());
        }
        
        logger.info("Order " + order.getOrderID() + " validation successful");
    }

    public void enrich(Order order) {
        logger.info("Enriching order: " + order.getOrderID());
        
        // Assign unique ID if not already assigned
        if (order.getOrderID() == null || order.getOrderID().trim().isEmpty()) {
            String uniqueId = UniqueIdGenerator.generate("ORD");
            order.setOrderID(uniqueId);
            logger.info("Assigned unique ID " + uniqueId + " to order");
        }
    }

    public void place(Order order) {
        logger.info("Placing order: " + order.getOrderID());
    }

    public void confirm(Order order) {
        logger.info("Confirming order: " + order.getOrderID());
    }

    public void contract(Order order) {
        logger.info("Contracting order: " + order.getOrderID());
    }

    public void book(Order order) {
        logger.info("Booking order: " + order.getOrderID());
    }
}
