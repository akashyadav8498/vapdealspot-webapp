package com.vapedealspot.storefront;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class StorefrontController {

    private final StorefrontService storefrontService;

    public StorefrontController(StorefrontService storefrontService) {
        this.storefrontService = storefrontService;
    }

    /**
     * Public Homepage: GET /
     * Strictly reads ONLY published CMS document. Never exposes draft content to anonymous visitors.
     */
    @GetMapping("/")
    public String showHomepage(Model model) {
        Map<String, Object> data = storefrontService.getPublishedHomepageData();
        model.addAllAttributes(data);
        model.addAttribute("activeNav", "home");
        return "storefront/home";
    }

    /**
     * Deals & Coupons page: GET /deals
     */
    @GetMapping("/deals")
    public String showDeals(Model model) {
        model.addAttribute("pageTitle", "Vape Deals & Coupons | Vape Deal Spot");
        model.addAttribute("metaDescription", "Discover the latest verified discounts and promo codes from top vape retailers.");
        model.addAttribute("deals", storefrontService.getActiveDeals());
        model.addAttribute("activeNav", "deals");
        return "storefront/deals";
    }

    /**
     * Best Of curated lists: GET /best-of
     */
    @GetMapping("/best-of")
    public String showBestOf(Model model) {
        model.addAttribute("pageTitle", "The Best Vapes of 2026 | Vape Deal Spot");
        model.addAttribute("metaDescription", "Curated vape picks across devices, e-liquids, and more. Compare prices across trusted merchants.");
        model.addAttribute("curatedLists", storefrontService.getBestOfLists());
        model.addAttribute("activeNav", "best-of");
        return "storefront/best-of";
    }

    /**
     * Blog / Recent Reads page: GET /blog
     */
    @GetMapping("/blog")
    public String showBlog(Model model) {
        model.addAttribute("pageTitle", "Vape Deal Spot Blog & News | Vape Deal Spot");
        model.addAttribute("metaDescription", "Read the latest news, hardware comparisons, and battery safety guides on Vape Deal Spot.");
        model.addAttribute("posts", storefrontService.getBlogPosts());
        model.addAttribute("activeNav", "blog");
        return "storefront/blog";
    }

    /**
     * robots.txt endpoint
     */
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String robotsTxt(HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        return storefrontService.generateRobotsTxt(baseUrl);
    }

    /**
     * sitemap.xml endpoint
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemapXml(HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        return storefrontService.generateSitemapXml(baseUrl);
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        if ((scheme.equals("http") && serverPort == 80) || (scheme.equals("https") && serverPort == 443)) {
            return scheme + "://" + serverName;
        }
        return scheme + "://" + serverName + ":" + serverPort;
    }
}

