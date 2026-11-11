package com.vapedealspot.product;

import com.vapedealspot.cms.dto.ProductDisplayDto;
import com.vapedealspot.product.dto.OfferFormDto;
import com.vapedealspot.product.dto.ProductFormDto;
import com.vapedealspot.product.dto.ProductListItemDto;
import com.vapedealspot.product.dto.VariantFormDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.*;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductOfferRepository offerRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    public ProductService(ProductRepository productRepository,
                          ProductVariantRepository variantRepository,
                          ProductOfferRepository offerRepository,
                          CategoryRepository categoryRepository,
                          BrandRepository brandRepository) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.offerRepository = offerRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductListItemDto> findProducts(String search, String category, String brand,
                                                 String status, Pageable pageable) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String cleanCategory = (category != null && !category.trim().isEmpty() && !"All".equalsIgnoreCase(category)) ? category.trim() : null;
        String cleanBrand = (brand != null && !brand.trim().isEmpty() && !"All".equalsIgnoreCase(brand)) ? brand.trim() : null;
        String cleanStatus = (status != null && !status.trim().isEmpty() && !"All".equalsIgnoreCase(status)) ? status.trim() : null;

        Page<Product> page = productRepository.findWithFilters(cleanSearch, cleanCategory, cleanBrand, cleanStatus, pageable);

        List<ProductListItemDto> dtos = new ArrayList<>();
        for (Product product : page.getContent()) {
            List<ProductOffer> offers = offerRepository.findByProductId(product.getId());

            BigDecimal lowestPrice = null;
            int activeOffersCount = 0;

            for (ProductOffer offer : offers) {
                if ("active".equalsIgnoreCase(offer.getStatus())) {
                    activeOffersCount++;
                    if (offer.getPrice() != null) {
                        if (lowestPrice == null || offer.getPrice().compareTo(lowestPrice) < 0) {
                            lowestPrice = offer.getPrice();
                        }
                    }
                }
            }

            if (lowestPrice == null) {
                List<ProductVariant> variants = variantRepository.findByProductId(product.getId());
                for (ProductVariant variant : variants) {
                    if (variant.getPrice() != null && variant.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                        if (lowestPrice == null || variant.getPrice().compareTo(lowestPrice) < 0) {
                            lowestPrice = variant.getPrice();
                        }
                    }
                }
            }

            dtos.add(new ProductListItemDto(
                    product.getId(),
                    product.getName(),
                    product.getBrand(),
                    product.getCategory(),
                    product.getSku(),
                    product.getImage(),
                    lowestPrice,
                    activeOffersCount,
                    product.getStatus(),
                    product.isFeatured()
            ));
        }

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ProductFormDto> getProductForEdit(String id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty()) {
            return Optional.empty();
        }

        Product product = productOpt.get();
        ProductFormDto form = new ProductFormDto();
        form.setId(product.getId());
        form.setName(product.getName());
        form.setSlug(product.getSlug());
        form.setBrand(product.getBrand());
        form.setCategory(product.getCategory());
        form.setSku(product.getSku());
        form.setShortDescription(product.getShortDescription());
        form.setDescription(product.getDescription());
        form.setTags(product.getTags());
        form.setImage(product.getImage());
        form.setStatus(product.getStatus());
        form.setFeatured(product.isFeatured());
        form.setTrending(product.isTrending());
        form.setShowOnHomepage(product.isShowOnHomepage());

        List<ProductVariant> variants = variantRepository.findByProductId(product.getId());
        List<VariantFormDto> variantDtos = new ArrayList<>();
        for (ProductVariant v : variants) {
            variantDtos.add(new VariantFormDto(
                    v.getId(),
                    v.getSku(),
                    v.getName(),
                    v.getAttributeName(),
                    v.getAttributeValue(),
                    v.getPrice(),
                    v.getStock(),
                    v.getImage(),
                    v.getStatus()
            ));
        }
        form.setVariants(variantDtos);

        List<ProductOffer> offers = offerRepository.findByProductId(product.getId());
        List<OfferFormDto> offerDtos = new ArrayList<>();
        for (ProductOffer o : offers) {
            offerDtos.add(new OfferFormDto(
                    o.getId(),
                    o.getMerchantName(),
                    o.getMerchantLogo(),
                    o.getPrice(),
                    o.getOriginalPrice(),
                    o.getAffiliateUrl(),
                    o.getAvailability(),
                    o.getShippingInfo(),
                    o.getStatus()
            ));
        }
        form.setOffers(offerDtos);

        return Optional.of(form);
    }

    @Transactional
    public Product createProduct(ProductFormDto form) {
        validateForm(form);

        String slug = (form.getSlug() != null && !form.getSlug().trim().isEmpty())
                ? slugify(form.getSlug())
                : slugify(form.getBrand() + " " + form.getName());

        slug = ensureUniqueSlug(slug, null);

        String productId = (form.getId() != null && !form.getId().trim().isEmpty())
                ? form.getId().trim()
                : "prod_" + UUID.randomUUID().toString().substring(0, 12);

        Product product = new Product();
        product.setId(productId);
        product.setName(form.getName().trim());
        product.setSlug(slug);
        product.setBrand(form.getBrand().trim());
        product.setCategory(form.getCategory().trim());
        product.setSku(form.getSku() != null ? form.getSku().trim() : null);
        product.setShortDescription(form.getShortDescription() != null ? form.getShortDescription().trim() : null);
        product.setDescription(form.getDescription().trim());
        product.setTags(form.getTags() != null ? form.getTags().trim() : null);
        product.setImage(form.getImage().trim());
        product.setStatus((form.getStatus() != null && !form.getStatus().trim().isEmpty()) ? form.getStatus().trim() : "active");
        product.setFeatured(form.isFeatured());
        product.setTrending(form.isTrending());
        product.setShowOnHomepage(form.isShowOnHomepage());

        Product saved = productRepository.save(product);

        // Save Variants
        if (form.getVariants() != null) {
            for (VariantFormDto vDto : form.getVariants()) {
                if (vDto.getSku() != null && !vDto.getSku().trim().isEmpty()) {
                    String vId = (vDto.getId() != null && !vDto.getId().trim().isEmpty())
                            ? vDto.getId()
                            : "var_" + UUID.randomUUID().toString().substring(0, 12);
                    ProductVariant variant = new ProductVariant(
                            vId,
                            saved,
                            vDto.getSku().trim(),
                            vDto.getName() != null ? vDto.getName().trim() : vDto.getSku().trim(),
                            vDto.getAttributeName() != null ? vDto.getAttributeName().trim() : "Color",
                            vDto.getAttributeValue() != null ? vDto.getAttributeValue().trim() : "",
                            vDto.getPrice() != null ? vDto.getPrice() : BigDecimal.ZERO,
                            vDto.getStock() != null ? vDto.getStock() : 0,
                            vDto.getImage() != null ? vDto.getImage().trim() : saved.getImage(),
                            vDto.getStatus() != null ? vDto.getStatus().trim() : "active"
                    );
                    variantRepository.save(variant);
                }
            }
        }

        // Save Offers
        if (form.getOffers() != null) {
            for (OfferFormDto oDto : form.getOffers()) {
                if (oDto.getMerchantName() != null && !oDto.getMerchantName().trim().isEmpty() && oDto.getPrice() != null) {
                    String oId = (oDto.getId() != null && !oDto.getId().trim().isEmpty())
                            ? oDto.getId()
                            : "off_" + UUID.randomUUID().toString().substring(0, 12);
                    ProductOffer offer = new ProductOffer(
                            oId,
                            saved,
                            oDto.getMerchantName().trim(),
                            oDto.getPrice(),
                            oDto.getOriginalPrice(),
                            oDto.getAffiliateUrl() != null ? oDto.getAffiliateUrl().trim() : "#",
                            oDto.getAvailability() != null ? oDto.getAvailability().trim() : "in_stock",
                            oDto.getShippingInfo() != null ? oDto.getShippingInfo().trim() : null,
                            oDto.getStatus() != null ? oDto.getStatus().trim() : "active"
                    );
                    offerRepository.save(offer);
                }
            }
        }

        log.info("Product '{}' (ID: {}) created successfully", saved.getName(), saved.getId());
        return saved;
    }

    @Transactional
    public Product updateProduct(String id, ProductFormDto form) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        validateForm(form);

        String slug = (form.getSlug() != null && !form.getSlug().trim().isEmpty())
                ? slugify(form.getSlug())
                : slugify(form.getBrand() + " " + form.getName());

        slug = ensureUniqueSlug(slug, id);

        product.setName(form.getName().trim());
        product.setSlug(slug);
        product.setBrand(form.getBrand().trim());
        product.setCategory(form.getCategory().trim());
        product.setSku(form.getSku() != null ? form.getSku().trim() : null);
        product.setShortDescription(form.getShortDescription() != null ? form.getShortDescription().trim() : null);
        product.setDescription(form.getDescription().trim());
        product.setTags(form.getTags() != null ? form.getTags().trim() : null);
        product.setImage(form.getImage().trim());
        if (form.getStatus() != null && !form.getStatus().trim().isEmpty()) {
            product.setStatus(form.getStatus().trim());
        }
        product.setFeatured(form.isFeatured());
        product.setTrending(form.isTrending());
        product.setShowOnHomepage(form.isShowOnHomepage());

        Product saved = productRepository.save(product);

        // Update variants: delete existing and replace with form variants
        List<ProductVariant> existingVariants = variantRepository.findByProductId(id);
        variantRepository.deleteAll(existingVariants);

        if (form.getVariants() != null) {
            for (VariantFormDto vDto : form.getVariants()) {
                if (vDto.getSku() != null && !vDto.getSku().trim().isEmpty()) {
                    String vId = (vDto.getId() != null && !vDto.getId().trim().isEmpty())
                            ? vDto.getId()
                            : "var_" + UUID.randomUUID().toString().substring(0, 12);
                    ProductVariant variant = new ProductVariant(
                            vId,
                            saved,
                            vDto.getSku().trim(),
                            vDto.getName() != null ? vDto.getName().trim() : vDto.getSku().trim(),
                            vDto.getAttributeName() != null ? vDto.getAttributeName().trim() : "Color",
                            vDto.getAttributeValue() != null ? vDto.getAttributeValue().trim() : "",
                            vDto.getPrice() != null ? vDto.getPrice() : BigDecimal.ZERO,
                            vDto.getStock() != null ? vDto.getStock() : 0,
                            vDto.getImage() != null ? vDto.getImage().trim() : saved.getImage(),
                            vDto.getStatus() != null ? vDto.getStatus().trim() : "active"
                    );
                    variantRepository.save(variant);
                }
            }
        }

        // Update offers: delete existing and replace with form offers
        List<ProductOffer> existingOffers = offerRepository.findByProductId(id);
        offerRepository.deleteAll(existingOffers);

        if (form.getOffers() != null) {
            for (OfferFormDto oDto : form.getOffers()) {
                if (oDto.getMerchantName() != null && !oDto.getMerchantName().trim().isEmpty() && oDto.getPrice() != null) {
                    String oId = (oDto.getId() != null && !oDto.getId().trim().isEmpty())
                            ? oDto.getId()
                            : "off_" + UUID.randomUUID().toString().substring(0, 12);
                    ProductOffer offer = new ProductOffer(
                            oId,
                            saved,
                            oDto.getMerchantName().trim(),
                            oDto.getPrice(),
                            oDto.getOriginalPrice(),
                            oDto.getAffiliateUrl() != null ? oDto.getAffiliateUrl().trim() : "#",
                            oDto.getAvailability() != null ? oDto.getAvailability().trim() : "in_stock",
                            oDto.getShippingInfo() != null ? oDto.getShippingInfo().trim() : null,
                            oDto.getStatus() != null ? oDto.getStatus().trim() : "active"
                    );
                    offerRepository.save(offer);
                }
            }
        }

        log.info("Product '{}' (ID: {}) updated successfully", saved.getName(), saved.getId());
        return saved;
    }

    @Transactional
    public Product toggleStatus(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        String newStatus = product.isActive() ? "inactive" : "active";
        product.setStatus(newStatus);
        Product saved = productRepository.save(product);
        log.info("Product '{}' (ID: {}) status toggled to '{}'", saved.getName(), saved.getId(), newStatus);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ProductDisplayDto> resolveCanonicalProducts(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Product> products = productRepository.findByIdInWithOffers(productIds);
        Map<String, Product> productMap = new HashMap<>();
        for (Product p : products) {
            productMap.put(p.getId(), p);
        }

        List<ProductDisplayDto> resolved = new ArrayList<>();
        for (String id : productIds) {
            Product product = productMap.get(id);
            if (product == null || !product.isActive()) {
                // Inactive or missing products omitted gracefully
                continue;
            }

            BigDecimal minPrice = null;
            BigDecimal maxPrice = null;
            int activeOffersCount = 0;

            if (product.getOffers() != null) {
                for (ProductOffer offer : product.getOffers()) {
                    if ("active".equalsIgnoreCase(offer.getStatus())) {
                        activeOffersCount++;
                        if (offer.getPrice() != null) {
                            if (minPrice == null || offer.getPrice().compareTo(minPrice) < 0) {
                                minPrice = offer.getPrice();
                            }
                            if (maxPrice == null || offer.getPrice().compareTo(maxPrice) > 0) {
                                maxPrice = offer.getPrice();
                            }
                        }
                        if (offer.getOriginalPrice() != null) {
                            if (maxPrice == null || offer.getOriginalPrice().compareTo(maxPrice) > 0) {
                                maxPrice = offer.getOriginalPrice();
                            }
                        }
                    }
                }
            }

            // Fallback to variants if no offers exist
            if (minPrice == null && product.getVariants() != null) {
                for (ProductVariant v : product.getVariants()) {
                    if (v.getPrice() != null && v.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                        if (minPrice == null || v.getPrice().compareTo(minPrice) < 0) {
                            minPrice = v.getPrice();
                        }
                        if (maxPrice == null || v.getPrice().compareTo(maxPrice) > 0) {
                            maxPrice = v.getPrice();
                        }
                    }
                }
            }

            String savingsBadge = null;
            if (minPrice != null && maxPrice != null && maxPrice.compareTo(minPrice) > 0) {
                BigDecimal diff = maxPrice.subtract(minPrice);
                BigDecimal ratio = diff.divide(maxPrice, 4, RoundingMode.HALF_UP);
                if (ratio.compareTo(new BigDecimal("0.15")) >= 0) {
                    int percent = ratio.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
                    savingsBadge = "Save " + percent + "%";
                }
            }

            Double minDouble = minPrice != null ? minPrice.doubleValue() : null;
            Double maxDouble = maxPrice != null ? maxPrice.doubleValue() : null;

            resolved.add(new ProductDisplayDto(
                    product.getId(),
                    product.getSlug(),
                    product.getBrand(),
                    product.getName(),
                    product.getImage(),
                    minDouble,
                    maxDouble,
                    activeOffersCount,
                    savingsBadge
            ));
        }

        return resolved;
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Brand> getAllBrands() {
        return brandRepository.findAllByOrderByNameAsc();
    }

    private void validateForm(ProductFormDto form) {
        if (form.getName() == null || form.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (form.getBrand() == null || form.getBrand().trim().isEmpty()) {
            throw new IllegalArgumentException("Brand is required");
        }
        if (form.getCategory() == null || form.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }
        if (form.getDescription() == null || form.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (form.getImage() == null || form.getImage().trim().isEmpty()) {
            throw new IllegalArgumentException("Featured image URL is required");
        }
    }

    private String ensureUniqueSlug(String baseSlug, String currentProductId) {
        String slug = baseSlug;
        int counter = 1;
        while (true) {
            Optional<Product> existing = productRepository.findBySlug(slug);
            if (existing.isEmpty() || (currentProductId != null && existing.get().getId().equals(currentProductId))) {
                return slug;
            }
            slug = baseSlug + "-" + counter;
            counter++;
        }
    }

    public static String slugify(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}

