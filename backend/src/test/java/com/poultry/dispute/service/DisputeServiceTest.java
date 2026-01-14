package com.poultry.dispute.service;

import com.poultry.common.exception.BusinessException;
import com.poultry.dispute.dto.CreateDisputeRequest;
import com.poultry.dispute.dto.DisputeDto;
import com.poultry.dispute.entity.Dispute;
import com.poultry.dispute.repository.DisputeHistoryRepository;
import com.poultry.dispute.repository.DisputeMessageRepository;
import com.poultry.dispute.repository.DisputeRepository;
import com.poultry.order.entity.Order;
import com.poultry.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisputeService Unit Tests")
class DisputeServiceTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private DisputeMessageRepository messageRepository;

    @Mock
    private DisputeHistoryRepository historyRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private DisputeService disputeService;

    private UUID buyerId;
    private UUID sellerId;
    private UUID orderId;
    private UUID disputeId;
    private Order order;
    private Dispute dispute;

    @BeforeEach
    void setUp() {
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        disputeId = UUID.randomUUID();

        order = new Order();
        order.setId(orderId);
        order.setOrderNumber("ORD-001");
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setStatus(Order.OrderStatus.DELIVERED);
        order.setTotalAmount(BigDecimal.valueOf(1000));

        dispute = new Dispute();
        dispute.setId(disputeId);
        dispute.setDisputeNumber("DSP-001");
        dispute.setOrderId(orderId);
        dispute.setBuyerId(buyerId);
        dispute.setSellerId(sellerId);
        dispute.setRaisedBy(Dispute.RaisedBy.BUYER);
        dispute.setType(Dispute.DisputeType.QUALITY_ISSUE);
        dispute.setStatus(Dispute.DisputeStatus.OPEN);
        dispute.setPriority(2);
        dispute.setTitle("Quality Issue");
        dispute.setDescription("Product quality not as expected");
        dispute.setRequestedAmount(BigDecimal.valueOf(500));
        dispute.setMessages(new ArrayList<>());
    }

    @Nested
    @DisplayName("createDispute")
    class CreateDisputeTests {

        @Test
        @DisplayName("should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            CreateDisputeRequest request = new CreateDisputeRequest();
            request.setOrderId(orderId);
            request.setType(Dispute.DisputeType.QUALITY_ISSUE);
            request.setTitle("Quality Issue");
            request.setDescription("Issue");

            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.createDispute(buyerId, Dispute.RaisedBy.BUYER, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("should create dispute successfully")
        void shouldCreateDisputeSuccessfully() {
            CreateDisputeRequest request = new CreateDisputeRequest();
            request.setOrderId(orderId);
            request.setType(Dispute.DisputeType.QUALITY_ISSUE);
            request.setTitle("Quality Issue");
            request.setDescription("Product quality issue");
            request.setRequestedAmount(BigDecimal.valueOf(500));

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(disputeRepository.getNextDisputeNumber()).thenReturn(1L);
            when(disputeRepository.save(any(Dispute.class))).thenReturn(dispute);

            DisputeDto result = disputeService.createDispute(buyerId, Dispute.RaisedBy.BUYER, request);

            assertThat(result).isNotNull();
            verify(disputeRepository).save(any(Dispute.class));
        }

        @Test
        @DisplayName("should throw exception when buyer does not own the order")
        void shouldThrowExceptionWhenBuyerDoesNotOwnOrder() {
            UUID differentBuyerId = UUID.randomUUID();
            CreateDisputeRequest request = new CreateDisputeRequest();
            request.setOrderId(orderId);
            request.setType(Dispute.DisputeType.QUALITY_ISSUE);
            request.setTitle("Quality Issue");
            request.setDescription("Issue");

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> disputeService.createDispute(differentBuyerId, Dispute.RaisedBy.BUYER, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("does not belong to this buyer");
        }
    }

    @Nested
    @DisplayName("getDispute")
    class GetDisputeTests {

        @Test
        @DisplayName("should return dispute by id")
        void shouldReturnDisputeById() {
            when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));
            when(messageRepository.countByDisputeId(disputeId)).thenReturn(0L);

            DisputeDto result = disputeService.getDispute(disputeId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(disputeId);
        }

        @Test
        @DisplayName("should throw exception when dispute not found")
        void shouldThrowExceptionWhenDisputeNotFound() {
            when(disputeRepository.findById(disputeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> disputeService.getDispute(disputeId))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("getDisputesByBuyer")
    class GetDisputesByBuyerTests {

        @Test
        @DisplayName("should return buyer disputes")
        void shouldReturnBuyerDisputes() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Dispute> disputePage = new PageImpl<>(List.of(dispute), pageable, 1);

            when(disputeRepository.findByBuyerId(buyerId, pageable)).thenReturn(disputePage);

            Page<DisputeDto> result = disputeService.getDisputesByBuyer(buyerId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTests {

        @Test
        @DisplayName("should update dispute status")
        void shouldUpdateDisputeStatus() {
            UUID adminId = UUID.randomUUID();

            when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));
            when(disputeRepository.save(any(Dispute.class))).thenReturn(dispute);

            DisputeDto result = disputeService.updateStatus(disputeId, adminId, Dispute.DisputeStatus.UNDER_REVIEW, "Starting review");

            assertThat(result).isNotNull();
            verify(disputeRepository).save(any(Dispute.class));
            verify(historyRepository).save(any());
        }
    }

    @Nested
    @DisplayName("escalateDispute")
    class EscalateDisputeTests {

        @Test
        @DisplayName("should escalate dispute priority")
        void shouldEscalateDisputePriority() {
            UUID adminId = UUID.randomUUID();

            when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));
            when(disputeRepository.save(any(Dispute.class))).thenReturn(dispute);

            DisputeDto result = disputeService.escalateDispute(disputeId, adminId, "Urgent customer");

            assertThat(result).isNotNull();
            verify(disputeRepository).save(argThat(d -> d.getPriority() == 1 && d.getStatus() == Dispute.DisputeStatus.ESCALATED));
        }
    }
}
