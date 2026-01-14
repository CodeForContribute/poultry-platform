package com.poultry.product.repository;

import com.poultry.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findByCode(Category.CategoryCode code);

    @Query("SELECT c FROM Category c WHERE c.status = 'ACTIVE' ORDER BY c.displayOrder")
    List<Category> findAllActiveOrdered();

    boolean existsByCode(Category.CategoryCode code);
}
