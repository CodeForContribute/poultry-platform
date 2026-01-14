package com.poultry.verification.service;

import com.poultry.common.exception.BusinessException;
import com.poultry.product.entity.Seller;
import com.poultry.product.repository.SellerRepository;
import com.poultry.verification.dto.SubmitVerificationRequest;
import com.poultry.verification.entity.SellerVerification;
import com.poultry.verification.repository.SellerVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerVerificationService {

    private final SellerVerificationRepository verificationRepository;
    private final SellerRepository sellerRepository;

    @Transactional
    public SellerVerification submitVerification(SubmitVerificationRequest request) {
        Seller seller = sellerRepository.findById(request.getSellerId())
                .orElseThrow(() -> BusinessException.notFound("Seller", request.getSellerId()));

        // Check if verification already exists
        verificationRepository.findBySellerIdAndVerificationType(request.getSellerId(), request.getVerificationType())
                .ifPresent(existing -> {
                    if (existing.isPending() || existing.isApproved()) {
                        throw new BusinessException(
                                "Verification already submitted or approved",
                                "VERIFICATION_EXISTS",
                                HttpStatus.CONFLICT
                        );
                    }
                });

        SellerVerification verification = SellerVerification.builder()
                .sellerId(request.getSellerId())
                .verificationType(request.getVerificationType())
                .documentIds(request.getDocumentIds())
                .status(SellerVerification.VerificationStatus.PENDING)
                .build();

        verification = verificationRepository.save(verification);
        log.info("Verification submitted: {} for seller: {}", verification.getId(), request.getSellerId());

        return verification;
    }

    @Transactional(readOnly = true)
    public List<SellerVerification> getVerificationStatus(UUID sellerId) {
        return verificationRepository.findBySellerId(sellerId);
    }

    @Transactional
    public SellerVerification approveVerification(UUID verificationId, UUID adminId, String remarks) {
        SellerVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> BusinessException.notFound("Verification", verificationId));

        if (!verification.isPending()) {
            throw BusinessException.invalidState("Verification is not in pending state");
        }

        verification.setStatus(SellerVerification.VerificationStatus.APPROVED);
        verification.setVerifiedBy(adminId);
        verification.setVerifiedAt(Instant.now());
        verification.setRemarks(remarks);

        // Set expiry for FSSAI (typically valid for 1-5 years)
        if (verification.getVerificationType() == SellerVerification.VerificationType.FSSAI) {
            verification.setExpiresAt(Instant.now().plus(365, ChronoUnit.DAYS));
        }

        verification = verificationRepository.save(verification);

        // Update seller status if all verifications are complete
        updateSellerStatusIfComplete(verification.getSellerId());

        log.info("Verification approved: {} by admin: {}", verificationId, adminId);

        return verification;
    }

    @Transactional
    public SellerVerification rejectVerification(UUID verificationId, UUID adminId, String reason) {
        SellerVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> BusinessException.notFound("Verification", verificationId));

        if (!verification.isPending()) {
            throw BusinessException.invalidState("Verification is not in pending state");
        }

        verification.setStatus(SellerVerification.VerificationStatus.REJECTED);
        verification.setVerifiedBy(adminId);
        verification.setVerifiedAt(Instant.now());
        verification.setRejectionReason(reason);

        verification = verificationRepository.save(verification);
        log.info("Verification rejected: {} by admin: {} - {}", verificationId, adminId, reason);

        return verification;
    }

    @Transactional(readOnly = true)
    public List<SellerVerification> getPendingVerifications() {
        return verificationRepository.findByStatus(SellerVerification.VerificationStatus.PENDING);
    }

    @Transactional
    public SellerVerification startReview(UUID verificationId, UUID adminId) {
        SellerVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> BusinessException.notFound("Verification", verificationId));

        if (verification.getStatus() != SellerVerification.VerificationStatus.PENDING) {
            throw BusinessException.invalidState("Verification is not in pending state");
        }

        verification.setStatus(SellerVerification.VerificationStatus.UNDER_REVIEW);
        verification = verificationRepository.save(verification);

        log.info("Verification review started: {} by admin: {}", verificationId, adminId);

        return verification;
    }

    private void updateSellerStatusIfComplete(UUID sellerId) {
        List<SellerVerification> verifications = verificationRepository.findBySellerId(sellerId);

        boolean kycApproved = verifications.stream()
                .anyMatch(v -> v.getVerificationType() == SellerVerification.VerificationType.KYC && v.isApproved());

        boolean bankApproved = verifications.stream()
                .anyMatch(v -> v.getVerificationType() == SellerVerification.VerificationType.BANK && v.isApproved());

        if (kycApproved && bankApproved) {
            Seller seller = sellerRepository.findById(sellerId).orElse(null);
            if (seller != null && seller.getStatus() == Seller.SellerStatus.PENDING_VERIFICATION) {
                seller.setStatus(Seller.SellerStatus.ACTIVE);
                sellerRepository.save(seller);
                log.info("Seller {} activated after verification completion", sellerId);
            }
        }
    }
}
