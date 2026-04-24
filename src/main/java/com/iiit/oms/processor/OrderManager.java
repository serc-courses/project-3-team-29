package com.iiit.oms.processor;

import com.iiit.oms.model.Fund;
import com.iiit.oms.model.Order;
import com.iiit.oms.model.OrderSide;
import com.iiit.oms.repository.AccountRepository;
import com.iiit.oms.repository.FundRepository;
import com.iiit.oms.util.UniqueIdGenerator;
import java.math.BigDecimal;
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
    private final com.iiit.oms.repository.OrderRepository orderRepository;

    public OrderManager(AccountRepository accountRepository, FundRepository fundRepository,
            com.iiit.oms.repository.OrderRepository orderRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository must not be null");
        this.fundRepository = Objects.requireNonNull(fundRepository, "fundRepository must not be null");
        this.orderRepository = orderRepository;
    }

    public void validate(Order order) {
        logger.info("Validating order: " + order.getOrderID());

        if (!fundRepository.existsByFundId(order.getProductID())) {
            throw new IllegalStateException("Fund ID " + order.getProductID() + " does not exist");
        }
        Optional<com.iiit.oms.model.Account> accOpt = accountRepository.findByAccountId(order.getAccountID());
        if (accOpt.isEmpty()) {
            throw new IllegalStateException("Account ID " + order.getAccountID() + " does not exist");
        }
        com.iiit.oms.model.Account account = accOpt.get();

        if (order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Order amount must be positive");
        }

        if (order.getOrderSide() == null || order.getOrderSide() == OrderSide.BUY) {
            // BUY Validation: Enforce Cash Balances
            java.util.List<Order> accountOrders = this.orderRepository.findAll().stream()
                    .filter(o -> o.getAccountID().equals(order.getAccountID()))
                    .filter(o -> o.getOrderSide() == null || o.getOrderSide() == OrderSide.BUY)
                    .collect(java.util.stream.Collectors.toList());

            BigDecimal pendingBuyCash = BigDecimal.ZERO;
            for (Order o : accountOrders) {
                if (o.getOrderStatus() != com.iiit.oms.model.OrderStatus.BOOKED && o.getOrderStatus() != com.iiit.oms.model.OrderStatus.ERRORED && o.getOrderStatus() != com.iiit.oms.model.OrderStatus.CANCELLED) {
                    if (o.getAmount() != null && !o.getOrderID().equals(order.getOrderID())) {
                        pendingBuyCash = pendingBuyCash.add(o.getAmount());
                    }
                }
            }

            BigDecimal availableCash = account.getCashBalance().subtract(pendingBuyCash);
            if (order.getAmount().compareTo(availableCash) > 0) {
                throw new IllegalStateException("Insufficient funds. Attempting to buy ₹" 
                    + order.getAmount() + " but safe available cash is only ₹" + availableCash.setScale(2, java.math.RoundingMode.HALF_UP));
            }
        } else if (order.getOrderSide() != OrderSide.SELL) {
            throw new IllegalStateException("Order side must be BUY or SELL, got: " + order.getOrderSide());
        }

        if (order.getOrderSide() == OrderSide.SELL) {
            // Verify sufficient holdings to sell. We check current live value of booked
            // shares.
            if (this.orderRepository != null) {
                java.util.List<Order> accountOrders = this.orderRepository.findAll().stream()
                        .filter(o -> o.getAccountID().equals(order.getAccountID()))
                        .filter(o -> o.getProductID().equals(order.getProductID()))
                        .collect(java.util.stream.Collectors.toList());

                BigDecimal bookedShares = BigDecimal.ZERO;
                BigDecimal pendingSellAmount = BigDecimal.ZERO;

                for (Order o : accountOrders) {
                    if (o.getOrderStatus() == com.iiit.oms.model.OrderStatus.BOOKED) {
                        BigDecimal shares = o.getAllocatedShares() != null ? o.getAllocatedShares() : o.getQuantity();
                        if (shares != null) {
                            if (o.getOrderSide() == null || o.getOrderSide() == OrderSide.BUY) {
                                bookedShares = bookedShares.add(shares);
                            } else if (o.getOrderSide() == OrderSide.SELL) {
                                bookedShares = bookedShares.subtract(shares);
                            }
                        }
                    } else if (o.getOrderStatus() != com.iiit.oms.model.OrderStatus.ERRORED && o.getOrderStatus() != com.iiit.oms.model.OrderStatus.BOOKED && o.getOrderStatus() != com.iiit.oms.model.OrderStatus.CANCELLED) {
                        if (o.getOrderSide() == OrderSide.SELL && o.getAmount() != null && !o.getOrderID().equals(order.getOrderID())) {
                            pendingSellAmount = pendingSellAmount.add(o.getAmount());
                        }
                    }
                }

                Optional<Fund> fundOpt = fundRepository.findByFundId(order.getProductID());
                BigDecimal nav = fundOpt.isPresent() ? fundOpt.get().getNAV() : null;

                if (nav == null || nav.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalStateException("Cannot sell: missing live NAV for fund " + order.getProductID());
                }

                BigDecimal bookedValue = bookedShares.multiply(nav).setScale(4, java.math.RoundingMode.HALF_UP);
                
                BigDecimal availableValue = bookedValue.subtract(pendingSellAmount);
                if (availableValue.compareTo(BigDecimal.ZERO) < 0) {
                    availableValue = BigDecimal.ZERO;
                }

                // Allow selling up to 99% of max sell value to prevent short selling when NAV drops
                BigDecimal safeMaxSellValue = availableValue.multiply(new BigDecimal("0.99")).setScale(4, java.math.RoundingMode.HALF_UP);

                if (order.getAmount().compareTo(safeMaxSellValue) > 0) {
                    throw new IllegalStateException("Insufficient balance. Attempting to sell ₹"
                            + order.getAmount() + " but safe available portfolio value (accounting for pending sells) is only ₹"
                            + safeMaxSellValue.setScale(2, java.math.RoundingMode.HALF_UP));
                }
            }
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
            // Compute expected units from amount / NAV
            java.math.BigDecimal nav = fund.getNAV();
            if (nav != null && nav.compareTo(java.math.BigDecimal.ZERO) > 0 && order.getAmount() != null) {
                order.setNav(nav);
                order.setQuantity(order.getAmount().divide(nav, 4, java.math.RoundingMode.HALF_UP));
            }
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
