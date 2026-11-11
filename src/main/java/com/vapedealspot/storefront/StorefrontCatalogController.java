package com.vapedealspot.storefront;

import com.vapedealspot.storefront.dto.ProductDetailDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@Controller
public class StorefrontCatalogController {

    private final StorefrontService storefrontService;

    public StorefrontCatalogController(StorefrontService storefrontService) {
        this.storefrontService = storefrontService;
    }

    /**
     * Search and Filter catalog: GET /search
     */
    @GetMapping("/search")
    public String search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "instock", defaultValue = "false") boolean inStockOnly,
            @RequestParam(name = "sort", defaultValue = "default") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model) {

        Map<String, Object> data = storefrontService.searchCatalog(query, category, brand, inStockOnly, sort, page, size);
        model.addAllAttributes(data);

        String title = (query != null && !query.trim().isEmpty())
                ? "Search Results for \"" + query.trim() + "\" | Vape Deal Spot"
                : "All Vape Products | Vape Deal Spot";
        model.addAttribute("pageTitle", title);
        model.addAttribute("metaDescription", "Search and compare vape prices across 25+ trusted vape stores.");
        model.addAttribute("activeNav", "search");

        return "storefront/search";
    }

    /**
     * Categories Index: GET /categories
     */
    @GetMapping("/categories")
    public String showCategories(Model model) {
        model.addAttribute("pageTitle", "All Vape Categories | Vape Deal Spot");
        model.addAttribute("metaDescription", "Browse all vape categories including pod vapes, box mods, e-liquids, CBD, and tanks.");
        model.addAttribute("categories", storefrontService.getAllCategoriesWithCounts());
        model.addAttribute("activeNav", "categories");
        return "storefront/categories";
    }

    /**
     * Category Detail / Products in Category: GET /category/{slug}
     */
    @GetMapping("/category/{slug}")
    public String showCategoryDetail(
            @PathVariable("slug") String slug,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "instock", defaultValue = "false") boolean inStockOnly,
            @RequestParam(name = "sort", defaultValue = "default") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model) {

        Optional<Map<String, Object>> dataOpt = storefrontService.getCategoryPageData(slug, brand, inStockOnly, sort, page, size);
        if (dataOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: " + slug);
        }

        model.addAllAttributes(dataOpt.get());
        model.addAttribute("activeNav", "categories");
        return "storefront/category-detail";
    }

    /**
     * Brands Directory: GET /brands
     */
    @GetMapping("/brands")
    public String showBrands(Model model) {
        model.addAttribute("pageTitle", "Vape Brands Directory | Vape Deal Spot");
        model.addAttribute("metaDescription", "Discover the top vape hardware and e-liquid brands on the market. Compare prices across top retailers.");
        model.addAttribute("brands", storefrontService.getAllBrandsWithCounts());
        model.addAttribute("activeNav", "brands");
        return "storefront/brands";
    }

    /**
     * Brand Detail / Products by Brand: GET /brand/{slug}
     */
    @GetMapping("/brand/{slug}")
    public String showBrandDetail(
            @PathVariable("slug") String slug,
            @RequestParam(name = "instock", defaultValue = "false") boolean inStockOnly,
            @RequestParam(name = "sort", defaultValue = "default") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model) {

        Optional<Map<String, Object>> dataOpt = storefrontService.getBrandPageData(slug, inStockOnly, sort, page, size);
        if (dataOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found: " + slug);
        }

        model.addAllAttributes(dataOpt.get());
        model.addAttribute("activeNav", "brands");
        return "storefront/brand-detail";
    }

    /**
     * Product Detail: GET /product/{slug}
     */
    @GetMapping("/product/{slug}")
    public String showProductDetail(@PathVariable("slug") String slug, Model model) {
        Optional<ProductDetailDto> prodOpt = storefrontService.getProductDetail(slug);
        if (prodOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + slug);
        }

        ProductDetailDto prod = prodOpt.get();
        model.addAttribute("product", prod);
        model.addAttribute("pageTitle", prod.getBrand() + " " + prod.getName() + " | Vape Deal Spot");
        model.addAttribute("metaDescription", prod.getDescription() != null && !prod.getDescription().isEmpty()
                ? prod.getDescription()
                : "Compare prices and deals across verified vape shops for " + prod.getName() + ".");
        model.addAttribute("activeNav", "products");

        return "storefront/product-detail";
    }
}

