package com.poultry.auth.repository;

import com.poultry.auth.entity.Buyer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BuyerRepository extends JpaRepository<Buyer, UUID> {

    Optional<Buyer> findByPhoneHash(String phoneHash);

    boolean existsByPhoneHash(String phoneHash);

    Optional<Buyer> findByIdAndStatus(UUID id, Buyer.BuyerStatus status);
}
