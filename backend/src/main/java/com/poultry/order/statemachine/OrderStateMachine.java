package com.poultry.order.statemachine;

import com.poultry.common.exception.BusinessException;
import com.poultry.order.entity.Order;
import com.poultry.order.entity.Order.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        VALID_TRANSITIONS.put(OrderStatus.DRAFT, Set.of(
                OrderStatus.PLACED
        ));

        VALID_TRANSITIONS.put(OrderStatus.PLACED, Set.of(
                OrderStatus.SELLER_CONFIRMED,
                OrderStatus.SELLER_REJECTED
        ));

        VALID_TRANSITIONS.put(OrderStatus.SELLER_CONFIRMED, Set.of(
                OrderStatus.PAYMENT_PENDING
        ));

        VALID_TRANSITIONS.put(OrderStatus.PAYMENT_PENDING, Set.of(
                OrderStatus.PAID,
                OrderStatus.PAYMENT_FAILED
        ));

        VALID_TRANSITIONS.put(OrderStatus.PAYMENT_FAILED, Set.of(
                OrderStatus.PAYMENT_PENDING // Retry
        ));

        VALID_TRANSITIONS.put(OrderStatus.PAID, Set.of(
                OrderStatus.DISPATCHED,
                OrderStatus.CANCELLED_BY_BUYER
        ));

        VALID_TRANSITIONS.put(OrderStatus.DISPATCHED, Set.of(
                OrderStatus.DELIVERED
        ));

        VALID_TRANSITIONS.put(OrderStatus.DELIVERED, Set.of(
                OrderStatus.SETTLED
        ));

        VALID_TRANSITIONS.put(OrderStatus.CANCELLED_BY_BUYER, Set.of(
                OrderStatus.REFUND_INITIATED
        ));

        VALID_TRANSITIONS.put(OrderStatus.REFUND_INITIATED, Set.of(
                OrderStatus.REFUNDED
        ));

        // Terminal states have no valid transitions
        VALID_TRANSITIONS.put(OrderStatus.SELLER_REJECTED, Set.of());
        VALID_TRANSITIONS.put(OrderStatus.SETTLED, Set.of());
        VALID_TRANSITIONS.put(OrderStatus.REFUNDED, Set.of());
    }

    public boolean canTransition(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> validTargets = VALID_TRANSITIONS.get(from);
        return validTargets != null && validTargets.contains(to);
    }

    public void validateTransition(Order order, OrderStatus targetStatus) {
        OrderStatus currentStatus = order.getStatus();

        if (!canTransition(currentStatus, targetStatus)) {
            throw BusinessException.invalidState(
                    String.format("Cannot transition order from %s to %s", currentStatus, targetStatus)
            );
        }
    }

    public void transition(Order order, OrderStatus targetStatus) {
        validateTransition(order, targetStatus);
        order.setStatus(targetStatus);
    }

    public Set<OrderStatus> getValidNextStates(OrderStatus current) {
        return VALID_TRANSITIONS.getOrDefault(current, Set.of());
    }

    public boolean isTerminal(OrderStatus status) {
        return VALID_TRANSITIONS.getOrDefault(status, Set.of()).isEmpty();
    }

    public boolean canCancel(OrderStatus status) {
        return status == OrderStatus.PAID;
    }

    public boolean requiresPayment(OrderStatus status) {
        return status == OrderStatus.SELLER_CONFIRMED || status == OrderStatus.PAYMENT_FAILED;
    }

    public boolean isPaymentComplete(OrderStatus status) {
        return status == OrderStatus.PAID
                || status == OrderStatus.DISPATCHED
                || status == OrderStatus.DELIVERED
                || status == OrderStatus.SETTLED;
    }
}
