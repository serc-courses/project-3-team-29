error id: file:///C:/Users/saksh/OneDrive%20-%20International%20Institute%20of%20Information%20Technology/Desktop/Soft_Engg/project3/src/main/java/com/iiit/oms/processor/OrderManager.java:java/math/BigDecimal#
file:///C:/Users/saksh/OneDrive%20-%20International%20Institute%20of%20Information%20Technology/Desktop/Soft_Engg/project3/src/main/java/com/iiit/oms/processor/OrderManager.java
empty definition using pc, found symbol in pc: java/math/BigDecimal#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 301
uri: file:///C:/Users/saksh/OneDrive%20-%20International%20Institute%20of%20Information%20Technology/Desktop/Soft_Engg/project3/src/main/java/com/iiit/oms/processor/OrderManager.java
text:
```scala
package com.iiit.oms.processor;

import com.iiit.oms.model.Fund;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderSide;
import com.iiit.oms.repository.AccountRepository;
import com.iiit.oms.repository.FundRepository;
import com.iiit.oms.util.UniqueIdGenerator;
import java.math.@@BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public class OrderManager {
    private static final Logger logger = Logger.getLogger(OrderManager.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final AccountRepository accountRepository;
    private final FundRepository fundRepository;

    public OrderManager(AccountRepository accountRepository, FundRepository fundRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository must not be null");
        this.fundRepository = Objects.requireNonNull(fundRepository, "fundRepository must not be null");
    }

    public void validate(Order order) {
        logger.info("Validating order: " + order.getOrderID());
        
        if (!fundRepository.existsByFundId(order.getProductID())) {
            throw new IllegalStateException("Fund ID " + order.getProductID() + " does not exist");
        }
        if (!accountRepository.existsByAccountId(order.getAccountID())) {
            throw new IllegalStateException("Account ID " + order.getAccountID() + " does not exist");
        }
        if (order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Order amount must be greater than 0, got: " + order.getAmount());
        }
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

        // Enrich from fund data
        Optional<Fund> maybeFund = fundRepository.findByFundId(order.getProductID());
        if (maybeFund.isPresent()) {
            Fund fund = maybeFund.get();
            order.setFundFamily(fund.getFundFamily());
            order.setTransferAgent(fund.isOffshore() ? "RBC" : "NSCC");
        }

        // Set trade date (today) and settlement date (T+1 business day)
        LocalDate today = LocalDate.now();
        order.setTradeDate(today.format(DATE_FMT));
        order.setSettlementDate(today.plusDays(1).format(DATE_FMT));

        logger.info("Order " + order.getOrderID() + " enriched: transferAgent=" + order.getTransferAgent()
                + ", tradeDate=" + order.getTradeDate() + ", settlementDate=" + order.getSettlementDate());
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


```


#### Short summary: 

empty definition using pc, found symbol in pc: java/math/BigDecimal#