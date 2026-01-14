package com.poultry.review.repository;

import com.poultry.review.entity.SellerRatingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SellerRatingSummaryRepository
    extends JpaRepository<SellerRatingSummary, UUID>
{
}
