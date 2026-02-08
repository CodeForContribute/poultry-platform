package com.poultry.seller.repository;

import com.poultry.seller.entity.SellerBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerBankAccountRepository extends JpaRepository<SellerBankAccount, UUID> {

    List<SellerBankAccount> findBySellerId(UUID sellerId);

    Optional<SellerBankAccount> findByIdAndSellerId(UUID id, UUID sellerId);

    Optional<SellerBankAccount> findBySellerIdAndIsPrimaryTrue(UUID sellerId);

    @Modifying
    @Query("UPDATE SellerBankAccount ba SET ba.isPrimary = false WHERE ba.sellerId = :sellerId")
    void clearPrimaryForSeller(@Param("sellerId") UUID sellerId);

    long countBySellerId(UUID sellerId);

    boolean existsBySellerIdAndAccountNumberLast4AndIfscCode(UUID sellerId, String accountNumberLast4, String ifscCode);
}
