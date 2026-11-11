package com.vapedealspot.storefront;

import com.vapedealspot.cms.CmsProductResolver;
import com.vapedealspot.cms.PageBuilderService;
import com.vapedealspot.cms.dto.PageDocumentDto;
import com.vapedealspot.cms.dto.PageSectionDto;
import com.vapedealspot.cms.dto.ProductDisplayDto;
import com.vapedealspot.product.*;
import com.vapedealspot.storefront.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class StorefrontService {

    private static final Logger log = LoggerFactory.getLogger(StorefrontService.class);

    private final PageBuilderService pageBuilderService;
    private final CmsProductResolver cmsProductResolver;
    private final ProductRepository productRepository;
    private final ProductOfferRepository offerRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    public StorefrontService(PageBuilderService pageBuilderService,
                             CmsProductResolver cmsProductResolver,
                             ProductRepository productRepository,
                             ProductOfferRepository offerRepository,
                             ProductVariantRepository variantRepository,
                             CategoryRepository categoryRepository,
                             BrandRepository brandRepository) {
        this.pageBuilderService = pageBuilderService;
        this.cmsProductResolver = cmsProductResolver;
        this.productRepository = productRepository;
        this.offerRepository = offerRepository;
        this.variantRepository = variantRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
    }

    /**
     * Retrieves the published homepage document from MySQL CMS tables.
     * Invariant: Never reads the draft document for anonymous visitors.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPublishedHomepageData() {
        Optional<PageDocumentDto> publishedOpt = pageBuilderService.getPublishedHomepage();
        PageDocumentDto doc = publishedOpt.orElseGet(this::createFallbackHomepageDoc);

        List<PageSectionDto> activeSections = new ArrayList<>();
        if (doc.getSections() != null) {
            for (PageSectionDto s : doc.getSections()) {
                if (s.isEnabled()) {
                    activeSections.add(s);
                }
            }
        }

        List<ProductDisplayDto> topSavingsProducts = Collections.emptyList();
        List<ProductDisplayDto> newArrivalProducts = Collections.emptyList();

        for (PageSectionDto section : activeSections) {
            if ("top_savings".equals(section.getType()) && section.getProps() != null) {
                Object pids = section.getProps().get("productIds");
                if (pids instanceof List<?> pidList) {
                    List<String> ids = pidList.stream().map(String::valueOf).toList();
                    topSavingsProducts = cmsProductResolver.resolve(ids);
                }
            } else if ("new_arrivals".equals(section.getType()) && section.getProps() != null) {
                Object pids = section.getProps().get("productIds");
                if (pids instanceof List<?> pidList) {
                    List<String> ids = pidList.stream().map(String::valueOf).toList();
                    newArrivalProducts = cmsProductResolver.resolve(ids);
                }
            }
        }

        Map<String, Object> model = new HashMap<>();
        model.put("document", doc);
        model.put("sections", activeSections);
        model.put("topSavingsProducts", topSavingsProducts);
        model.put("newArrivalProducts", newArrivalProducts);
        model.put("pageTitle", doc.getSeo() != null && doc.getSeo().getMetaTitle() != null
                ? doc.getSeo().getMetaTitle()
                : "Compare Vape Prices | Vape Deal Spot");
        model.put("metaDescription", doc.getSeo() != null && doc.getSeo().getMetaDescription() != null
                ? doc.getSeo().getMetaDescription()
                : "Search pods, mods, e-liquids and CBD. Compare prices across 25+ vape retailers and never overpay again.");
        model.put("canonicalUrl", doc.getSeo() != null && doc.getSeo().getCanonicalUrl() != null
                ? doc.getSeo().getCanonicalUrl()
                : "/");

        return model;
    }

    /**
     * Search and Filter products catalog with server-side database pagination.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> searchCatalog(String query, String category, String brand,
                                             boolean inStockOnly, String sort, int page, int size) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        String cleanCat = (category != null && !category.trim().isEmpty() && !"all".equalsIgnoreCase(category)) ? category.trim() : null;
        String cleanBrand = (brand != null && !brand.trim().isEmpty() && !"all".equalsIgnoreCase(brand)) ? brand.trim() : null;

        int currentPage = Math.max(0, page);
        int pageSize = (size > 0 && size <= 60) ? size : 20;

        Sort jpaSort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("name".equalsIgnoreCase(sort)) {
            jpaSort = Sort.by(Sort.Direction.ASC, "name");
        }

        Page<Product> productPage = productRepository.findWithFilters(
                cleanQuery, cleanCat, cleanBrand, "active",
                PageRequest.of(currentPage, pageSize, jpaSort)
        );

        List<ProductDisplayDto> displayList = new ArrayList<>();
        for (Product p : productPage.getContent()) {
            ProductDisplayDto dto = mapToDisplayDto(p);
            if (!inStockOnly || dto.isInStock()) {
                displayList.add(dto);
            }
        }

        if ("price-asc".equalsIgnoreCase(sort)) {
            displayList.sort(Comparator.comparing(p -> p.getMinPrice() != null ? p.getMinPrice() : Double.MAX_VALUE));
        } else if ("price-desc".equalsIgnoreCase(sort)) {
            displayList.sort((a, b) -> Double.compare(
                    b.getMinPrice() != null ? b.getMinPrice() : 0.0,
                    a.getMinPrice() != null ? a.getMinPrice() : 0.0));
        }

        List<String> availableBrands = productRepository.findDistinctActiveBrands();

        Map<String, Object> result = new HashMap<>();
        result.put("products", displayList);
        result.put("totalResults", productPage.getTotalElements());
        result.put("currentPage", currentPage);
        result.put("totalPages", productPage.getTotalPages());
        result.put("pageSize", pageSize);
        result.put("availableBrands", availableBrands);
        result.put("query", query != null ? query : "");
        result.put("selectedCategory", category != null ? category : "");
        result.put("selectedBrand", brand != null ? brand : "");
        result.put("inStockOnly", inStockOnly);
        result.put("sort", sort != null ? sort : "default");

        return result;
    }

    /**
     * Resolves single product details with active offers, variants, and price sorting.
     */
    @Transactional(readOnly = true)
    public Optional<ProductDetailDto> getProductDetail(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            return Optional.empty();
        }

        Optional<Product> prodOpt = productRepository.findBySlug(slug.trim());
        if (prodOpt.isEmpty()) {
            return Optional.empty();
        }

        Product product = prodOpt.get();
        if (!product.isActive()) {
            return Optional.empty();
        }

        ProductDetailDto dto = new ProductDetailDto();
        dto.setId(product.getId());
        dto.setSlug(product.getSlug());
        dto.setName(product.getName());
        dto.setBrand(product.getBrand());
        dto.setCategory(product.getCategory());
        dto.setSku(product.getSku());
        dto.setShortDescription(product.getShortDescription());
        dto.setDescription(product.getDescription());
        dto.setImage(product.getImage());
        dto.setRating(product.getRating());

        // Category name lookup
        categoryRepository.findBySlug(product.getCategory())
                .ifPresent(c -> dto.setCategoryName(c.getName()));
        if (dto.getCategoryName() == null) {
            dto.setCategoryName(product.getCategory());
        }

        // Active offers
        List<ProductOffer> offers = offerRepository.findByProductIdAndStatus(product.getId(), "active");
        offers.sort((a, b) -> {
            boolean aStock = "in_stock".equalsIgnoreCase(a.getAvailability());
            boolean bStock = "in_stock".equalsIgnoreCase(b.getAvailability());
            if (aStock && !bStock) return -1;
            if (!aStock && bStock) return 1;
            return a.getPrice().compareTo(b.getPrice());
        });
        dto.setOffers(offers);

        // Active variants
        List<ProductVariant> variants = variantRepository.findByProductIdAndStatus(product.getId(), "active");
        dto.setVariants(variants);

        // Pricing derivation
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        boolean inStock = false;
        ProductOffer bestOffer = null;

        for (ProductOffer o : offers) {
            boolean isAvail = "in_stock".equalsIgnoreCase(o.getAvailability());
            if (isAvail) {
                inStock = true;
                if (bestOffer == null) {
                    bestOffer = o;
                }
            }
            if (o.getPrice() != null) {
                if (minPrice == null || o.getPrice().compareTo(minPrice) < 0) {
                    minPrice = o.getPrice();
                }
                if (maxPrice == null || o.getPrice().compareTo(maxPrice) > 0) {
                    maxPrice = o.getPrice();
                }
            }
            if (o.getOriginalPrice() != null) {
                if (maxPrice == null || o.getOriginalPrice().compareTo(maxPrice) > 0) {
                    maxPrice = o.getOriginalPrice();
                }
            }
        }

        if (bestOffer == null && !offers.isEmpty()) {
            bestOffer = offers.get(0);
        }

        dto.setMinPrice(minPrice);
        dto.setMaxPrice(maxPrice);
        dto.setShopCount(offers.size());
        dto.setInStock(inStock);
        dto.setBestOffer(bestOffer);

        if (minPrice != null && maxPrice != null && maxPrice.compareTo(minPrice) > 0) {
            BigDecimal diff = maxPrice.subtract(minPrice);
            BigDecimal ratio = diff.divide(maxPrice, 4, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("0.15")) >= 0) {
                int pct = ratio.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
                dto.setSavingsBadge("Save " + pct + "%");
            }
        }

        // Breadcrumbs
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(new BreadcrumbItem("Home", "/"));
        breadcrumbs.add(new BreadcrumbItem(dto.getCategoryName(), "/category/" + product.getCategory()));
        breadcrumbs.add(new BreadcrumbItem(product.getName()));
        dto.setBreadcrumbs(breadcrumbs);

        return Optional.of(dto);
    }

    /**
     * Resolves category detail data and associated products.
     */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getCategoryPageData(String slug, String brand,
                                                             boolean inStockOnly, String sort, int page, int size) {
        Optional<Category> catOpt = categoryRepository.findBySlug(slug);
        if (catOpt.isEmpty()) {
            return Optional.empty();
        }

        Category category = catOpt.get();
        Map<String, Object> data = searchCatalog(null, slug, brand, inStockOnly, sort, page, size);
        data.put("category", category);
        data.put("pageTitle", category.getName() + " | Vape Deal Spot");
        data.put("metaDescription", "Compare prices and find the best verified deals on " + category.getName() + ".");

        List<BreadcrumbItem> breadcrumbs = List.of(
                new BreadcrumbItem("Home", "/"),
                new BreadcrumbItem("Categories", "/categories"),
                new BreadcrumbItem(category.getName())
        );
        data.put("breadcrumbs", breadcrumbs);

        return Optional.of(data);
    }

    /**
     * Resolves brand detail data and associated products.
     */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getBrandPageData(String slug, boolean inStockOnly,
                                                          String sort, int page, int size) {
        Optional<Brand> brandOpt = brandRepository.findBySlug(slug);
        if (brandOpt.isEmpty()) {
            return Optional.empty();
        }

        Brand brand = brandOpt.get();
        Map<String, Object> data = searchCatalog(null, null, brand.getName(), inStockOnly, sort, page, size);
        data.put("brand", brand);
        data.put("pageTitle", brand.getName() + " Vapes & Products | Vape Deal Spot");
        data.put("metaDescription", brand.getDescription() != null
                ? brand.getDescription()
                : "Compare prices and deals on all " + brand.getName() + " vape devices and accessories.");

        List<BreadcrumbItem> breadcrumbs = List.of(
                new BreadcrumbItem("Home", "/"),
                new BreadcrumbItem("Brands", "/brands"),
                new BreadcrumbItem(brand.getName())
        );
        data.put("breadcrumbs", breadcrumbs);

        return Optional.of(data);
    }

    /**
     * Resolves all categories with real product counts.
     */
    @Transactional(readOnly = true)
    public List<CategoryItemDto> getAllCategoriesWithCounts() {
        List<Category> categories = categoryRepository.findAllByOrderByDisplayOrderAscNameAsc();
        List<CategoryItemDto> result = new ArrayList<>();
        for (Category c : categories) {
            long count = productRepository.countByCategoryAndStatus(c.getSlug(), "active");
            result.add(new CategoryItemDto(
                    c.getId(),
                    c.getSlug(),
                    c.getName(),
                    c.getIcon(),
                    "/category/" + c.getSlug(),
                    count
            ));
        }
        return result;
    }

    /**
     * Resolves all brands with real product counts.
     */
    @Transactional(readOnly = true)
    public List<BrandItemDto> getAllBrandsWithCounts() {
        List<Brand> brands = brandRepository.findAllByOrderByNameAsc();
        List<BrandItemDto> result = new ArrayList<>();
        for (Brand b : brands) {
            long count = productRepository.countByBrandAndStatus(b.getName(), "active");
            result.add(new BrandItemDto(
                    b.getId(),
                    b.getSlug(),
                    b.getName(),
                    b.getDescription(),
                    b.getLogoUrl(),
                    count
            ));
        }
        return result;
    }

    /**
     * Resolves active verified deals.
     */
    @Transactional(readOnly = true)
    public List<DealItemDto> getActiveDeals() {
        return List.of(
                new DealItemDto("d1", "Element Vape", "20% Off All SMOK Devices", "20% OFF", "SMOK20", true, "Expires in 2 days", "https://example.com/deals/element-smok"),
                new DealItemDto("d2", "VaporDNA", "Buy 2 Get 1 Free E-Liquids", "B2G1 FREE", null, true, "Ongoing Offer", "https://example.com/deals/vapordna-b2g1"),
                new DealItemDto("d3", "DirectVapor", "15% Off Sitewide Hardware", "15% OFF", "DV15", false, "Ongoing Offer", "https://example.com/deals/directvapor-15"),
                new DealItemDto("d4", "EightVape", "$10 Off Orders Over $50", "$10 OFF", "SAVE10", true, "Ends Today", "https://example.com/deals/eightvape-10")
        );
    }

    /**
     * Resolves curated Best-Of lists referencing live database catalog items.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBestOfLists() {
        List<Map<String, Object>> lists = new ArrayList<>();

        // List 1: Best Pod Vapes
        lists.add(Map.of(
                "id", "b1",
                "title", "Best Pod Vapes",
                "description", "Our top picks for compact, flavor-packed pod systems perfect for nic salts.",
                "products", cmsProductResolver.resolve(List.of("p1", "p3", "n1", "p9"))
        ));

        // List 2: Best Vape Kits for Clouds
        lists.add(Map.of(
                "id", "b2",
                "title", "Best Vape Kits for Clouds",
                "description", "High-power devices and sub-ohm tanks designed for massive cloud production.",
                "products", cmsProductResolver.resolve(List.of("p2", "n2", "n3", "p11"))
        ));

        // List 3: Best Fruit E-Liquids
        lists.add(Map.of(
                "id", "b3",
                "title", "Best Fruit E-Liquids",
                "description", "The most refreshing and authentic fruit blends on the market right now.",
                "products", cmsProductResolver.resolve(List.of("p4", "n4", "p12", "p13"))
        ));

        return lists;
    }

    /**
     * Resolves Recent Reads / Blog posts reference content.
     */
    public List<Map<String, String>> getBlogPosts() {
        return List.of(
                Map.of(
                        "id", "rr1",
                        "title", "How to Choose Your First Refillable Pod System",
                        "excerpt", "Pod kits have come a long way. Here is our breakdown of draw resistance, coil life, and maintenance costs.",
                        "date", "June 12, 2026",
                        "path", "/blog"
                ),
                Map.of(
                        "id", "rr2",
                        "title", "Freebase vs. Salt Nicotine: Price and Performance Compared",
                        "excerpt", "Higher strength does not always mean higher cost. We ran the numbers on consumption rates across 500 vapers.",
                        "date", "May 28, 2026",
                        "path", "/blog"
                ),
                Map.of(
                        "id", "rr3",
                        "title", "Battery Safety 101: 18650 and 21700 Best Practices",
                        "excerpt", "External batteries give you flexibility, but proper wraps, chargers, and storage cases are non-negotiable.",
                        "date", "May 14, 2026",
                        "path", "/blog"
                )
        );
    }

    /**
     * Generates XML Sitemap including only verified public customer routes.
     * Excludes: /admin/**, /api/**, /draft/**, /preview/**
     */
    @Transactional(readOnly = true)
    public String generateSitemapXml(String baseUrl) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        List<String> staticRoutes = List.of("/", "/categories", "/brands", "/deals", "/best-of", "/blog");
        for (String route : staticRoutes) {
            xml.append("  <url><loc>").append(baseUrl).append(route).append("</loc><changefreq>daily</changefreq><priority>0.8</priority></url>\n");
        }

        // Active Categories
        for (Category c : categoryRepository.findAll()) {
            xml.append("  <url><loc>").append(baseUrl).append("/category/").append(c.getSlug()).append("</loc><changefreq>weekly</changefreq><priority>0.7</priority></url>\n");
        }

        // Active Brands
        for (Brand b : brandRepository.findAll()) {
            xml.append("  <url><loc>").append(baseUrl).append("/brand/").append(b.getSlug()).append("</loc><changefreq>weekly</changefreq><priority>0.7</priority></url>\n");
        }

        // Active Products
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            if (p.isActive()) {
                xml.append("  <url><loc>").append(baseUrl).append("/product/").append(p.getSlug()).append("</loc><changefreq>daily</changefreq><priority>0.9</priority></url>\n");
            }
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    /**
     * Generates robots.txt.
     */
    public String generateRobotsTxt(String baseUrl) {
        return "User-agent: *\n" +
                "Allow: /\n" +
                "Disallow: /admin/\n" +
                "Disallow: /api/\n" +
                "Disallow: /foundation/\n" +
                "\n" +
                "Sitemap: " + baseUrl + "/sitemap.xml\n";
    }

    private ProductDisplayDto mapToDisplayDto(Product p) {
        List<ProductOffer> offers = offerRepository.findByProductIdAndStatus(p.getId(), "active");

        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        boolean inStock = false;

        for (ProductOffer o : offers) {
            if ("in_stock".equalsIgnoreCase(o.getAvailability())) {
                inStock = true;
            }
            if (o.getPrice() != null) {
                if (minPrice == null || o.getPrice().compareTo(minPrice) < 0) {
                    minPrice = o.getPrice();
                }
                if (maxPrice == null || o.getPrice().compareTo(maxPrice) > 0) {
                    maxPrice = o.getPrice();
                }
            }
            if (o.getOriginalPrice() != null) {
                if (maxPrice == null || o.getOriginalPrice().compareTo(maxPrice) > 0) {
                    maxPrice = o.getOriginalPrice();
                }
            }
        }

        // Fallback to variant prices if no offers exist
        if (minPrice == null) {
            List<ProductVariant> variants = variantRepository.findByProductIdAndStatus(p.getId(), "active");
            for (ProductVariant v : variants) {
                if (v.getPrice() != null && v.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                    if (minPrice == null || v.getPrice().compareTo(minPrice) < 0) {
                        minPrice = v.getPrice();
                    }
                    if (maxPrice == null || v.getPrice().compareTo(maxPrice) > 0) {
                        maxPrice = v.getPrice();
                    }
                }
                if (v.getStock() != null && v.getStock() > 0) {
                    inStock = true;
                }
            }
        }

        String savingsBadge = null;
        if (minPrice != null && maxPrice != null && maxPrice.compareTo(minPrice) > 0) {
            BigDecimal diff = maxPrice.subtract(minPrice);
            BigDecimal ratio = diff.divide(maxPrice, 4, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("0.15")) >= 0) {
                int pct = ratio.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
                savingsBadge = "Save " + pct + "%";
            }
        }

        ProductDisplayDto dto = new ProductDisplayDto(
                p.getId(),
                p.getSlug(),
                p.getBrand(),
                p.getName(),
                p.getImage(),
                minPrice != null ? minPrice.doubleValue() : null,
                maxPrice != null ? maxPrice.doubleValue() : null,
                offers.size(),
                savingsBadge
        );
        dto.setInStock(inStock);
        return dto;
    }

    private PageDocumentDto createFallbackHomepageDoc() {
        PageDocumentDto doc = new PageDocumentDto();
        doc.setId("homepage");
        doc.setSlug("/");
        doc.setTitle("Home");
        doc.setVersion(1);
        doc.setSections(Collections.emptyList());
        return doc;
    }
}

