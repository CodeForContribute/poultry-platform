package com.poultry.seller.service;

import com.poultry.common.exception.BusinessException;
import com.poultry.common.service.EncryptionService;
import com.poultry.product.entity.Seller;
import com.poultry.product.repository.SellerRepository;
import com.poultry.seller.dto.*;
import com.poultry.seller.entity.SellerBankAccount;
import com.poultry.seller.entity.SellerNotificationPreferences;
import com.poultry.seller.repository.SellerBankAccountRepository;
import com.poultry.seller.repository.SellerNotificationPreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerProfileService {

    private final SellerRepository sellerRepository;
    private final SellerBankAccountRepository bankAccountRepository;
    private final SellerNotificationPreferencesRepository notificationPreferencesRepository;
    private final EncryptionService encryptionService;

    @Transactional(readOnly = true)
    public SellerProfileDto getProfile(UUID sellerId) {
        log.info("Fetching profile for seller: {}", sellerId);

        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> BusinessException.notFound("Seller", sellerId));

        return mapToProfileDto(seller);
    }

    @Transactional
    public SellerProfileDto updateProfile(UUID sellerId, UpdateSellerProfileRequest request) {
        log.info("Updating profile for seller: {}", sellerId);

        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> BusinessException.notFound("Seller", sellerId));

        if (request.getBusinessName() != null) {
            seller.setBusinessName(request.getBusinessName());
        }
        if (request.getEmail() != null) {
            seller.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            seller.setPhoneEncrypted(encryptionService.encrypt(request.getPhone()));
        }
        if (request.getAddress() != null) {
            seller.setAddress(request.getAddress());
        }
        if (request.getFssaiNumber() != null) {
            seller.setFssaiNumber(request.getFssaiNumber());
        }

        seller = sellerRepository.save(seller);
        log.info("Profile updated for seller: {}", sellerId);

        return mapToProfileDto(seller);
    }

    // ============ Bank Account Methods ============

    @Transactional(readOnly = true)
    public List<BankAccountDto> getBankAccounts(UUID sellerId) {
        log.info("Fetching bank accounts for seller: {}", sellerId);

        return bankAccountRepository.findBySellerId(sellerId).stream()
                .map(this::mapToBankAccountDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BankAccountDto addBankAccount(UUID sellerId, AddBankAccountRequest request) {
        log.info("Adding bank account for seller: {}", sellerId);

        // Check if seller exists
        if (!sellerRepository.existsById(sellerId)) {
            throw BusinessException.notFound("Seller", sellerId);
        }

        // Check for duplicate
        String last4 = request.getAccountNumber().substring(request.getAccountNumber().length() - 4);
        if (bankAccountRepository.existsBySellerIdAndAccountNumberLast4AndIfscCode(
                sellerId, last4, request.getIfscCode())) {
            throw new BusinessException(
                    "A bank account with this account number and IFSC already exists",
                    "DUPLICATE_BANK_ACCOUNT",
                    HttpStatus.CONFLICT
            );
        }

        // If this is the first account or set as primary, clear other primaries
        boolean isPrimary = request.isSetPrimary() || bankAccountRepository.countBySellerId(sellerId) == 0;
        if (isPrimary) {
            bankAccountRepository.clearPrimaryForSeller(sellerId);
        }

        SellerBankAccount bankAccount = SellerBankAccount.builder()
                .sellerId(sellerId)
                .accountHolderName(request.getAccountHolderName())
                .bankName(request.getBankName())
                .accountNumberEncrypted(encryptionService.encrypt(request.getAccountNumber()))
                .accountNumberLast4(last4)
                .ifscCode(request.getIfscCode())
                .accountType(SellerBankAccount.AccountType.valueOf(request.getAccountType()))
                .isPrimary(isPrimary)
                .isVerified(false)
                .build();

        bankAccount = bankAccountRepository.save(bankAccount);
        log.info("Bank account {} added for seller: {}", bankAccount.getId(), sellerId);

        return mapToBankAccountDto(bankAccount);
    }

    @Transactional
    public void deleteBankAccount(UUID sellerId, UUID bankAccountId) {
        log.info("Deleting bank account {} for seller: {}", bankAccountId, sellerId);

        SellerBankAccount bankAccount = bankAccountRepository.findByIdAndSellerId(bankAccountId, sellerId)
                .orElseThrow(() -> BusinessException.notFound("Bank account", bankAccountId));

        // If deleting the primary account, set another one as primary
        if (bankAccount.isPrimary()) {
            List<SellerBankAccount> otherAccounts = bankAccountRepository.findBySellerId(sellerId).stream()
                    .filter(ba -> !ba.getId().equals(bankAccountId))
                    .collect(Collectors.toList());

            if (!otherAccounts.isEmpty()) {
                SellerBankAccount newPrimary = otherAccounts.get(0);
                newPrimary.setPrimary(true);
                bankAccountRepository.save(newPrimary);
            }
        }

        bankAccountRepository.delete(bankAccount);
        log.info("Bank account {} deleted for seller: {}", bankAccountId, sellerId);
    }

    @Transactional
    public BankAccountDto setPrimaryBankAccount(UUID sellerId, UUID bankAccountId) {
        log.info("Setting primary bank account {} for seller: {}", bankAccountId, sellerId);

        SellerBankAccount bankAccount = bankAccountRepository.findByIdAndSellerId(bankAccountId, sellerId)
                .orElseThrow(() -> BusinessException.notFound("Bank account", bankAccountId));

        // Clear all primaries and set this one
        bankAccountRepository.clearPrimaryForSeller(sellerId);
        bankAccount.setPrimary(true);
        bankAccount = bankAccountRepository.save(bankAccount);

        log.info("Primary bank account set to {} for seller: {}", bankAccountId, sellerId);
        return mapToBankAccountDto(bankAccount);
    }

    // ============ Notification Preferences Methods ============

    @Transactional(readOnly = true)
    public NotificationPreferencesDto getNotificationPreferences(UUID sellerId) {
        log.info("Fetching notification preferences for seller: {}", sellerId);

        return notificationPreferencesRepository.findBySellerId(sellerId)
                .map(this::mapToNotificationPreferencesDto)
                .orElse(NotificationPreferencesDto.defaultPreferences());
    }

    @Transactional
    public NotificationPreferencesDto updateNotificationPreferences(UUID sellerId, NotificationPreferencesDto request) {
        log.info("Updating notification preferences for seller: {}", sellerId);

        SellerNotificationPreferences preferences = notificationPreferencesRepository.findBySellerId(sellerId)
                .orElse(SellerNotificationPreferences.builder().sellerId(sellerId).build());

        // Update email preferences
        preferences.setEmailNewOrders(request.isEmailNewOrders());
        preferences.setEmailOrderUpdates(request.isEmailOrderUpdates());
        preferences.setEmailPaymentReceived(request.isEmailPaymentReceived());
        preferences.setEmailSettlementCompleted(request.isEmailSettlementCompleted());
        preferences.setEmailDisputeRaised(request.isEmailDisputeRaised());
        preferences.setEmailPromotions(request.isEmailPromotions());

        // Update SMS preferences
        preferences.setSmsNewOrders(request.isSmsNewOrders());
        preferences.setSmsOrderUpdates(request.isSmsOrderUpdates());
        preferences.setSmsPaymentReceived(request.isSmsPaymentReceived());
        preferences.setSmsSettlementCompleted(request.isSmsSettlementCompleted());
        preferences.setSmsDisputeRaised(request.isSmsDisputeRaised());

        // Update push preferences
        preferences.setPushNewOrders(request.isPushNewOrders());
        preferences.setPushOrderUpdates(request.isPushOrderUpdates());
        preferences.setPushPaymentReceived(request.isPushPaymentReceived());
        preferences.setPushSettlementCompleted(request.isPushSettlementCompleted());
        preferences.setPushDisputeRaised(request.isPushDisputeRaised());

        preferences = notificationPreferencesRepository.save(preferences);
        log.info("Notification preferences updated for seller: {}", sellerId);

        return mapToNotificationPreferencesDto(preferences);
    }

    // ============ Mapping Methods ============

    private SellerProfileDto mapToProfileDto(Seller seller) {
        String phone = null;
        if (seller.getPhoneEncrypted() != null) {
            try {
                phone = encryptionService.decrypt(seller.getPhoneEncrypted());
            } catch (Exception e) {
                log.warn("Failed to decrypt phone for seller {}", seller.getId());
            }
        }

        String bankAccountLast4 = null;
        if (seller.getBankAccountNumberEncrypted() != null) {
            try {
                String fullAccountNumber = encryptionService.decrypt(seller.getBankAccountNumberEncrypted());
                bankAccountLast4 = fullAccountNumber.substring(fullAccountNumber.length() - 4);
            } catch (Exception e) {
                log.warn("Failed to decrypt bank account for seller {}", seller.getId());
            }
        }

        return SellerProfileDto.builder()
                .id(seller.getId())
                .businessName(seller.getBusinessName())
                .gstin(seller.getGstin())
                .pan(seller.getPan())
                .phone(phone)
                .email(seller.getEmail())
                .address(seller.getAddress())
                .fssaiNumber(seller.getFssaiNumber())
                .status(seller.getStatus())
                .settlementCycle(seller.getSettlementCycle())
                .platformFeePercent(seller.getPlatformFeePercent())
                .createdAt(seller.getCreatedAt())
                .updatedAt(seller.getUpdatedAt())
                .bankAccountLast4(bankAccountLast4)
                .bankIfsc(seller.getBankIfsc())
                .bankName(seller.getBankName())
                .bankAccountHolder(seller.getBankAccountHolder())
                .hasBankAccount(seller.getBankAccountNumberEncrypted() != null)
                .build();
    }

    private BankAccountDto mapToBankAccountDto(SellerBankAccount bankAccount) {
        return BankAccountDto.builder()
                .id(bankAccount.getId())
                .accountHolderName(bankAccount.getAccountHolderName())
                .bankName(bankAccount.getBankName())
                .accountNumberLast4(bankAccount.getAccountNumberLast4())
                .ifscCode(bankAccount.getIfscCode())
                .accountType(bankAccount.getAccountType().name())
                .isPrimary(bankAccount.isPrimary())
                .isVerified(bankAccount.isVerified())
                .createdAt(bankAccount.getCreatedAt())
                .build();
    }

    private NotificationPreferencesDto mapToNotificationPreferencesDto(SellerNotificationPreferences preferences) {
        return NotificationPreferencesDto.builder()
                .emailNewOrders(preferences.isEmailNewOrders())
                .emailOrderUpdates(preferences.isEmailOrderUpdates())
                .emailPaymentReceived(preferences.isEmailPaymentReceived())
                .emailSettlementCompleted(preferences.isEmailSettlementCompleted())
                .emailDisputeRaised(preferences.isEmailDisputeRaised())
                .emailPromotions(preferences.isEmailPromotions())
                .smsNewOrders(preferences.isSmsNewOrders())
                .smsOrderUpdates(preferences.isSmsOrderUpdates())
                .smsPaymentReceived(preferences.isSmsPaymentReceived())
                .smsSettlementCompleted(preferences.isSmsSettlementCompleted())
                .smsDisputeRaised(preferences.isSmsDisputeRaised())
                .pushNewOrders(preferences.isPushNewOrders())
                .pushOrderUpdates(preferences.isPushOrderUpdates())
                .pushPaymentReceived(preferences.isPushPaymentReceived())
                .pushSettlementCompleted(preferences.isPushSettlementCompleted())
                .pushDisputeRaised(preferences.isPushDisputeRaised())
                .build();
    }
}
