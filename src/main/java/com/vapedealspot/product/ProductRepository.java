package com.vapedealspot.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findBySlug(String slug);

    @Query("SELECT p FROM Product p WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:category IS NULL OR :category = '' OR p.category = :category) AND " +
           "(:brand IS NULL OR :brand = '' OR p.brand = :brand) AND " +
           "(:status IS NULL OR :status = '' OR p.status = :status)")
    Page<Product> findWithFilters(@Param("search") String search,
                                  @Param("category") String category,
                                  @Param("brand") String brand,
                                  @Param("status") String status,
                                  Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.offers WHERE p.id IN :ids")
    List<Product> findByIdInWithOffers(@Param("ids") Collection<String> ids);

    Page<Product> findByCategoryAndStatus(String category, String status, Pageable pageable);

    Page<Product> findByBrandAndStatus(String brand, String status, Pageable pageable);

    long countByCategoryAndStatus(String category, String status);

    long countByBrandAndStatus(String brand, String status);

    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.status = 'active' ORDER BY p.brand ASC")
    List<String> findDistinctActiveBrands();

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.status = 'active' ORDER BY p.category ASC")
    List<String> findDistinctActiveCategories();
}

