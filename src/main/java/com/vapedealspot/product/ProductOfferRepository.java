package com.vapedealspot.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductOfferRepository extends JpaRepository<ProductOffer, String> {
    List<ProductOffer> findByProductId(String productId);
    List<ProductOffer> findByProductIdAndStatus(String productId, String status);
}

