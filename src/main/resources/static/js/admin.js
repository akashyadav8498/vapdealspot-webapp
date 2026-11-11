/**
 * Admin Back-Office Vanilla JavaScript
 */
document.addEventListener('DOMContentLoaded', function() {
  'use strict';

  // Flash Alerts Auto-Dismiss after 5s
  var alerts = document.querySelectorAll('.admin-alert');
  alerts.forEach(function(alertBox) {
    setTimeout(function() {
      alertBox.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
      alertBox.style.opacity = '0';
      alertBox.style.transform = 'translateY(-6px)';
      setTimeout(function() { alertBox.remove(); }, 400);
    }, 5000);
  });

  // Modal Backdrop Click to Dismiss
  var backdrops = document.querySelectorAll('.admin-modal-backdrop');
  backdrops.forEach(function(backdrop) {
    backdrop.addEventListener('click', function(e) {
      if (e.target === backdrop) {
        backdrop.classList.remove('open');
      }
    });
  });

  // Escape key to dismiss modals
  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
      backdrops.forEach(function(b) { b.classList.remove('open'); });
    }
  });
});
