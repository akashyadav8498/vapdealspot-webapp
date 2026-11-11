package com.vapedealspot.cms;

import com.vapedealspot.cms.dto.ProductDisplayDto;
import com.vapedealspot.product.ProductService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CmsProductResolver {

    private final ProductService productService;

    public CmsProductResolver(ProductService productService) {
        this.productService = productService;
    }

    public List<ProductDisplayDto> resolve(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        return productService.resolveCanonicalProducts(productIds);
    }
}
