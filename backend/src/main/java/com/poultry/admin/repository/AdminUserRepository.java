package com.poultry.admin.repository;

import com.poultry.admin.entity.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminUserRepository
    extends JpaRepository<AdminUser, UUID>
{

  Optional<AdminUser> findByEmail(String email);

  boolean existsByEmail(String email);

  @Query("SELECT a FROM AdminUser a WHERE a.status = :status")
  List<AdminUser> findByStatus(
      @Param("status")
      AdminUser.Status status);

  @Query("SELECT a FROM AdminUser a WHERE a.role = :role AND a.status = 'ACTIVE'")
  List<AdminUser> findActiveByRole(
      @Param("role")
      AdminUser.AdminRole role);

  @Query("SELECT a FROM AdminUser a WHERE " +
      "(:search IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
      "OR LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
      "AND (:role IS NULL OR a.role = :role) " +
      "AND (:status IS NULL OR a.status = :status)")
  Page<AdminUser> findWithFilters(
      @Param("search")
      String search,
      @Param("role")
      AdminUser.AdminRole role,
      @Param("status")
      AdminUser.Status status,
      Pageable pageable);

  @Query("SELECT COUNT(a) FROM AdminUser a WHERE a.status = 'ACTIVE'")
  Long countActive();
}
