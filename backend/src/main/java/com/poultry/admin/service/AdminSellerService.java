package com.poultry.admin.service;

import com.poultry.admin.dto.SellerListDto;
import com.poultry.common.exception.BusinessException;
import com.poultry.product.entity.Seller;
import com.poultry.product.repository.SellerRepository;
import com.poultry.verification.entity.SellerVerification;
import com.poultry.verification.repository.SellerVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSellerService
{

  @Transactional(readOnly = true)
  public Page<SellerListDto> getSellers(String search, String status, Pageable pageable)
  {
    log.info("Fetching sellers with search: {}, status: {}", search, status);

    return sellerRepository.findAll(pageable).map(this::toSellerListDto);
  }

  @Transactional(readOnly = true)
  public Seller getSellerById(UUID sellerId)
  {
    return sellerRepository.findById(sellerId)
                           .orElseThrow(() -> new BusinessException("Seller not found", "SELLER_NOT_FOUND", HttpStatus.NOT_FOUND));
  }

  @Transactional
  public void updateSellerStatus(UUID sellerId, Seller.SellerStatus status, UUID adminId)
  {
    log.info("Updating seller {} status to {} by admin {}", sellerId, status, adminId);

    Seller seller = getSellerById(sellerId);
    seller.setStatus(status);
    sellerRepository.save(seller);

    log.info("Seller {} status updated to {}", sellerId, status);
  }

  @Transactional
  public void updatePlatformFee(UUID sellerId, java.math.BigDecimal feePercent, UUID adminId)
  {
    log.info("Updating seller {} platform fee to {}% by admin {}", sellerId, feePercent, adminId);

    if (feePercent.compareTo(java.math.BigDecimal.ZERO) < 0 || feePercent.compareTo(java.math.BigDecimal.valueOf(10)) > 0)
    {
      throw new BusinessException("Platform fee must be between 0 and 10%", "INVALID_FEE", HttpStatus.BAD_REQUEST);
    }

    Seller seller = getSellerById(sellerId);
    seller.setPlatformFeePercent(feePercent);
    sellerRepository.save(seller);

    log.info("Seller {} platform fee updated to {}%", sellerId, feePercent);
  }

  @Transactional(readOnly = true)
  public List<SellerVerification> getSellerVerifications(UUID sellerId)
  {
    return verificationRepository.findBySellerId(sellerId);
  }

  @Transactional(readOnly = true)
  public Page<SellerVerification> getPendingVerifications(Pageable pageable)
  {
    return verificationRepository.findByStatusIn(
        List.of(SellerVerification.VerificationStatus.PENDING,
                SellerVerification.VerificationStatus.UNDER_REVIEW),
        pageable);
  }

  @Transactional
  public SellerVerification approveVerification(UUID verificationId, UUID adminId, String remarks)
  {
    log.info("Approving verification {} by admin {}", verificationId, adminId);

    SellerVerification verification = verificationRepository.findById(verificationId)
                                                            .orElseThrow(() -> new BusinessException("Verification not found", "VERIFICATION_NOT_FOUND", HttpStatus.NOT_FOUND));

    if (verification.getStatus() != SellerVerification.VerificationStatus.PENDING &&
        verification.getStatus() != SellerVerification.VerificationStatus.UNDER_REVIEW)
    {
      throw new BusinessException("Verification is not pending", "INVALID_STATUS", HttpStatus.BAD_REQUEST);
    }

    verification.setStatus(SellerVerification.VerificationStatus.APPROVED);
    verification.setVerifiedBy(adminId);
    verification.setVerifiedAt(java.time.Instant.now());
    verification.setRemarks(remarks);

    verification = verificationRepository.save(verification);

    // Check if all verifications are complete and update seller status
    updateSellerVerificationStatus(verification.getSellerId());

    log.info("Verification {} approved", verificationId);
    return verification;
  }

  @Transactional
  public SellerVerification rejectVerification(UUID verificationId, UUID adminId, String reason)
  {
    log.info("Rejecting verification {} by admin {}", verificationId, adminId);

    SellerVerification verification = verificationRepository.findById(verificationId)
                                                            .orElseThrow(() -> new BusinessException("Verification not found", "VERIFICATION_NOT_FOUND", HttpStatus.NOT_FOUND));

    if (verification.getStatus() != SellerVerification.VerificationStatus.PENDING &&
        verification.getStatus() != SellerVerification.VerificationStatus.UNDER_REVIEW)
    {
      throw new BusinessException("Verification is not pending", "INVALID_STATUS", HttpStatus.BAD_REQUEST);
    }

    verification.setStatus(SellerVerification.VerificationStatus.REJECTED);
    verification.setVerifiedBy(adminId);
    verification.setVerifiedAt(java.time.Instant.now());
    verification.setRejectionReason(reason);

    verification = verificationRepository.save(verification);

    log.info("Verification {} rejected", verificationId);
    return verification;
  }

  private void updateSellerVerificationStatus(UUID sellerId)
  {
    List<SellerVerification> verifications = verificationRepository.findBySellerId(sellerId);

    boolean kycApproved = verifications.stream()
                                       .anyMatch(v -> v.getVerificationType() == SellerVerification.VerificationType.KYC &&
                                           v.getStatus() == SellerVerification.VerificationStatus.APPROVED);

    boolean bankApproved = verifications.stream()
                                        .anyMatch(v -> v.getVerificationType() == SellerVerification.VerificationType.BANK &&
                                            v.getStatus() == SellerVerification.VerificationStatus.APPROVED);

    if (kycApproved && bankApproved)
    {
      Seller seller = getSellerById(sellerId);
      if (seller.getStatus() == Seller.SellerStatus.PENDING_VERIFICATION)
      {
        seller.setStatus(Seller.SellerStatus.ACTIVE);
        sellerRepository.save(seller);
        log.info("Seller {} activated after verification completion", sellerId);
      }
    }
  }

  private SellerListDto toSellerListDto(Seller seller)
  {
    return SellerListDto.builder()
                        .id(seller.getId())
                        .businessName(seller.getBusinessName())
                        .gstin(seller.getGstin())
                        .email(seller.getEmail())
                        .status(seller.getStatus().name())
                        .platformFeePercent(seller.getPlatformFeePercent())
                        .createdAt(seller.getCreatedAt())
                        .build();
  }
  private final SellerRepository sellerRepository;
  private final SellerVerificationRepository verificationRepository;
}
