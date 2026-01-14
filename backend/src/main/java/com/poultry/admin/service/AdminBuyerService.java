package com.poultry.admin.service;

import com.poultry.admin.dto.BuyerListDto;
import com.poultry.auth.entity.Buyer;
import com.poultry.auth.repository.BuyerRepository;
import com.poultry.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBuyerService
{

  @Transactional(readOnly = true)
  public Page<BuyerListDto> getBuyers(String search, String status, Pageable pageable)
  {
    log.info("Fetching buyers with search: {}, status: {}", search, status);

    return buyerRepository.findAll(pageable).map(this::toBuyerListDto);
  }

  @Transactional(readOnly = true)
  public Buyer getBuyerById(UUID buyerId)
  {
    return buyerRepository.findById(buyerId)
                          .orElseThrow(() -> new BusinessException("Buyer not found", "BUYER_NOT_FOUND", HttpStatus.NOT_FOUND));
  }

  @Transactional
  public void updateBuyerStatus(UUID buyerId, Buyer.BuyerStatus status, UUID adminId)
  {
    log.info("Updating buyer {} status to {} by admin {}", buyerId, status, adminId);

    Buyer buyer = getBuyerById(buyerId);
    buyer.setStatus(status);
    buyerRepository.save(buyer);

    log.info("Buyer {} status updated to {}", buyerId, status);
  }

  @Transactional
  public void blockBuyer(UUID buyerId, String reason, UUID adminId)
  {
    log.info("Blocking buyer {} by admin {}: {}", buyerId, adminId, reason);

    Buyer buyer = getBuyerById(buyerId);
    buyer.setStatus(Buyer.BuyerStatus.BLOCKED);
    buyerRepository.save(buyer);

    log.info("Buyer {} blocked", buyerId);
  }

  @Transactional
  public void unblockBuyer(UUID buyerId, UUID adminId)
  {
    log.info("Unblocking buyer {} by admin {}", buyerId, adminId);

    Buyer buyer = getBuyerById(buyerId);
    if (buyer.getStatus() != Buyer.BuyerStatus.BLOCKED)
    {
      throw new BusinessException("Buyer is not blocked", "NOT_BLOCKED", HttpStatus.BAD_REQUEST);
    }

    buyer.setStatus(Buyer.BuyerStatus.ACTIVE);
    buyerRepository.save(buyer);

    log.info("Buyer {} unblocked", buyerId);
  }

  private BuyerListDto toBuyerListDto(Buyer buyer)
  {
    return BuyerListDto.builder()
                       .id(buyer.getId())
                       .name(buyer.getName())
                       .businessName(buyer.getBusinessName())
                       .status(buyer.getStatus().name())
                       .createdAt(buyer.getCreatedAt())
                       .build();
  }
  private final BuyerRepository buyerRepository;
}
