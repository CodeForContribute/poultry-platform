package com.poultry.dispute.service;

import com.poultry.common.exception.BusinessException;
import com.poultry.dispute.dto.*;
import com.poultry.dispute.entity.Dispute;
import com.poultry.dispute.entity.Dispute.DisputeStatus;
import com.poultry.dispute.entity.Dispute.DisputeType;
import com.poultry.dispute.entity.Dispute.RaisedBy;
import com.poultry.dispute.entity.DisputeHistory;
import com.poultry.dispute.entity.DisputeMessage;
import com.poultry.dispute.repository.DisputeHistoryRepository;
import com.poultry.dispute.repository.DisputeMessageRepository;
import com.poultry.dispute.repository.DisputeRepository;
import com.poultry.order.entity.Order;
import com.poultry.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeService
{

  @Transactional
  public DisputeDto createDispute(UUID userId, RaisedBy raisedBy, CreateDisputeRequest request)
  {
    log.info("Creating dispute for order {} by {} {}", request.getOrderId(), raisedBy, userId);

    Order order = orderRepository.findById(request.getOrderId())
                                 .orElseThrow(() -> new BusinessException("Order not found", "ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));

    // Validate user owns the order
    if (raisedBy == RaisedBy.BUYER && !order.getBuyerId().equals(userId))
    {
      throw new BusinessException("Order does not belong to this buyer", "UNAUTHORIZED", HttpStatus.FORBIDDEN);
    }
    if (raisedBy == RaisedBy.SELLER && !order.getSellerId().equals(userId))
    {
      throw new BusinessException("Order does not belong to this seller", "UNAUTHORIZED", HttpStatus.FORBIDDEN);
    }

    // Generate dispute number
    Long seq = disputeRepository.getNextDisputeNumber();
    String disputeNumber = "DSP" + seq;

    Dispute dispute = Dispute.builder()
                             .disputeNumber(disputeNumber)
                             .orderId(request.getOrderId())
                             .buyerId(order.getBuyerId())
                             .sellerId(order.getSellerId())
                             .raisedBy(raisedBy)
                             .type(request.getType())
                             .status(DisputeStatus.OPEN)
                             .title(request.getTitle())
                             .description(request.getDescription())
                             .evidenceUrls(request.getEvidenceUrls() != null ? request.getEvidenceUrls() : List.of())
                             .requestedResolution(request.getRequestedResolution())
                             .requestedAmount(request.getRequestedAmount())
                             .priority(2)
                             .build();

    dispute = disputeRepository.save(dispute);

    // Record history
    recordHistory(dispute.getId(), "CREATED", null, DisputeStatus.OPEN, userId, "Dispute created");

    log.info("Dispute {} created successfully", disputeNumber);
    return DisputeDto.fromEntity(dispute);
  }

  @Transactional(readOnly = true)
  public DisputeDto getDispute(UUID disputeId)
  {
    Dispute dispute = disputeRepository.findById(disputeId)
                                       .orElseThrow(() -> new BusinessException("Dispute not found", "DISPUTE_NOT_FOUND", HttpStatus.NOT_FOUND));

    DisputeDto dto = DisputeDto.fromEntity(dispute);
    dto.setMessageCount((int) messageRepository.countByDisputeId(disputeId));
    return dto;
  }

  @Transactional(readOnly = true)
  public DisputeDto getDisputeByNumber(String disputeNumber)
  {
    Dispute dispute = disputeRepository.findByDisputeNumber(disputeNumber)
                                       .orElseThrow(() -> new BusinessException("Dispute not found", "DISPUTE_NOT_FOUND", HttpStatus.NOT_FOUND));
    return DisputeDto.fromEntity(dispute);
  }

  @Transactional(readOnly = true)
  public Page<DisputeDto> getDisputesByBuyer(UUID buyerId, Pageable pageable)
  {
    return disputeRepository.findByBuyerId(buyerId, pageable)
                            .map(DisputeDto::fromEntity);
  }

  @Transactional(readOnly = true)
  public Page<DisputeDto> getDisputesBySeller(UUID sellerId, Pageable pageable)
  {
    return disputeRepository.findBySellerId(sellerId, pageable)
                            .map(DisputeDto::fromEntity);
  }

  @Transactional
  public DisputeMessageDto addMessage(UUID disputeId, UUID senderId, String senderType, AddMessageRequest request)
  {
    log.info("Adding message to dispute {} by {} {}", disputeId, senderType, senderId);

    Dispute dispute = disputeRepository.findById(disputeId)
                                       .orElseThrow(() -> new BusinessException("Dispute not found", "DISPUTE_NOT_FOUND", HttpStatus.NOT_FOUND));

    if (!dispute.isOpen())
    {
      throw new BusinessException("Cannot add message to closed dispute", "DISPUTE_CLOSED", HttpStatus.BAD_REQUEST);
    }

    DisputeMessage message = DisputeMessage.builder()
                                           .dispute(dispute)
                                           .senderType(senderType)
                                           .senderId(senderId)
                                           .message(request.getMessage())
                                           .attachmentUrls(request.getAttachmentUrls() != null ? request.getAttachmentUrls() : List.of())
                                           .isInternal(request.getIsInternal() != null && request.getIsInternal())
                                           .build();

    message = messageRepository.save(message);

    // Update dispute status if awaiting response
    if (dispute.getStatus() == DisputeStatus.AWAITING_RESPONSE)
    {
      DisputeStatus oldStatus = dispute.getStatus();
      dispute.setStatus(DisputeStatus.UNDER_REVIEW);
      disputeRepository.save(dispute);
      recordHistory(disputeId, "STATUS_CHANGE", oldStatus, DisputeStatus.UNDER_REVIEW, senderId, "Response received");
    }

    return DisputeMessageDto.fromEntity(message);
  }

  @Transactional(readOnly = true)
  public List<DisputeMessageDto> getMessages(UUID disputeId, boolean includeInternal)
  {
    List<DisputeMessage> messages;
    if (includeInternal)
    {
      messages = messageRepository.findByDisputeIdAll(disputeId);
    }
    else
    {
      messages = messageRepository.findByDisputeIdPublic(disputeId);
    }
    return messages.stream()
                   .map(DisputeMessageDto::fromEntity)
                   .collect(Collectors.toList());
  }

  @Transactional
  public DisputeDto updateStatus(UUID disputeId, UUID adminId, DisputeStatus newStatus, String notes)
  {
    log.info("Updating dispute {} status to {} by admin {}", disputeId, newStatus, adminId);

    Dispute dispute = disputeRepository.findById(disputeId)
                                       .orElseThrow(() -> new BusinessException("Dispute not found", "DISPUTE_NOT_FOUND", HttpStatus.NOT_FOUND));

    DisputeStatus oldStatus = dispute.getStatus();
    dispute.setStatus(newStatus);
    dispute = disputeRepository.save(dispute);

    recordHistory(disputeId, "STATUS_CHANGE", oldStatus, newStatus, adminId, notes);

    return DisputeDto.fromEntity(dispute);
  }

  @Transactional
  public DisputeDto assignToAdmin(UUID disputeId, UUID adminId, UUID assignee)
  {
    log.info("Assigning dispute {} to admin {}", disputeId, assignee);

    Dispute dispute = disputeRepository.findById(disputeId)
                                       .orElseThrow(() -> new BusinessException("Dispute not found", "DISPUTE_NOT_FOUND", HttpStatus.NOT_FOUND));

    dispute.setAssignedTo(assignee);
    if (dispute.getStatus() == DisputeStatus.OPEN)
    {
      DisputeStatus oldStatus = dispute.getStatus();
      dispute.setStatus(DisputeStatus.UNDER_REVIEW);
      recordHistory(disputeId, "ASSIGNED", oldStatus, DisputeStatus.UNDER_REVIEW, adminId, "Assigned to admin");
    }
    else
    {
      recordHistory(disputeId, "ASSIGNED", null, null, adminId, "Reassigned to admin");
    }

    dispute = disputeRepository.save(dispute);
    return DisputeDto.fromEntity(dispute);
  }

  @Transactional
  public DisputeDto escalateDispute(UUID disputeId, UUID adminId, String reason)
  {
    log.info("Escalating dispute {} by admin {}", disputeId, adminId);

    Dispute dispute = disputeRepository.findById(disputeId)
                                       .orElseThrow(() -> new BusinessException("Dispute not found", "DISPUTE_NOT_FOUND", HttpStatus.NOT_FOUND));

    if (!dispute.canBeEscalated())
    {
      throw new BusinessException("Dispute cannot be escalated", "INVALID_STATUS", HttpStatus.BAD_REQUEST);
    }

    DisputeStatus oldStatus = dispute.getStatus();
    dispute.setStatus(DisputeStatus.ESCALATED);
    dispute.setEscalatedAt(Instant.now());
    dispute.setPriority(1);
    dispute = disputeRepository.save(dispute);

    recordHistory(disputeId, "ESCALATED", oldStatus, DisputeStatus.ESCALATED, adminId, reason);

    return DisputeDto.fromEntity(dispute);
  }

  @Transactional
  public DisputeDto resolveDispute(UUID disputeId, UUID adminId, ResolveDisputeRequest request)
  {
    log.info("Resolving dispute {} by admin {}", disputeId, adminId);

    Dispute dispute = disputeRepository.findById(disputeId)
                                       .orElseThrow(() -> new BusinessException("Dispute not found", "DISPUTE_NOT_FOUND", HttpStatus.NOT_FOUND));

    if (!dispute.isOpen())
    {
      throw new BusinessException("Dispute is already resolved", "ALREADY_RESOLVED", HttpStatus.BAD_REQUEST);
    }

    DisputeStatus oldStatus = dispute.getStatus();
    dispute.setStatus(DisputeStatus.RESOLVED);
    dispute.setFinalResolution(request.getResolution());
    dispute.setResolutionAmount(request.getResolutionAmount());
    dispute.setResolutionNotes(request.getNotes());
    dispute.setResolvedAt(Instant.now());
    dispute.setResolvedBy(adminId);

    dispute = disputeRepository.save(dispute);

    recordHistory(disputeId, "RESOLVED", oldStatus, DisputeStatus.RESOLVED, adminId,
                  "Resolution: " + request.getResolution().name() + (request.getNotes() != null ? " - " + request.getNotes() : ""));

    return DisputeDto.fromEntity(dispute);
  }

  @Transactional(readOnly = true)
  public Page<DisputeDto> getDisputesWithFilters(UUID buyerId, UUID sellerId, DisputeStatus status,
                                                 DisputeType type, Pageable pageable)
  {
    return disputeRepository.findWithFilters(buyerId, sellerId, status, type, pageable)
                            .map(DisputeDto::fromEntity);
  }

  @Transactional(readOnly = true)
  public DisputeStatsDto getDisputeStats()
  {
    return DisputeStatsDto.builder()
                          .totalOpen(disputeRepository.countOpenDisputes())
                          .openCount(disputeRepository.countByStatus(DisputeStatus.OPEN))
                          .underReviewCount(disputeRepository.countByStatus(DisputeStatus.UNDER_REVIEW))
                          .escalatedCount(disputeRepository.countByStatus(DisputeStatus.ESCALATED))
                          .awaitingResponseCount(disputeRepository.countByStatus(DisputeStatus.AWAITING_RESPONSE))
                          .resolvedCount(disputeRepository.countByStatus(DisputeStatus.RESOLVED))
                          .closedCount(disputeRepository.countByStatus(DisputeStatus.CLOSED))
                          .unassignedCount(disputeRepository.countUnassigned())
                          .build();
  }

  @Transactional(readOnly = true)
  public List<DisputeHistory> getDisputeHistory(UUID disputeId)
  {
    return historyRepository.findByDisputeIdOrderByCreatedAtDesc(disputeId);
  }

  private void recordHistory(UUID disputeId, String action, DisputeStatus oldStatus,
                             DisputeStatus newStatus, UUID performedBy, String notes)
  {
    DisputeHistory history = DisputeHistory.builder()
                                           .disputeId(disputeId)
                                           .action(action)
                                           .oldStatus(oldStatus)
                                           .newStatus(newStatus)
                                           .performedBy(performedBy)
                                           .notes(notes)
                                           .build();
    historyRepository.save(history);
  }
  private final DisputeRepository disputeRepository;
  private final DisputeMessageRepository messageRepository;
  private final DisputeHistoryRepository historyRepository;
  private final OrderRepository orderRepository;
}
