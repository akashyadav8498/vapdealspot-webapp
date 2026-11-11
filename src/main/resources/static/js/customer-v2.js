/**
 * Customer V2 Vanilla JavaScript Client Interactions
 */
document.addEventListener('DOMContentLoaded', function() {
  'use strict';

  // Sticky Header Scroll Detection
  var header = document.querySelector('.site-header');
  if (header) {
    var checkScroll = function() {
      if (window.scrollY > 20) {
        header.classList.add('is-scrolled');
      } else {
        header.classList.remove('is-scrolled');
      }
    };
    window.addEventListener('scroll', checkScroll, { passive: true });
    checkScroll();
  }

  // Mobile Menu Toggle
  var burger = document.querySelector('.burger-toggle') || document.querySelector('.site-header .burger');
  var mobileNav = document.querySelector('.mobile-nav-panel') || document.querySelector('.mobile-nav-overlay');
  if (burger && mobileNav) {
    var closeMobileNav = function() {
      burger.setAttribute('aria-expanded', 'false');
      mobileNav.classList.remove('is-open');
      document.body.style.overflow = '';
    };

    burger.addEventListener('click', function(e) {
      e.stopPropagation();
      var expanded = burger.getAttribute('aria-expanded') === 'true';
      burger.setAttribute('aria-expanded', !expanded);
      mobileNav.classList.toggle('is-open', !expanded);
      document.body.style.overflow = !expanded ? 'hidden' : '';
    });

    document.addEventListener('keydown', function(e) {
      if (e.key === 'Escape' && mobileNav.classList.contains('is-open')) {
        closeMobileNav();
      }
    });
  }

  // Copy Coupon Code Interaction
  document.querySelectorAll('.coupon-code').forEach(function(codeEl) {
    codeEl.style.cursor = 'pointer';
    codeEl.title = 'Click to copy code';
    codeEl.addEventListener('click', function() {
      var text = codeEl.innerText.trim();
      if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(function() {
          var original = codeEl.innerText;
          codeEl.innerText = 'Copied!';
          setTimeout(function() { codeEl.innerText = original; }, 1500);
        });
      }
    });
  });
});

// Global Theme Toggle Function
window.toggleTheme = function() {
  var html = document.documentElement;
  var currentTheme = html.getAttribute('data-theme') || 'light';
  var newTheme = currentTheme === 'dark' ? 'light' : 'dark';
  html.setAttribute('data-theme', newTheme);
  try {
    localStorage.setItem('theme', newTheme);
  } catch (e) {}
};

// Global Copy Coupon Code Function
window.copyCouponCode = function(btn) {
  if (!btn) return;
  var code = btn.getAttribute('data-copy');
  if (!code) {
    var sibling = btn.parentElement.querySelector('.coupon-code');
    if (sibling) code = sibling.innerText.trim();
  }
  if (code && navigator.clipboard) {
    navigator.clipboard.writeText(code).then(function() {
      var origText = btn.innerText;
      btn.innerText = 'Copied!';
      setTimeout(function() {
        btn.innerText = origText;
      }, 1500);
    });
  }
};
