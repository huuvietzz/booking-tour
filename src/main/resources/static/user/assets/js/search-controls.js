/**
 * assets/js/search-controls.js
 *
 * Ô chọn của thanh tìm kiếm — DÙNG CHUNG cho tours.html và index.html. Đi kèm assets/css/search-controls.css.
 *
 *   enhanceSelect(selectEl, { search, images, heading })
 *       Biến <select> thành combobox bo tròn xổ xuống. <select> gốc vẫn giữ (ẩn) để code đọc / ghi .value như thường;
 *       ghi .value bằng code xong thì gọi refreshBarSelects() cho chữ hiển thị cập nhật theo.
 *       images: true → mỗi lựa chọn có ảnh tròn lấy từ <option data-img="...">.
 *
 *   initDatePicker(ddEl, hiddenInput, labelEl)
 *       Lịch chọn "Ngày đi" (chỉ từ ngày mai). Giá trị "yyyy-mm-dd" ghi vào hiddenInput.
 *       ddEl là khung .tl-dd.tl-dd--date chứa .tl-dp-title, 2 nút .tl-dp-nav, .tl-dp-grid, nút xoá trong .tl-dp-foot.
 *
 *   openDropdown(ddEl, open?)   refreshBarSelects()
 */
(function () {
    'use strict';

    // ----- Tiện ích -----
    function escapeHtml(str) {
        return String(str ?? '').replace(/[&<>"']/g, c =>
            ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    }
    /** "Hạ Long" → "ha long": tìm không dấu */
    function removeAccents(text) {
        return String(text || '').replace(/đ/g, 'd').replace(/Đ/g, 'D').normalize('NFD').replace(/\p{M}+/gu, '').toLowerCase();
    }
    /** Date → "2026-11-08" theo giờ máy (toISOString lệch múi giờ UTC) */
    function toIso(d) {
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    }
    function parseIso(s) { const [y, m, d] = s.split('-').map(Number); return new Date(y, m - 1, d); }

    /* ============================================================
       MỞ / ĐÓNG
       ============================================================ */
    function openDropdown(dd, open = !dd.classList.contains('open')) {
        document.querySelectorAll('.tl-dd.open').forEach(o => { if (o !== dd) openDropdown(o, false); });
        dd.classList.toggle('open', open);
        dd.querySelector('.tl-dd-toggle').setAttribute('aria-expanded', open);
        if (open) {
            dd._render?.();                                   // vẽ lại danh sách / lịch mỗi lần mở
            (dd.querySelector('.tl-dd-search input') || dd.querySelector('li.active'))?.focus();
        }
    }
    document.addEventListener('click', e => {
        const btn = e.target.closest('.tl-dd-toggle');
        if (btn) openDropdown(btn.closest('.tl-dd'));
    });
    // Bấm ra ngoài / Esc → đóng. composedPath: phần tử vừa bấm có thể đã bị vẽ lại (đổi tháng) và rời khỏi trang
    document.addEventListener('click', e => {
        const inside = e.composedPath().some(el => el.classList?.contains('tl-dd'));
        if (!inside) document.querySelectorAll('.tl-dd.open').forEach(dd => openDropdown(dd, false));
    });
    document.addEventListener('keydown', e => {
        if (e.key === 'Escape') document.querySelectorAll('.tl-dd.open').forEach(dd => openDropdown(dd, false));
    });

    /* ============================================================
       COMBOBOX
       ============================================================ */
    const refreshers = [];   // cập nhật chữ hiển thị sau khi code đổi .value

    /** Ảnh tròn bên trái mỗi lựa chọn (option có data-img); không có ảnh / ảnh lỗi → biểu tượng ghim bản đồ */
    function optionThumb(o) {
        if (o.value === '' || o.value === '__multi') return '<span class="tl-dd-thumb"><i class="bi bi-globe-asia-australia"></i></span>';
        const fallback = `this.outerHTML='<span class=&quot;tl-dd-thumb&quot;><i class=&quot;bi bi-geo-alt&quot;></i></span>'`;
        return o.dataset.img
            ? `<img class="tl-dd-thumb" src="${escapeHtml(o.dataset.img)}" alt="" loading="lazy" onerror="${fallback}">`
            : '<span class="tl-dd-thumb"><i class="bi bi-geo-alt"></i></span>';
    }

    function enhanceSelect(sel, { search = false, images = false, heading = '' } = {}) {
        sel.classList.add('d-none');
        const dd = document.createElement('div');
        dd.className = 'tl-dd tl-dd--bar' + (images ? ' tl-dd--images' : '');
        dd.innerHTML = `
          <button type="button" class="tl-dd-toggle" id="${sel.id}Btn" aria-haspopup="listbox" aria-expanded="false">
            <span class="tl-dd-label"></span><i class="bi bi-chevron-down"></i>
          </button>
          <div class="tl-dd-menu">
            ${search ? '<label class="tl-dd-search"><i class="bi bi-search"></i><input type="text" placeholder="Tìm kiếm" autocomplete="off"></label>' : ''}
            ${heading ? `<div class="tl-dd-heading">${escapeHtml(heading)}</div>` : ''}
            <ul role="listbox"></ul>
          </div>`;
        sel.after(dd);
        document.querySelector(`label[for="${sel.id}"]`)?.setAttribute('for', `${sel.id}Btn`);

        const ul = dd.querySelector('ul'), input = dd.querySelector('.tl-dd-search input');
        const label = dd.querySelector('.tl-dd-label');

        dd._render = () => {
            const q = removeAccents(input?.value.trim() || '');
            const opts = [...sel.options].filter(o => !q || removeAccents(o.text).includes(q));
            ul.innerHTML = opts.length
                ? opts.map(o => `<li role="option" tabindex="0" data-value="${escapeHtml(o.value)}"
                                   class="${o.value === sel.value ? 'active' : ''}" aria-selected="${o.value === sel.value}">${images ? optionThumb(o) : ''}<span>${escapeHtml(o.text)}</span></li>`).join('')
                : '<div class="tl-dd-empty">Không tìm thấy</div>';
        };
        const refresh = () => { label.textContent = sel.options[sel.selectedIndex]?.text || ''; };
        const pick = li => {
            sel.value = li.dataset.value;
            sel.dispatchEvent(new Event('change', { bubbles: true }));
            refresh();
            openDropdown(dd, false);
            dd.querySelector('.tl-dd-toggle').focus();
        };

        ul.addEventListener('click', e => { const li = e.target.closest('li'); if (li) pick(li); });
        ul.addEventListener('keydown', e => {
            const li = e.target.closest('li'); if (!li) return;
            if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); pick(li); }
            if (e.key === 'ArrowDown') { e.preventDefault(); li.nextElementSibling?.focus(); }
            if (e.key === 'ArrowUp')   { e.preventDefault(); (li.previousElementSibling || input)?.focus(); }
        });
        input?.addEventListener('input', dd._render);
        input?.addEventListener('keydown', e => { if (e.key === 'ArrowDown') { e.preventDefault(); ul.querySelector('li')?.focus(); } });

        refresh();
        refreshers.push(refresh);
        return dd;
    }
    function refreshBarSelects() { refreshers.forEach(fn => fn()); }

    /* ============================================================
       LỊCH CHỌN "NGÀY ĐI"
       ============================================================ */
    function initDatePicker(dd, hidden, labelEl) {
        const title = dd.querySelector('.tl-dp-title');
        const [prev, next] = dd.querySelectorAll('.tl-dp-nav');
        const grid = dd.querySelector('.tl-dp-grid');
        const clear = dd.querySelector('.tl-dp-foot button');

        const today = new Date(); today.setHours(0, 0, 0, 0);
        const todayIso = toIso(today);
        const tomorrow = new Date(today); tomorrow.setDate(tomorrow.getDate() + 1);
        const minIso = toIso(tomorrow);   // chỉ chọn từ ngày mai (BE yêu cầu ngày đi sau hôm nay)
        let month = new Date(today.getFullYear(), today.getMonth(), 1);

        function refreshLabel() {
            const v = hidden.value;
            labelEl.textContent = v
                ? parseIso(v).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
                : 'dd/mm/yyyy';
            labelEl.classList.toggle('is-placeholder', !v);
        }

        function render() {
            const y = month.getFullYear(), m = month.getMonth();
            const selected = hidden.value;
            title.textContent = `Tháng ${m + 1} - ${y}`;
            prev.disabled = y === today.getFullYear() && m === today.getMonth();

            let html = ['Th 2', 'Th 3', 'Th 4', 'Th 5', 'Th 6', 'Th 7', 'CN']
                .map((w, i) => `<div class="tl-dp-wd ${i === 6 ? 'sun' : ''}">${w}</div>`).join('');
            html += '<div></div>'.repeat((new Date(y, m, 1).getDay() + 6) % 7);   // ô trống trước ngày 1 (T2 là cột đầu)

            for (let d = 1, n = new Date(y, m + 1, 0).getDate(); d <= n; d++) {
                const iso = toIso(new Date(y, m, d));
                const cls = [
                    new Date(y, m, d).getDay() === 0 && 'sun',
                    iso === todayIso && 'today',
                    iso === selected && 'selected'
                ].filter(Boolean).join(' ');
                html += `<button type="button" class="tl-dp-day ${cls}" data-date="${iso}" ${iso < minIso ? 'disabled' : ''}
                                 aria-label="${d}/${m + 1}/${y}" ${iso === selected ? 'aria-pressed="true"' : ''}>${d}</button>`;
            }
            grid.innerHTML = html;
        }

        // Mở lịch → nhảy tới tháng của ngày đang chọn (chưa chọn thì tháng hiện tại)
        dd._render = () => {
            const base = hidden.value ? parseIso(hidden.value) : today;
            month = new Date(base.getFullYear(), base.getMonth(), 1);
            render();
        };
        prev.addEventListener('click', () => { month.setMonth(month.getMonth() - 1); render(); });
        next.addEventListener('click', () => { month.setMonth(month.getMonth() + 1); render(); });
        grid.addEventListener('click', e => {
            const day = e.target.closest('.tl-dp-day:not(:disabled)');
            if (!day) return;
            hidden.value = day.dataset.date;
            refreshLabel();
            openDropdown(dd, false);
        });
        clear?.addEventListener('click', () => {
            hidden.value = '';
            refreshLabel();
            openDropdown(dd, false);
        });

        refreshLabel();
        refreshers.push(refreshLabel);   // code đổi hidden.value rồi gọi refreshBarSelects() → nhãn cập nhật theo
    }

    // ----- Xuất ra ngoài (tours.html gọi trực tiếp các tên này) -----
    window.openDropdown = openDropdown;
    window.enhanceSelect = enhanceSelect;
    window.refreshBarSelects = refreshBarSelects;
    window.initDatePicker = initDatePicker;
})();