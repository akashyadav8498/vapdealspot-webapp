/**
 * Vape Deal Spot — Page Builder Vanilla JavaScript Client
 * Milestone 18 — Schema-Driven Homepage Page Builder
 */
(function() {
  'use strict';

  // State
  let originalDoc = null;
  let currentDoc = null;
  let selectedId = 'seo'; // 'seo' or section ID
  let isSaving = false;
  let isPublishing = false;
  let isRestoring = false;

  const SECTION_METADATA = {
    hero: { label: 'Hero Banner', icon: '✨' },
    categories: { label: 'Shop by Category', icon: '🏷️' },
    top_savings: { label: 'Top Savings', icon: '💰' },
    new_arrivals: { label: 'New Arrivals', icon: '⚡' },
    coupons: { label: 'Coupons & Deals', icon: '🎟️' },
    editorial: { label: 'Editorial Promos', icon: '📰' },
    recent_reads: { label: 'Recent Reads', icon: '📖' }
  };

  // Helper: Deep Clone
  function deepClone(obj) {
    return JSON.parse(JSON.stringify(obj));
  }

  // Helper: Check Dirty State
  function isDirty() {
    if (!originalDoc || !currentDoc) return false;
    return JSON.stringify(originalDoc) !== JSON.stringify(currentDoc);
  }

  // Helper: CSRF fetch
  function getCsrfInfo() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    return {
      token: tokenMeta ? tokenMeta.getAttribute('content') : '',
      header: headerMeta ? headerMeta.getAttribute('content') : 'X-CSRF-TOKEN'
    };
  }

  async function pbFetch(url, options = {}) {
    const { token, header } = getCsrfInfo();
    const headers = options.headers || {};
    if (token && header && !options.noCsrf) {
      headers[header] = token;
    }
    headers['Accept'] = 'application/json';
    if (options.body && typeof options.body === 'string') {
      headers['Content-Type'] = 'application/json';
    }
    return fetch(url, { ...options, headers });
  }

  // Update Status Badges
  function updateBadges() {
    const versionBadge = document.getElementById('pbVersionBadge');
    const dirtyBadge = document.getElementById('pbDirtyBadge');
    const saveBtn = document.getElementById('pbSaveBtn');
    const publishBtn = document.getElementById('pbPublishBtn');

    if (versionBadge && currentDoc) {
      versionBadge.textContent = 'Draft v' + (currentDoc.version || 1);
    }

    const dirty = isDirty();
    if (dirtyBadge) {
      if (dirty) {
        dirtyBadge.className = 'pb-badge pb-badge-dirty';
        dirtyBadge.textContent = '● Unsaved Changes';
      } else {
        dirtyBadge.className = 'pb-badge pb-badge-clean';
        dirtyBadge.textContent = '✓ Saved';
      }
    }

    if (saveBtn) {
      saveBtn.disabled = isSaving || !window.__PB_CAN_EDIT__;
    }

    if (publishBtn && window.__PB_CAN_PUBLISH__) {
      publishBtn.disabled = isPublishing || dirty;
      if (dirty) {
        publishBtn.title = 'Save draft before publishing';
      } else {
        publishBtn.title = 'Publish current draft to live homepage';
      }
    }
  }

  // Render Section List (Sidebar)
  function renderSectionList() {
    const listContainer = document.getElementById('pbSidebarList');
    if (!listContainer || !currentDoc) return;

    listContainer.innerHTML = '';

    // 1. SEO item
    const seoItem = document.createElement('div');
    seoItem.className = 'pb-list-item' + (selectedId === 'seo' ? ' active' : '');
    seoItem.innerHTML = `
      <div class="pb-item-main">
        <span class="pb-item-icon">🔍</span>
        <div class="pb-item-info">
          <span class="pb-item-label">SEO & Metadata</span>
          <span class="pb-item-id">Page title, meta tags, URL</span>
        </div>
      </div>
    `;
    seoItem.addEventListener('click', () => {
      selectedId = 'seo';
      renderSectionList();
      renderInspector();
    });
    listContainer.appendChild(seoItem);

    // Separator
    const sep = document.createElement('div');
    sep.style.cssText = 'height: 1px; background: #e2e8f0; margin: 8px 0;';
    listContainer.appendChild(sep);

    // Section count header
    const secHeader = document.createElement('div');
    secHeader.style.cssText = 'padding: 4px 8px; font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em;';
    secHeader.textContent = 'Homepage Sections';
    listContainer.appendChild(secHeader);

    // Sort sections by order
    const sortedSections = [...currentDoc.sections].sort((a, b) => a.order - b.order);

    sortedSections.forEach((sec, idx) => {
      const meta = SECTION_METADATA[sec.type] || { label: sec.type, icon: '📦' };
      const item = document.createElement('div');
      item.className = 'pb-list-item' + (selectedId === sec.id ? ' active' : '');
      if (!sec.enabled) {
        item.style.opacity = '0.65';
      }

      item.innerHTML = `
        <div class="pb-item-main">
          <span class="pb-item-order">${idx + 1}</span>
          <span class="pb-item-icon">${meta.icon}</span>
          <div class="pb-item-info">
            <span class="pb-item-label">
              ${escapeHtml(meta.label)}
              ${!sec.enabled ? '<span class="pb-hidden-pill">Hidden</span>' : ''}
            </span>
            <span class="pb-item-id">${escapeHtml(sec.id)}</span>
          </div>
        </div>
        <div class="pb-item-controls">
          <button type="button" class="pb-control-btn pb-toggle-btn" title="${sec.enabled ? 'Hide section' : 'Show section'}">
            ${sec.enabled ? '👁️' : '🚫'}
          </button>
          <button type="button" class="pb-control-btn pb-up-btn" ${idx === 0 || !window.__PB_CAN_EDIT__ ? 'disabled' : ''} title="Move up">
            ▲
          </button>
          <button type="button" class="pb-control-btn pb-down-btn" ${idx === sortedSections.length - 1 || !window.__PB_CAN_EDIT__ ? 'disabled' : ''} title="Move down">
            ▼
          </button>
        </div>
      `;

      // Select item on click
      item.addEventListener('click', (e) => {
        if (e.target.closest('.pb-control-btn')) return;
        selectedId = sec.id;
        renderSectionList();
        renderInspector();
      });

      // Controls
      const toggleBtn = item.querySelector('.pb-toggle-btn');
      if (toggleBtn) {
        toggleBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          if (!window.__PB_CAN_EDIT__) return;
          sec.enabled = !sec.enabled;
          renderSectionList();
          renderInspector();
          updateBadges();
        });
      }

      const upBtn = item.querySelector('.pb-up-btn');
      if (upBtn) {
        upBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          if (!window.__PB_CAN_EDIT__ || idx === 0) return;
          moveSection(idx, -1);
        });
      }

      const downBtn = item.querySelector('.pb-down-btn');
      if (downBtn) {
        downBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          if (!window.__PB_CAN_EDIT__ || idx === sortedSections.length - 1) return;
          moveSection(idx, 1);
        });
      }

      listContainer.appendChild(item);
    });
  }

  // Move Section Up or Down
  function moveSection(index, delta) {
    const sorted = [...currentDoc.sections].sort((a, b) => a.order - b.order);
    const targetIndex = index + delta;
    if (targetIndex < 0 || targetIndex >= sorted.length) return;

    const [moved] = sorted.splice(index, 1);
    sorted.splice(targetIndex, 0, moved);

    // Re-assign order sequence (0, 1, 2, ...)
    sorted.forEach((s, i) => { s.order = i; });
    currentDoc.sections = sorted;

    renderSectionList();
    updateBadges();
  }

  // Render Inspector Panel
  function renderInspector() {
    const container = document.getElementById('pbInspectorContainer');
    if (!container || !currentDoc) return;

    container.innerHTML = '';

    if (selectedId === 'seo') {
      renderSeoInspector(container);
    } else {
      const section = currentDoc.sections.find(s => s.id === selectedId);
      if (section) {
        renderSectionInspector(container, section);
      } else {
        container.innerHTML = `<div class="pb-inspector-card"><p>Select a section from the left panel to inspect its settings.</p></div>`;
      }
    }
  }

  // SEO Inspector
  function renderSeoInspector(container) {
    const seo = currentDoc.seo || { metaTitle: '', metaDescription: '', canonicalUrl: '/' };
    const readOnlyAttr = !window.__PB_CAN_EDIT__ ? 'disabled' : '';

    const card = document.createElement('div');
    card.className = 'pb-inspector-card';
    card.innerHTML = `
      <div class="pb-inspector-header">
        <div>
          <h2>SEO & Metadata</h2>
          <p>Search engine optimization and browser metadata for the homepage</p>
        </div>
      </div>

      <div class="pb-form-group">
        <label class="pb-form-label">
          <span>Meta Title</span>
          <span class="pb-char-count" id="pbSeoTitleCount">${(seo.metaTitle || '').length} / 70</span>
        </label>
        <input type="text" class="pb-input" id="pbMetaTitle" value="${escapeHtml(seo.metaTitle || '')}" maxlength="70" ${readOnlyAttr}>
        <div class="pb-help-text">Appears in browser tabs and search engine search result headlines (max 70 chars).</div>
      </div>

      <div class="pb-form-group">
        <label class="pb-form-label">
          <span>Meta Description</span>
          <span class="pb-char-count" id="pbSeoDescCount">${(seo.metaDescription || '').length} / 160</span>
        </label>
        <textarea class="pb-textarea" id="pbMetaDescription" maxlength="160" ${readOnlyAttr}>${escapeHtml(seo.metaDescription || '')}</textarea>
        <div class="pb-help-text">Summary displayed in search snippets beneath the title (max 160 chars).</div>
      </div>

      <div class="pb-form-group">
        <label class="pb-form-label">
          <span>Canonical URL</span>
        </label>
        <input type="text" class="pb-input" id="pbCanonicalUrl" value="${escapeHtml(seo.canonicalUrl || '/')}" maxlength="1024" ${readOnlyAttr}>
        <div class="pb-help-text">Root-relative or absolute canonical URL (e.g. / or https://vapedealspot.com/).</div>
      </div>
    `;

    // Events
    const titleInput = card.querySelector('#pbMetaTitle');
    const titleCount = card.querySelector('#pbSeoTitleCount');
    titleInput.addEventListener('input', (e) => {
      seo.metaTitle = e.target.value;
      if (titleCount) titleCount.textContent = `${e.target.value.length} / 70`;
      updateBadges();
    });

    const descInput = card.querySelector('#pbMetaDescription');
    const descCount = card.querySelector('#pbSeoDescCount');
    descInput.addEventListener('input', (e) => {
      seo.metaDescription = e.target.value;
      if (descCount) descCount.textContent = `${e.target.value.length} / 160`;
      updateBadges();
    });

    const urlInput = card.querySelector('#pbCanonicalUrl');
    urlInput.addEventListener('input', (e) => {
      seo.canonicalUrl = e.target.value;
      updateBadges();
    });

    container.appendChild(card);
  }

  // Section Inspector Router
  function renderSectionInspector(container, section) {
    const meta = SECTION_METADATA[section.type] || { label: section.type, icon: '📦' };
    const readOnlyAttr = !window.__PB_CAN_EDIT__ ? 'disabled' : '';

    const card = document.createElement('div');
    card.className = 'pb-inspector-card';

    // Header with enable toggle
    card.innerHTML = `
      <div class="pb-inspector-header">
        <div>
          <h2>${meta.icon} ${escapeHtml(meta.label)}</h2>
          <p>Section ID: <code style="font-family: monospace; font-size: 11px;">${escapeHtml(section.id)}</code> &bull; Type: <code>${section.type}</code></p>
        </div>
        <div style="display: flex; align-items: center; gap: 8px;">
          <label style="font-size: 13px; font-weight: 600; color: #334155; cursor: pointer;">
            <input type="checkbox" id="pbSectionEnabled" ${section.enabled ? 'checked' : ''} ${readOnlyAttr}> Visible
          </label>
        </div>
      </div>
      <div id="pbSectionPropsForm"></div>
    `;

    const enabledCheck = card.querySelector('#pbSectionEnabled');
    enabledCheck.addEventListener('change', (e) => {
      section.enabled = e.target.checked;
      renderSectionList();
      updateBadges();
    });

    const propsForm = card.querySelector('#pbSectionPropsForm');
    const props = section.props || {};

    switch (section.type) {
      case 'hero':
        renderHeroFields(propsForm, props, readOnlyAttr);
        break;
      case 'categories':
        renderCategoriesFields(propsForm, props, readOnlyAttr);
        break;
      case 'top_savings':
      case 'new_arrivals':
        renderProductGridFields(propsForm, props, readOnlyAttr);
        break;
      case 'coupons':
        renderCouponsFields(propsForm, props, readOnlyAttr);
        break;
      case 'editorial':
        renderEditorialFields(propsForm, props, readOnlyAttr);
        break;
      case 'recent_reads':
        renderRecentReadsFields(propsForm, props, readOnlyAttr);
        break;
      default:
        propsForm.innerHTML = `<p>Custom properties for section type "${section.type}".</p>`;
    }

    container.appendChild(card);
  }

  // Hero Fields
  function renderHeroFields(container, props, readOnlyAttr) {
    container.innerHTML = `
      <div style="display: grid; grid-template-columns: 80px 1fr; gap: 12px;" class="pb-form-group">
        <div>
          <label class="pb-form-label">Pill Icon</label>
          <input type="text" class="pb-input" id="pbHeroPillIcon" value="${escapeHtml(props.pillIcon || '')}" maxlength="10" ${readOnlyAttr}>
        </div>
        <div>
          <label class="pb-form-label">Pill Text</label>
          <input type="text" class="pb-input" id="pbHeroPillText" value="${escapeHtml(props.pillText || '')}" maxlength="100" ${readOnlyAttr}>
        </div>
      </div>

      <div class="pb-form-group">
        <label class="pb-form-label">Title (Primary Headline)</label>
        <input type="text" class="pb-input" id="pbHeroTitlePrimary" value="${escapeHtml(props.titlePrimary || '')}" maxlength="150" ${readOnlyAttr}>
      </div>

      <div class="pb-form-group">
        <label class="pb-form-label">Title (Highlight Headline)</label>
        <input type="text" class="pb-input" id="pbHeroTitleHighlight" value="${escapeHtml(props.titleHighlight || '')}" maxlength="150" ${readOnlyAttr}>
      </div>

      <div class="pb-form-group">
        <label class="pb-form-label">Lede Description</label>
        <textarea class="pb-textarea" id="pbHeroLede" maxlength="500" ${readOnlyAttr}>${escapeHtml(props.ledeText || '')}</textarea>
      </div>

      <div class="pb-form-group">
        <label style="font-size: 13px; font-weight: 600; color: #334155; cursor: pointer;">
          <input type="checkbox" id="pbHeroShowStats" ${props.showStats ? 'checked' : ''} ${readOnlyAttr}> Show Statistics Strip
        </label>
      </div>

      <div class="pb-form-group">
        <label class="pb-form-label">Statistics Items (Max 6)</label>
        <div id="pbHeroStatsList"></div>
      </div>
    `;

    bindInput(container, '#pbHeroPillIcon', val => props.pillIcon = val);
    bindInput(container, '#pbHeroPillText', val => props.pillText = val);
    bindInput(container, '#pbHeroTitlePrimary', val => props.titlePrimary = val);
    bindInput(container, '#pbHeroTitleHighlight', val => props.titleHighlight = val);
    bindInput(container, '#pbHeroLede', val => props.ledeText = val);

    const showStatsCheck = container.querySelector('#pbHeroShowStats');
    showStatsCheck.addEventListener('change', e => {
      props.showStats = e.target.checked;
      updateBadges();
    });

    const statsList = container.querySelector('#pbHeroStatsList');
    if (!Array.isArray(props.stats)) props.stats = [];
    props.stats.forEach((st, idx) => {
      const row = document.createElement('div');
      row.style.cssText = 'display: grid; grid-template-columns: 60px 1fr; gap: 8px; margin-bottom: 8px;';
      row.innerHTML = `
        <input type="text" class="pb-input" value="${escapeHtml(st.icon || '')}" maxlength="10" ${readOnlyAttr}>
        <input type="text" class="pb-input" value="${escapeHtml(st.text || '')}" maxlength="100" ${readOnlyAttr}>
      `;
      const inputs = row.querySelectorAll('input');
      inputs[0].addEventListener('input', e => { st.icon = e.target.value; updateBadges(); });
      inputs[1].addEventListener('input', e => { st.text = e.target.value; updateBadges(); });
      statsList.appendChild(row);
    });
  }

  // Categories Fields
  function renderCategoriesFields(container, props, readOnlyAttr) {
    container.innerHTML = `
      <div style="display: grid; grid-template-columns: 1fr 2fr; gap: 12px;" class="pb-form-group">
        <div>
          <label class="pb-form-label">Eyebrow</label>
          <input type="text" class="pb-input" id="pbCatEyebrow" value="${escapeHtml(props.eyebrow || '')}" maxlength="60" ${readOnlyAttr}>
        </div>
        <div>
          <label class="pb-form-label">Title</label>
          <input type="text" class="pb-input" id="pbCatTitle" value="${escapeHtml(props.title || '')}" maxlength="120" ${readOnlyAttr}>
        </div>
      </div>

      <div class="pb-form-group">
        <label class="pb-form-label">Categories List</label>
        <div id="pbCategoriesItems"></div>
      </div>
    `;

    bindInput(container, '#pbCatEyebrow', val => props.eyebrow = val);
    bindInput(container, '#pbCatTitle', val => props.title = val);

    const itemsContainer = container.querySelector('#pbCategoriesItems');
    if (!Array.isArray(props.categories)) props.categories = [];
    props.categories.forEach((cat, idx) => {
      const card = document.createElement('div');
      card.className = 'pb-subitem-card';
      card.innerHTML = `
        <div class="pb-subitem-header">
          <span>Category #${idx + 1} &bull; ${escapeHtml(cat.id || '')}</span>
        </div>
        <div style="display: grid; grid-template-columns: 50px 1fr 80px; gap: 8px; margin-bottom: 8px;">
          <input type="text" class="pb-input" placeholder="Icon" value="${escapeHtml(cat.icon || '')}" maxlength="10" ${readOnlyAttr}>
          <input type="text" class="pb-input" placeholder="Label" value="${escapeHtml(cat.label || '')}" maxlength="100" ${readOnlyAttr}>
          <input type="number" class="pb-input" placeholder="Count" value="${cat.count || 0}" min="0" ${readOnlyAttr}>
        </div>
        <input type="text" class="pb-input" placeholder="Target Path" value="${escapeHtml(cat.path || '')}" maxlength="1024" ${readOnlyAttr}>
      `;
      const inputs = card.querySelectorAll('input');
      inputs[0].addEventListener('input', e => { cat.icon = e.target.value; updateBadges(); });
      inputs[1].addEventListener('input', e => { cat.label = e.target.value; updateBadges(); });
      inputs[2].addEventListener('input', e => { cat.count = parseInt(e.target.value, 10) || 0; updateBadges(); });
      inputs[3].addEventListener('input', e => { cat.path = e.target.value; updateBadges(); });
      itemsContainer.appendChild(card);
    });
  }

  // Product Grid Fields (Top Savings & New Arrivals)
  function renderProductGridFields(container, props, readOnlyAttr) {
    const pids = Array.isArray(props.productIds) ? props.productIds.join(', ') : '';
    container.innerHTML = `
      <div style="display: grid; grid-template-columns: 1fr 2fr; gap: 12px;" class="pb-form-group">
        <div>
          <label class="pb-form-label">Eyebrow</label>
          <input type="text" class="pb-input" id="pbGridEyebrow" value="${escapeHtml(props.eyebrow || '')}" maxlength="60" ${readOnlyAttr}>
        </div>
        <div>
          <label class="pb-form-label">Title</label>
          <input type="text" class="pb-input" id="pbGridTitle" value="${escapeHtml(props.title || '')}" maxlength="120" ${readOnlyAttr}>
        </div>
      </div>

      <div class="pb-form-group">
        <label class="pb-form-label">Canonical Product IDs (comma-separated)</label>
        <input type="text" class="pb-input" id="pbGridProductIds" value="${escapeHtml(pids)}" ${readOnlyAttr}>
        <div class="pb-help-text">Product IDs configured for this section (e.g. p1, p2, p3, p4 or n1, n2, n3, n4). Resolves against authoritative catalog.</div>
      </div>
    `;

    bindInput(container, '#pbGridEyebrow', val => props.eyebrow = val);
    bindInput(container, '#pbGridTitle', val => props.title = val);

    const pidsInput = container.querySelector('#pbGridProductIds');
    pidsInput.addEventListener('input', e => {
      const raw = e.target.value;
      props.productIds = raw.split(',').map(s => s.trim()).filter(Boolean);
      updateBadges();
    });
  }

  // Coupons Fields
  function renderCouponsFields(container, props, readOnlyAttr) {
    container.innerHTML = `
      <div style="display: grid; grid-template-columns: 80px 1fr 2fr; gap: 12px;" class="pb-form-group">
        <div>
          <label class="pb-form-label">Icon</label>
          <input type="text" class="pb-input" id="pbCpEyebrowIcon" value="${escapeHtml(props.eyebrowIcon || '')}" maxlength="10" ${readOnlyAttr}>
        </div>
        <div>
          <label class="pb-form-label">Eyebrow</label>
          <input type="text" class="pb-input" id="pbCpEyebrowText" value="${escapeHtml(props.eyebrowText || '')}" maxlength="60" ${readOnlyAttr}>
        </div>
        <div>
          <label class="pb-form-label">Title</label>
          <input type="text" class="pb-input" id="pbCpTitle" value="${escapeHtml(props.title || '')}" maxlength="120" ${readOnlyAttr}>
        </div>
      </div>

      <div class="pb-form-group">
        <label class="pb-form-label">Coupons List</label>
        <div id="pbCouponsItems"></div>
      </div>
    `;

    bindInput(container, '#pbCpEyebrowIcon', val => props.eyebrowIcon = val);
    bindInput(container, '#pbCpEyebrowText', val => props.eyebrowText = val);
    bindInput(container, '#pbCpTitle', val => props.title = val);

    const itemsContainer = container.querySelector('#pbCouponsItems');
    if (!Array.isArray(props.coupons)) props.coupons = [];
    props.coupons.forEach((cp, idx) => {
      const card = document.createElement('div');
      card.className = 'pb-subitem-card';
      card.innerHTML = `
        <div class="pb-subitem-header">
          <span>Coupon #${idx + 1} &bull; ${escapeHtml(cp.id || '')}</span>
          <label style="cursor: pointer;"><input type="checkbox" ${cp.verified ? 'checked' : ''} ${readOnlyAttr}> Verified</label>
        </div>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 8px;">
          <input type="text" class="pb-input" placeholder="Store name" value="${escapeHtml(cp.store || '')}" maxlength="100" ${readOnlyAttr}>
          <input type="text" class="pb-input" placeholder="Coupon Code" value="${escapeHtml(cp.code || '')}" maxlength="50" ${readOnlyAttr}>
        </div>
        <input type="text" class="pb-input" placeholder="Offer description" value="${escapeHtml(cp.offer || '')}" maxlength="150" style="margin-bottom: 8px;" ${readOnlyAttr}>
        <input type="text" class="pb-input" placeholder="Affiliate / Destination URL" value="${escapeHtml(cp.affiliateUrl || '')}" maxlength="1024" ${readOnlyAttr}>
      `;
      const inputs = card.querySelectorAll('input');
      const verifiedCheck = inputs[0];
      verifiedCheck.addEventListener('change', e => { cp.verified = e.target.checked; updateBadges(); });
      inputs[1].addEventListener('input', e => { cp.store = e.target.value; updateBadges(); });
      inputs[2].addEventListener('input', e => { cp.code = e.target.value; updateBadges(); });
      inputs[3].addEventListener('input', e => { cp.offer = e.target.value; updateBadges(); });
      inputs[4].addEventListener('input', e => { cp.affiliateUrl = e.target.value; updateBadges(); });
      itemsContainer.appendChild(card);
    });
  }

  // Editorial Fields
  function renderEditorialFields(container, props, readOnlyAttr) {
    container.innerHTML = `
      <div class="pb-form-group">
        <label class="pb-form-label">Editorial Promo Cards</label>
        <div id="pbEditorialItems"></div>
      </div>
    `;

    const itemsContainer = container.querySelector('#pbEditorialItems');
    if (!Array.isArray(props.promos)) props.promos = [];
    props.promos.forEach((pr, idx) => {
      const card = document.createElement('div');
      card.className = 'pb-subitem-card';
      card.innerHTML = `
        <div class="pb-subitem-header">
          <span>Promo #${idx + 1} &bull; ${escapeHtml(pr.id || '')}</span>
        </div>
        <input type="text" class="pb-input" placeholder="Promo Title" value="${escapeHtml(pr.title || '')}" maxlength="150" style="margin-bottom: 8px;" ${readOnlyAttr}>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 8px;">
          <input type="text" class="pb-input" placeholder="Image URL (/demo-assets/...)" value="${escapeHtml(pr.image || '')}" maxlength="1024" ${readOnlyAttr}>
          <input type="text" class="pb-input" placeholder="Target Path (/deals...)" value="${escapeHtml(pr.path || '')}" maxlength="1024" ${readOnlyAttr}>
        </div>
        <input type="text" class="pb-input" placeholder="CTA Button Text (Shop now)" value="${escapeHtml(pr.ctaText || '')}" maxlength="50" ${readOnlyAttr}>
      `;
      const inputs = card.querySelectorAll('input');
      inputs[0].addEventListener('input', e => { pr.title = e.target.value; updateBadges(); });
      inputs[1].addEventListener('input', e => { pr.image = e.target.value; updateBadges(); });
      inputs[2].addEventListener('input', e => { pr.path = e.target.value; updateBadges(); });
      inputs[3].addEventListener('input', e => { pr.ctaText = e.target.value; updateBadges(); });
      itemsContainer.appendChild(card);
    });
  }

  // Recent Reads Fields
  function renderRecentReadsFields(container, props, readOnlyAttr) {
    container.innerHTML = `
      <div style="display: grid; grid-template-columns: 80px 1fr 2fr; gap: 12px;" class="pb-form-group">
        <div>
          <label class="pb-form-label">Icon</label>
          <input type="text" class="pb-input" id="pbRrEyebrowIcon" value="${escapeHtml(props.eyebrowIcon || '')}" maxlength="10" ${readOnlyAttr}>
        </div>
        <div>
          <label class="pb-form-label">Eyebrow</label>
          <input type="text" class="pb-input" id="pbRrEyebrowText" value="${escapeHtml(props.eyebrowText || '')}" maxlength="60" ${readOnlyAttr}>
        </div>
        <div>
          <label class="pb-form-label">Title</label>
          <input type="text" class="pb-input" id="pbRrTitle" value="${escapeHtml(props.title || '')}" maxlength="120" ${readOnlyAttr}>
        </div>
      </div>

      <div class="pb-form-group">
        <label class="pb-form-label">Recent Blog Reads</label>
        <div id="pbRecentReadsItems"></div>
      </div>
    `;

    bindInput(container, '#pbRrEyebrowIcon', val => props.eyebrowIcon = val);
    bindInput(container, '#pbRrEyebrowText', val => props.eyebrowText = val);
    bindInput(container, '#pbRrTitle', val => props.title = val);

    const itemsContainer = container.querySelector('#pbRecentReadsItems');
    if (!Array.isArray(props.reads)) props.reads = [];
    props.reads.forEach((rr, idx) => {
      const card = document.createElement('div');
      card.className = 'pb-subitem-card';
      card.innerHTML = `
        <div class="pb-subitem-header">
          <span>Article #${idx + 1} &bull; ${escapeHtml(rr.id || '')}</span>
        </div>
        <input type="text" class="pb-input" placeholder="Article Title" value="${escapeHtml(rr.title || '')}" maxlength="200" style="margin-bottom: 8px;" ${readOnlyAttr}>
        <textarea class="pb-textarea" placeholder="Article Excerpt" maxlength="500" style="margin-bottom: 8px;" ${readOnlyAttr}>${escapeHtml(rr.excerpt || '')}</textarea>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
          <input type="text" class="pb-input" placeholder="Date (e.g. June 12, 2026)" value="${escapeHtml(rr.date || '')}" maxlength="50" ${readOnlyAttr}>
          <input type="text" class="pb-input" placeholder="Target Path (/blog...)" value="${escapeHtml(rr.path || '')}" maxlength="1024" ${readOnlyAttr}>
        </div>
      `;
      const inputs = card.querySelectorAll('input, textarea');
      inputs[0].addEventListener('input', e => { rr.title = e.target.value; updateBadges(); });
      inputs[1].addEventListener('input', e => { rr.excerpt = e.target.value; updateBadges(); });
      inputs[2].addEventListener('input', e => { rr.date = e.target.value; updateBadges(); });
      inputs[3].addEventListener('input', e => { rr.path = e.target.value; updateBadges(); });
      itemsContainer.appendChild(card);
    });
  }

  function bindInput(container, selector, setter) {
    const el = container.querySelector(selector);
    if (el) {
      el.addEventListener('input', e => {
        setter(e.target.value);
        updateBadges();
      });
    }
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // Action: Save Draft
  async function handleSaveDraft() {
    if (isSaving || !window.__PB_CAN_EDIT__) return;
    isSaving = true;
    updateBadges();

    const saveBtn = document.getElementById('pbSaveBtn');
    const originalText = saveBtn ? saveBtn.innerHTML : '';
    if (saveBtn) saveBtn.innerHTML = '💾 Saving...';

    try {
      const res = await pbFetch('/admin/page-builder/draft', {
        method: 'POST',
        body: JSON.stringify({
          document: currentDoc,
          expectedVersion: currentDoc.version
        })
      });

      const data = await res.json().catch(() => null);

      if (res.status === 200 && data && data.success) {
        currentDoc.version = data.version;
        originalDoc = deepClone(currentDoc);
        showToast(`Draft saved successfully as version v${data.version}`, 'success');
      } else if (res.status === 409) {
        showToast(data?.error || 'Version conflict: A newer draft exists on the server. Please reload.', 'error');
      } else if (res.status === 400) {
        const issuesMsg = data?.issues ? data.issues.map(i => i.message || i).join('; ') : data?.error;
        showToast('Validation failed: ' + issuesMsg, 'error');
      } else if (res.status === 403) {
        showToast(data?.error || 'Access denied: Edit permission required.', 'error');
      } else {
        showToast(data?.error || `Failed to save draft (HTTP ${res.status})`, 'error');
      }
    } catch (err) {
      showToast('Network error while saving draft: ' + err.message, 'error');
    } finally {
      isSaving = false;
      if (saveBtn) saveBtn.innerHTML = originalText;
      updateBadges();
    }
  }

  // Action: Preview Draft
  function handlePreviewDraft() {
    if (isDirty()) {
      alert('You have unsaved changes. Please click "Save Draft" before starting preview mode.');
      return;
    }
    window.open('/admin/page-builder/preview', '_blank');
  }

  // Action: Publish Homepage
  function handlePublishClick() {
    if (!window.__PB_CAN_PUBLISH__) return;
    if (isDirty()) {
      alert('You have unsaved changes. Please save the draft before publishing.');
      return;
    }

    const modal = document.getElementById('pbPublishModal');
    const versionSpan = document.getElementById('pbPublishVersionText');
    if (versionSpan && currentDoc) {
      versionSpan.textContent = 'v' + currentDoc.version;
    }
    if (modal) modal.style.display = 'flex';
  }

  async function handleConfirmPublish() {
    if (isPublishing || !window.__PB_CAN_PUBLISH__) return;
    isPublishing = true;

    const modal = document.getElementById('pbPublishModal');
    const confirmBtn = document.getElementById('pbConfirmPublishBtn');
    if (confirmBtn) confirmBtn.innerHTML = 'Publishing...';

    try {
      const res = await pbFetch('/admin/page-builder/publish', {
        method: 'POST',
        body: JSON.stringify({
          expectedVersion: currentDoc.version
        })
      });

      const data = await res.json().catch(() => null);

      if (res.status === 200 && data && data.success) {
        if (modal) modal.style.display = 'none';
        showToast(`Homepage published successfully at v${data.version}. Revision recorded.`, 'success');
      } else if (res.status === 409) {
        showToast(data?.error || 'Publish conflict: Draft version mismatch.', 'error');
      } else if (res.status === 403) {
        showToast(data?.error || 'Access denied: Only administrators can publish.', 'error');
      } else {
        showToast(data?.error || 'Failed to publish homepage', 'error');
      }
    } catch (err) {
      showToast('Error publishing homepage: ' + err.message, 'error');
    } finally {
      isPublishing = false;
      if (confirmBtn) confirmBtn.innerHTML = 'Confirm Publication';
      updateBadges();
    }
  }

  // Action: Revisions Drawer
  async function handleOpenRevisions() {
    const drawer = document.getElementById('pbRevisionsDrawer');
    const listContainer = document.getElementById('pbRevisionsList');
    if (drawer) drawer.style.display = 'flex';
    if (listContainer) listContainer.innerHTML = '<div style="padding: 24px; text-align: center; color: #64748b;">Loading revision history...</div>';

    try {
      const res = await pbFetch('/admin/page-builder/revisions?limit=30');
      const data = await res.json().catch(() => null);

      if (res.ok && data && Array.isArray(data.revisions)) {
        renderRevisionsList(listContainer, data.revisions);
      } else {
        if (listContainer) listContainer.innerHTML = `<div style="padding: 24px; color: #dc2626;">Failed to load revisions: ${data?.error || res.status}</div>`;
      }
    } catch (err) {
      if (listContainer) listContainer.innerHTML = `<div style="padding: 24px; color: #dc2626;">Error loading revisions: ${err.message}</div>`;
    }
  }

  function renderRevisionsList(container, revisions) {
    if (!revisions || revisions.length === 0) {
      container.innerHTML = '<div style="padding: 24px; text-align: center; color: #64748b;">No published revisions recorded yet.</div>';
      return;
    }

    container.innerHTML = '';
    revisions.forEach(rev => {
      const item = document.createElement('div');
      item.className = 'pb-revision-item';

      const createdDate = rev.createdAt ? new Date(rev.createdAt).toLocaleString() : 'N/A';
      const headline = rev.heroHeadline ? `&ldquo;${escapeHtml(rev.heroHeadline)}&rdquo;` : 'No headline';
      const author = rev.publishedBy ? escapeHtml(rev.publishedBy) : 'System';

      item.innerHTML = `
        <div class="pb-revision-header">
          <span class="pb-revision-id">Revision #${rev.id} &bull; v${rev.version || 1}</span>
          <span class="pb-revision-date">${createdDate}</span>
        </div>
        <div class="pb-revision-author">Published by: <strong>${author}</strong></div>
        <div class="pb-revision-summary">
          <div>Headline: ${headline}</div>
          <div>Sections: ${rev.sectionsCount || 0} (${(rev.enabledSections || []).join(', ')})</div>
        </div>
        <div style="margin-top: 10px; display: flex; justify-content: flex-end;">
          <button type="button" class="admin-btn admin-btn-sm admin-btn-secondary pb-restore-btn" ${!window.__PB_CAN_EDIT__ ? 'disabled' : ''}>
            ↺ Restore to Draft
          </button>
        </div>
      `;

      const restoreBtn = item.querySelector('.pb-restore-btn');
      restoreBtn.addEventListener('click', () => {
        handleInitiateRestore(rev);
      });

      container.appendChild(item);
    });
  }

  // Restore revision handler
  let pendingRestoreRevision = null;
  function handleInitiateRestore(rev) {
    if (!window.__PB_CAN_EDIT__) return;
    if (isDirty()) {
      alert('You have unsaved changes in the editor. Please save or discard them before restoring a revision.');
      return;
    }

    pendingRestoreRevision = rev;
    const modal = document.getElementById('pbRestoreModal');
    const textSpan = document.getElementById('pbRestoreDetailsText');
    if (textSpan) {
      textSpan.textContent = `Revision #${rev.id} (version v${rev.version || 1}, published by ${rev.publishedBy || 'System'})`;
    }
    if (modal) modal.style.display = 'flex';
  }

  async function handleConfirmRestore() {
    if (!pendingRestoreRevision || isRestoring || !window.__PB_CAN_EDIT__) return;
    isRestoring = true;

    const modal = document.getElementById('pbRestoreModal');
    const drawer = document.getElementById('pbRevisionsDrawer');
    const confirmBtn = document.getElementById('pbConfirmRestoreBtn');
    if (confirmBtn) confirmBtn.innerHTML = 'Restoring...';

    try {
      const res = await pbFetch('/admin/page-builder/restore', {
        method: 'POST',
        body: JSON.stringify({
          revisionId: pendingRestoreRevision.id,
          expectedDraftVersion: currentDoc.version
        })
      });

      const data = await res.json().catch(() => null);

      if (res.status === 200 && data && data.success) {
        currentDoc = deepClone(data.document);
        originalDoc = deepClone(data.document);
        selectedId = 'seo';

        if (modal) modal.style.display = 'none';
        if (drawer) drawer.style.display = 'none';

        renderSectionList();
        renderInspector();
        updateBadges();

        showToast(`Revision #${pendingRestoreRevision.id} restored to draft v${data.version}. Published homepage remains untouched.`, 'success');
      } else if (res.status === 409) {
        showToast(data?.error || 'Restore conflict: draft version has changed. Please reload.', 'error');
      } else {
        showToast(data?.error || 'Failed to restore revision', 'error');
      }
    } catch (err) {
      showToast('Error restoring revision: ' + err.message, 'error');
    } finally {
      isRestoring = false;
      pendingRestoreRevision = null;
      if (confirmBtn) confirmBtn.innerHTML = 'Confirm Restore to Draft';
    }
  }

  // Toast Notification helper
  function showToast(message, type = 'info') {
    const existing = document.getElementById('pbToastAlert');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.id = 'pbToastAlert';
    toast.style.cssText = `
      position: fixed;
      bottom: 24px;
      right: 24px;
      z-index: 9999;
      padding: 12px 20px;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 500;
      color: #ffffff;
      box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1);
      display: flex;
      align-items: center;
      gap: 10px;
      background: ${type === 'success' ? '#15803d' : (type === 'error' ? '#b91c1c' : '#1e293b')};
      animation: pbToastIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
    `;
    toast.innerHTML = `<span>${escapeHtml(message)}</span><button style="background:none;border:none;color:#ffffff;cursor:pointer;font-size:16px;">&times;</button>`;
    toast.querySelector('button').addEventListener('click', () => toast.remove());
    document.body.appendChild(toast);

    setTimeout(() => {
      if (toast.parentElement) toast.remove();
    }, 5000);
  }

  // Initialization
  document.addEventListener('DOMContentLoaded', () => {
    const rawDocTag = document.getElementById('pbInitialDocJson');
    if (!rawDocTag) {
      console.warn('Page Builder initial JSON not found.');
      return;
    }

    try {
      const parsed = JSON.parse(rawDocTag.textContent);
      originalDoc = deepClone(parsed);
      currentDoc = deepClone(parsed);
    } catch (err) {
      console.error('Failed to parse initial document JSON:', err);
      return;
    }

    // Bind action buttons
    const saveBtn = document.getElementById('pbSaveBtn');
    if (saveBtn) saveBtn.addEventListener('click', handleSaveDraft);

    const previewBtn = document.getElementById('pbPreviewBtn');
    if (previewBtn) previewBtn.addEventListener('click', handlePreviewDraft);

    const publishBtn = document.getElementById('pbPublishBtn');
    if (publishBtn) publishBtn.addEventListener('click', handlePublishClick);

    const revisionsBtn = document.getElementById('pbRevisionsBtn');
    if (revisionsBtn) revisionsBtn.addEventListener('click', handleOpenRevisions);

    // Modal close buttons
    document.querySelectorAll('.pb-modal-close-trigger').forEach(btn => {
      btn.addEventListener('click', () => {
        const modal = btn.closest('.pb-modal-overlay');
        if (modal) modal.style.display = 'none';
      });
    });

    const confirmPublishBtn = document.getElementById('pbConfirmPublishBtn');
    if (confirmPublishBtn) confirmPublishBtn.addEventListener('click', handleConfirmPublish);

    const confirmRestoreBtn = document.getElementById('pbConfirmRestoreBtn');
    if (confirmRestoreBtn) confirmRestoreBtn.addEventListener('click', handleConfirmRestore);

    // Initial render
    renderSectionList();
    renderInspector();
    updateBadges();

    console.info('Page Builder Vanilla JS client initialized.');
  });
})();
