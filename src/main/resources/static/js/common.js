/**
 * Vape Deal Spot - Common Vanilla JavaScript Utilities
 */
(function() {
  'use strict';

  // Theme Management
  window.initTheme = function() {
    try {
      var saved = localStorage.getItem('theme');
      var preferred = saved ? saved : (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
      document.documentElement.setAttribute('data-theme', preferred);
      updateThemeButtons(preferred);
    } catch(e) {
      console.warn('Theme storage unavailable', e);
    }
  };

  window.toggleTheme = function() {
    var current = document.documentElement.getAttribute('data-theme') || 'light';
    var next = current === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', next);
    try {
      localStorage.setItem('theme', next);
    } catch(e) {}
    updateThemeButtons(next);
  };

  function updateThemeButtons(theme) {
    var btns = document.querySelectorAll('.theme-toggle-btn');
    btns.forEach(function(btn) {
      btn.setAttribute('aria-label', 'Switch to ' + (theme === 'dark' ? 'light' : 'dark') + ' mode');
      var label = btn.querySelector('.theme-label');
      if (label) {
        label.textContent = theme === 'dark' ? 'Light' : 'Dark';
      }
    });
  }

  // Safe helper to extract CSRF token from server-rendered meta tag
  window.getCsrfToken = function() {
    var meta = document.querySelector('meta[name="_csrf"]');
    return meta ? meta.getAttribute('content') : null;
  };

  // Safe helper to extract CSRF header name (typically X-CSRF-TOKEN)
  window.getCsrfHeaderName = function() {
    var meta = document.querySelector('meta[name="_csrf_header"]');
    return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
  };

  // Reusable CSRF-aware fetch helper for Vanilla JavaScript
  window.csrfFetch = function(url, options) {
    options = options || {};
    var method = (options.method || 'GET').toUpperCase();

    // Check if the request is state-changing (POST, PUT, PATCH, DELETE)
    var isMutating = ['POST', 'PUT', 'PATCH', 'DELETE'].indexOf(method) !== -1;

    // Check same-origin or relative URL to avoid sending token to third parties
    var isSameOrigin = !url.match(/^[a-z]+:\/\//i) || url.indexOf(window.location.origin) === 0;

    if (isMutating && isSameOrigin) {
      var token = window.getCsrfToken();
      var headerName = window.getCsrfHeaderName();

      if (token && headerName) {
        if (typeof Headers !== 'undefined' && options.headers instanceof Headers) {
          if (!options.headers.has(headerName)) {
            options.headers.set(headerName, token);
          }
        } else {
          options.headers = options.headers || {};
          if (!options.headers[headerName]) {
            options.headers[headerName] = token;
          }
        }
      }
    }

    if (options.credentials === undefined) {
      options.credentials = 'same-origin';
    }

    return fetch(url, options);
  };

  // Run initial theme check immediately
  window.initTheme();
})();
