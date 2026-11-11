package com.vapedealspot.product;

import com.vapedealspot.product.dto.ProductFormDto;
import com.vapedealspot.product.dto.ProductListItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/admin/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String listProducts(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        int currentPage = Math.max(0, page);
        int pageSize = (size > 0 && size <= 100) ? size : 10;

        Page<ProductListItemDto> productPage = productService.findProducts(
                search, category, brand, status,
                PageRequest.of(currentPage, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("page", productPage);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());
        model.addAttribute("pageSize", pageSize);

        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("selectedCategory", category != null ? category : "");
        model.addAttribute("selectedBrand", brand != null ? brand : "");
        model.addAttribute("selectedStatus", status != null ? status : "");

        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("brands", productService.getAllBrands());

        model.addAttribute("activeNav", "products");
        model.addAttribute("pageTitle", "Product Management");

        return "admin/products/list";
    }

    @GetMapping("/new")
    public String newProductForm(Model model) {
        ProductFormDto form = new ProductFormDto();
        form.setStatus("active");

        model.addAttribute("productForm", form);
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("brands", productService.getAllBrands());
        model.addAttribute("isEdit", false);
        model.addAttribute("activeNav", "products");
        model.addAttribute("pageTitle", "Add New Product");

        return "admin/products/form";
    }

    @PostMapping("/new")
    public String createProduct(
            @ModelAttribute("productForm") ProductFormDto form,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            Product created = productService.createProduct(form);
            redirectAttributes.addFlashAttribute("successMessage", "Product '" + created.getName() + "' created successfully.");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create product: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("productForm", form);
            model.addAttribute("categories", productService.getAllCategories());
            model.addAttribute("brands", productService.getAllBrands());
            model.addAttribute("isEdit", false);
            model.addAttribute("activeNav", "products");
            model.addAttribute("pageTitle", "Add New Product");
            return "admin/products/form";
        } catch (Exception e) {
            log.error("Unexpected error creating product", e);
            model.addAttribute("errorMessage", "An unexpected error occurred: " + e.getMessage());
            model.addAttribute("productForm", form);
            model.addAttribute("categories", productService.getAllCategories());
            model.addAttribute("brands", productService.getAllBrands());
            model.addAttribute("isEdit", false);
            model.addAttribute("activeNav", "products");
            model.addAttribute("pageTitle", "Add New Product");
            return "admin/products/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editProductForm(@PathVariable("id") String id, Model model, RedirectAttributes redirectAttributes) {
        Optional<ProductFormDto> formOpt = productService.getProductForEdit(id);
        if (formOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Product not found with id: " + id);
            return "redirect:/admin/products";
        }

        model.addAttribute("productForm", formOpt.get());
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("brands", productService.getAllBrands());
        model.addAttribute("isEdit", true);
        model.addAttribute("activeNav", "products");
        model.addAttribute("pageTitle", "Edit Product - " + formOpt.get().getName());

        return "admin/products/form";
    }

    @PostMapping("/{id}/edit")
    public String updateProduct(
            @PathVariable("id") String id,
            @ModelAttribute("productForm") ProductFormDto form,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            Product updated = productService.updateProduct(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Product '" + updated.getName() + "' updated successfully.");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update product {}: {}", id, e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            form.setId(id);
            model.addAttribute("productForm", form);
            model.addAttribute("categories", productService.getAllCategories());
            model.addAttribute("brands", productService.getAllBrands());
            model.addAttribute("isEdit", true);
            model.addAttribute("activeNav", "products");
            model.addAttribute("pageTitle", "Edit Product");
            return "admin/products/form";
        } catch (Exception e) {
            log.error("Unexpected error updating product {}", id, e);
            model.addAttribute("errorMessage", "An unexpected error occurred: " + e.getMessage());
            form.setId(id);
            model.addAttribute("productForm", form);
            model.addAttribute("categories", productService.getAllCategories());
            model.addAttribute("brands", productService.getAllBrands());
            model.addAttribute("isEdit", true);
            model.addAttribute("activeNav", "products");
            model.addAttribute("pageTitle", "Edit Product");
            return "admin/products/form";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            Product toggled = productService.toggleStatus(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product '" + toggled.getName() + "' is now " + toggled.getStatus() + ".");
        } catch (Exception e) {
            log.error("Failed to toggle product status for id {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update product status: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }
}

