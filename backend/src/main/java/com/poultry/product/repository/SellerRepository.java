package com.poultry.product.repository;

import com.poultry.product.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerRepository extends JpaRepository<Seller, UUID> {

    Optional<Seller> findByGstin(String gstin);

    List<Seller> findByStatus(Seller.SellerStatus status);

    boolean existsByGstin(String gstin);

    Optional<Seller> findByIdAndStatus(UUID id, Seller.SellerStatus status);
}
