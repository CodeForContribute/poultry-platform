package com.poultry.delivery.service;

import com.poultry.common.exception.BusinessException;
import com.poultry.delivery.dto.DeliveryDto;
import com.poultry.delivery.entity.Delivery;
import com.poultry.delivery.entity.DeliveryAgent;
import com.poultry.delivery.repository.DeliveryAgentRepository;
import com.poultry.delivery.repository.DeliveryRepository;
import com.poultry.notification.service.NotificationService;
import com.poultry.order.entity.Order;
import com.poultry.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public Delivery createDelivery(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("Order", orderId));

        if (deliveryRepository.findByOrderId(orderId).isPresent()) {
            throw new BusinessException("Delivery already exists for this order", "DELIVERY_EXISTS", HttpStatus.CONFLICT);
        }

        Delivery delivery = Delivery.builder()
                .orderId(orderId)
                .status(Delivery.DeliveryStatus.PENDING_ASSIGNMENT)
                .slaDeadline(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        delivery = deliveryRepository.save(delivery);
        log.info("Delivery created: {} for order: {}", delivery.getId(), orderId);

        return delivery;
    }

    @Transactional
    public Delivery assignDeliveryAgent(UUID orderId, UUID agentId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> BusinessException.notFound("Delivery for order", orderId));

        DeliveryAgent agent = deliveryAgentRepository.findById(agentId)
                .orElseThrow(() -> BusinessException.notFound("DeliveryAgent", agentId));

        if (!agent.getIsAvailable()) {
            throw BusinessException.invalidState("Agent is not available");
        }

        delivery.setAgentId(agentId);
        delivery.setStatus(Delivery.DeliveryStatus.ASSIGNED);
        delivery = deliveryRepository.save(delivery);

        agent.setIsAvailable(false);
        deliveryAgentRepository.save(agent);

        log.info("Agent {} assigned to delivery {}", agentId, delivery.getId());

        return delivery;
    }

    @Transactional
    public Delivery updateDeliveryStatus(UUID deliveryId, Delivery.DeliveryStatus status, Map<String, Object> location) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> BusinessException.notFound("Delivery", deliveryId));

        delivery.setStatus(status);

        if (location != null) {
            delivery.setCurrentLocation(location);
        }

        if (status == Delivery.DeliveryStatus.DISPATCHED) {
            delivery.setDispatchedAt(Instant.now());
        }

        delivery = deliveryRepository.save(delivery);
        log.info("Delivery {} status updated to {}", deliveryId, status);

        return delivery;
    }

    @Transactional
    public String generateDeliveryOtp(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> BusinessException.notFound("Delivery", deliveryId));

        String otp = String.format("%06d", RANDOM.nextInt(1000000));
        delivery.setDeliveryOtp(otp);
        delivery.setOtpGeneratedAt(Instant.now());
        deliveryRepository.save(delivery);

        log.info("OTP generated for delivery: {}", deliveryId);

        return otp;
    }

    @Transactional
    public boolean verifyDeliveryOtp(UUID deliveryId, String otp) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> BusinessException.notFound("Delivery", deliveryId));

        if (!delivery.isOtpValid()) {
            throw BusinessException.invalidState("OTP has expired");
        }

        if (!delivery.getDeliveryOtp().equals(otp)) {
            return false;
        }

        delivery.setOtpVerifiedAt(Instant.now());
        deliveryRepository.save(delivery);

        log.info("OTP verified for delivery: {}", deliveryId);
        return true;
    }

    @Transactional
    public Delivery completeDelivery(UUID deliveryId, String photoProofUrl) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> BusinessException.notFound("Delivery", deliveryId));

        if (delivery.getOtpVerifiedAt() == null) {
            throw BusinessException.invalidState("OTP must be verified before completing delivery");
        }

      UUID orderId = delivery.getOrderId();
      UUID agentId = delivery.getAgentId();

      delivery.setStatus(Delivery.DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(Instant.now());
        delivery.setPhotoProofUrl(photoProofUrl);
        delivery = deliveryRepository.save(delivery);

        // Update order status
      Order order = orderRepository.findById(orderId)
                                   .orElseThrow(() -> BusinessException.notFound("Order", orderId));
        order.setStatus(Order.OrderStatus.DELIVERED);
        orderRepository.save(order);

        // Release agent
      if (agentId != null)
      {
        DeliveryAgent agent = deliveryAgentRepository.findById(agentId).orElse(null);
            if (agent != null) {
                agent.setIsAvailable(true);
                agent.incrementDeliveryCount(true);
                deliveryAgentRepository.save(agent);
            }
        }

        log.info("Delivery completed: {}", deliveryId);

        return delivery;
    }

    @Transactional
    public Delivery markDeliveryFailed(UUID deliveryId, String reason) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> BusinessException.notFound("Delivery", deliveryId));

        delivery.setStatus(Delivery.DeliveryStatus.FAILED);
        delivery.setFailedAt(Instant.now());
        delivery.setFailureReason(reason);
        delivery.setRetryCount(delivery.getRetryCount() + 1);

        // Release agent
        if (delivery.getAgentId() != null) {
            DeliveryAgent agent = deliveryAgentRepository.findById(delivery.getAgentId()).orElse(null);
            if (agent != null) {
                agent.setIsAvailable(true);
                agent.incrementDeliveryCount(false);
                deliveryAgentRepository.save(agent);
            }
            delivery.setAgentId(null);
        }

        delivery = deliveryRepository.save(delivery);
        log.info("Delivery marked as failed: {} - {}", deliveryId, reason);

        return delivery;
    }

    @Transactional(readOnly = true)
    public DeliveryDto getDeliveryTracking(UUID orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> BusinessException.notFound("Delivery for order", orderId));

        DeliveryAgent agent = null;
        if (delivery.getAgentId() != null) {
            agent = deliveryAgentRepository.findById(delivery.getAgentId()).orElse(null);
        }

        return DeliveryDto.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrderId())
                .agentId(delivery.getAgentId())
                .agentName(agent != null ? agent.getName() : null)
                .status(delivery.getStatus())
                .currentLocation(delivery.getCurrentLocation())
                .slaDeadline(delivery.getSlaDeadline())
                .dispatchedAt(delivery.getDispatchedAt())
                .deliveredAt(delivery.getDeliveredAt())
                .retryCount(delivery.getRetryCount())
                .createdAt(delivery.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<Delivery> getRetryableDeliveries() {
        return deliveryRepository.findRetryableDeliveries();
    }

    @Transactional(readOnly = true)
    public List<Delivery> getOverdueDeliveries() {
        return deliveryRepository.findOverdueDeliveries(Delivery.DeliveryStatus.DISPATCHED, Instant.now());
    }
}
