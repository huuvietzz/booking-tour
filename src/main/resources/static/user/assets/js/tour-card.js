/**
 * assets/js/tour-card.js
 *
 * Thẻ tour phía khách — DÙNG CHUNG cho tours.html và index.html (Tour trọn gói).
 * Đi kèm assets/css/tour-card.css. Cần Bootstrap (modal) + auth-ui.js (biết đã đăng nhập chưa).
 *
 *   TourCard.html(tour, { view: 'grid' | 'list', selectedDate: 'yyyy-mm-dd' })  → HTML <article> 1 thẻ
 *   TourCard.initDateNavs(rootElement)  → gọi SAU KHI chèn thẻ vào trang (ẩn/hiện nút ‹ › dãy ngày)
 *
 * tour = TourCardResponse { tourId, tourCode, thumbnailUrl, tourClass, tourName,
 *                           departureLocation, duration, basePrice, departureDates[] }
 */
(function () {
    'use strict';

    const CLASS_LABELS = { BUDGET: 'Tiết kiệm', STANDARD: 'Tiêu chuẩn', GOOD_PRICE: 'Giá tốt', PREMIUM: 'Cao cấp' };

    /* Xem chi tiết mở TAB MỚI: trang đang xem giữ nguyên bộ lọc + các tour đã tải + vị trí cuộn.
       noopener: trang mới không điều khiển ngược được tab này qua window.opener */
    const NEW_TAB = 'target="_blank" rel="noopener"';

    // ----- Tiện ích -----
    function escapeHtml(str) {
        return String(str ?? '').replace(/[&<>"']/g, c =>
            ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    }
    function parseIso(s) { const [y, m, d] = s.split('-').map(Number); return new Date(y, m - 1, d); }
    /** "2026-11-26" → "26/11" */
    function shortDate(s) { const [, m, d] = s.split('-'); return `${d}/${m}`; }
    const moneyFmt = new Intl.NumberFormat('vi-VN');
    function fmtMoney(v) { return v == null ? '' : moneyFmt.format(v) + ' ₫'; }

    /** "4N3Đ" → "4 ngày 3 đêm"; "1N0Đ" → "Trong ngày" (không đúng dạng thì giữ nguyên) */
    function durationText(d) {
        const m = /^\s*(\d+)\s*N\s*(\d+)\s*Đ/i.exec(d || '');
        if (!m) return d || '';
        return m[1] === '1' && m[2] === '0' ? 'Trong ngày' : `${m[1]} ngày ${m[2]} đêm`;
    }
    /** Chữ ngắn trên thẻ dọc: giữ "4N3Đ", riêng tour trong ngày ghi "Trong ngày" */
    function durationShort(d) {
        const full = durationText(d);
        return full === 'Trong ngày' ? full : (d || '');
    }

    /* ============================================================
       HTML 1 THẺ
       ============================================================ */
    function html(t, opts = {}) {
        const asList = opts.view === 'list';
        const selectedDate = opts.selectedDate || '';
        const url = `tour-details.html?id=${t.tourId}`;

        // departureDates: [{ scheduleId, departureDate, status }] — BE đã lọc: sau hôm nay, chưa huỷ, tăng dần
        const dates = t.departureDates || [];
        const chip = d => d.status === 'FULL'
            ? `<span class="tl-date is-full" title="Hết chỗ">${shortDate(d.departureDate)}</span>`
            : `<a href="${url}&date=${d.departureDate}" ${NEW_TAB} class="tl-date ${d.departureDate === selectedDate ? 'active' : ''}"
                  title="Khởi hành ${parseIso(d.departureDate).toLocaleDateString('vi-VN')}">${shortDate(d.departureDate)}</a>`;
        const datesHtml = dates.length
            ? `<div class="tl-dates">
                 <button type="button" class="tl-dates-nav prev is-hidden" aria-label="Ngày trước" onclick="scrollDates(this, -1)">
                   <i class="bi bi-chevron-left"></i></button>
                 <div class="tl-dates-track" onscroll="updateDateNav(this.parentElement)">${dates.map(chip).join('')}</div>
                 <button type="button" class="tl-dates-nav next" aria-label="Ngày sau" onclick="scrollDates(this, 1)">
                   <i class="bi bi-chevron-right"></i></button>
               </div>`
            : '<div class="tl-no-date">Liên hệ để nhận lịch khởi hành</div>';

        const img = t.thumbnailUrl
            ? `<img src="${escapeHtml(t.thumbnailUrl)}" alt="${escapeHtml(t.tourName)}" loading="lazy">`
            : '';
        return `
        <article class="tl-card ${asList ? 'tl-card--list' : ''}">
          <div class="tl-card-img">
            <a href="${url}" ${NEW_TAB} class="tl-card-img-link" aria-label="${escapeHtml(t.tourName)}">${img}</a>
            ${CLASS_LABELS[t.tourClass] ? `<span class="tl-badge ${t.tourClass}">${CLASS_LABELS[t.tourClass]}</span>` : ''}
            ${favButtonHtml(t)}
          </div>
          <div class="tl-card-body">
            <div class="tl-tip tl-tip-wide" data-tip="${escapeHtml(t.tourName)}">
              <h3 class="tl-card-title"><a href="${url}" ${NEW_TAB}>${escapeHtml(t.tourName)}</a></h3>
            </div>
            ${asList ? `
            <div class="tl-info">
              <div><i class="bi bi-upc-scan"></i>Mã tour: <b>${escapeHtml(t.tourCode)}</b></div>
              <div><i class="bi bi-geo-alt"></i>Điểm khởi hành: <b>${escapeHtml(t.departureLocation)}</b></div>
              <div><i class="bi bi-clock"></i>Thời gian: <b>${escapeHtml(durationText(t.duration))}</b></div>
              <div class="js-transport" data-tour-id="${t.tourId}">
                <i class="bi bi-signpost-2"></i>Phương tiện: <b class="tl-muted-val">Đang tải…</b></div>
            </div>
            <div class="tl-info-dates">
              <span class="lbl"><i class="bi bi-calendar3"></i>Ngày khởi hành:</span>
              ${datesHtml}
            </div>` : `
            <div class="tl-meta">
              <span class="tl-tip" data-tip="Điểm khởi hành">
                <i class="bi bi-geo-alt"></i><span class="tl-ellipsis">${escapeHtml(t.departureLocation)}</span></span>
              <span class="tl-tip tl-tip-right" data-tip="${escapeHtml(durationText(t.duration))}">
                <i class="bi bi-clock"></i>${escapeHtml(durationShort(t.duration))}</span>
            </div>
            <div class="tl-dates-label">Ngày khởi hành</div>
            ${datesHtml}`}
            <div class="tl-card-foot">
              <div class="tl-price">
                <small>Giá từ</small>
                <strong>${fmtMoney(t.basePrice)}</strong>
              </div>
              <a href="${url}" ${NEW_TAB} class="tl-detail-btn">Chi tiết</a>
            </div>
          </div>
        </article>`;
    }

    /* ============================================================
       DÃY NGÀY KHỞI HÀNH: nút ‹ ›
       (gọi từ onclick / onscroll trong HTML thẻ → phải là hàm toàn cục)
       ============================================================ */
    /** Bấm ‹ / › → trượt dãy ngày khoảng 2/3 bề rộng đang thấy */
    function scrollDates(btn, dir) {
        const track = btn.parentElement.querySelector('.tl-dates-track');
        track.scrollBy({ left: dir * track.clientWidth * 0.66 });
    }

    /** Còn ngày bị khuất bên trái → hiện ‹ ; còn bên phải → hiện › ; kịch mép nào thì ẩn nút mép đó */
    function updateDateNav(wrap) {
        const track = wrap.querySelector('.tl-dates-track');
        const atStart = track.scrollLeft <= 1;
        const atEnd   = track.scrollLeft + track.clientWidth >= track.scrollWidth - 1;
        wrap.querySelector('.prev').classList.toggle('is-hidden', atStart);
        wrap.querySelector('.next').classList.toggle('is-hidden', atEnd);
    }

    function initDateNavs(root) {
        (root || document).querySelectorAll('.tl-dates').forEach(updateDateNav);
    }
    window.addEventListener('resize', () => initDateNavs(document));

    /* ============================================================
       YÊU THÍCH — nối BE /api/favorites (bắt buộc đăng nhập)
         GET    /api/favorites/ids        → id các tour đã thích (tô đỏ tim)
         PUT    /api/favorites/{tourId}   → thêm
         DELETE /api/favorites/{tourId}   → bỏ
       Bấm tim: đổi giao diện NGAY (không chờ mạng), API lỗi thì trả tim về như cũ.
       Mỗi lần đổi thành công phát sự kiện "favorite:change" { tourId, on }
       → trang khác (profile.html) nghe để gỡ thẻ khỏi danh sách.
       ============================================================ */
    const FAV_API = '/api/favorites';
    const favorites = new Set();   // tourId đã thích
    const pending = new Set();     // tourId đang chờ API → chặn bấm liên tục gửi nhiều request

    function authHeader() {
        return { 'Authorization': 'Bearer ' + localStorage.getItem('token') };
    }

    function favButtonHtml(t) {
        const on = favorites.has(t.tourId);
        return `<button type="button" class="tl-fav ${on ? 'is-active' : ''}" data-tour-id="${t.tourId}"
                    aria-pressed="${on}" aria-label="${on ? 'Bỏ khỏi' : 'Thêm vào'} tour yêu thích"
                    title="${on ? 'Bỏ yêu thích' : 'Yêu thích'}">
              <i class="bi ${on ? 'bi-heart-fill' : 'bi-heart'}"></i>
            </button>`;
    }

    /** Cùng 1 tour có thể hiện ở nhiều chỗ trên trang → đổi hết các nút của tour đó */
    function paintFav(id, on) {
        document.querySelectorAll(`.tl-fav[data-tour-id="${id}"]`).forEach(b => {
            b.classList.toggle('is-active', on);
            b.setAttribute('aria-pressed', on);
            b.setAttribute('aria-label', `${on ? 'Bỏ khỏi' : 'Thêm vào'} tour yêu thích`);
            b.title = on ? 'Bỏ yêu thích' : 'Yêu thích';
            b.querySelector('i').className = `bi ${on ? 'bi-heart-fill' : 'bi-heart'}`;
        });
    }

    /** Trang tự biết chắc các tour này đã thích (vd: danh sách yêu thích) → tô đỏ ngay, không chờ /ids */
    function markFavorites(ids) {
        ids.forEach(id => { favorites.add(Number(id)); paintFav(Number(id), true); });
    }

    /** Lúc mở trang: lấy id đã thích. Thẻ có thể đã vẽ trước khi API về → tô lại các nút đang có. */
    async function loadFavoriteIds() {
        if (!window.AuthUI?.getCurrentUser()) return;
        try {
            const res = await fetch(FAV_API + '/ids', { headers: authHeader() });
            if (!res.ok) return;   // 401 token hết hạn / 403 tài khoản nhân viên → coi như chưa thích gì
            const json = await res.json();
            if (json && json.success) markFavorites(json.data || []);
        } catch (e) {
            console.error(e);      // lỗi mạng: tim để trắng, không chặn trang
        }
    }

    /** Hộp nhắc đăng nhập: tạo 1 lần khi cần, trang dùng thẻ không phải tự chèn HTML */
    function showLoginRequired() {
        let el = document.getElementById('loginRequiredModal');
        if (!el) {
            document.body.insertAdjacentHTML('beforeend', `
              <div class="modal fade tl-login-modal" id="loginRequiredModal" tabindex="-1" aria-labelledby="loginRequiredTitle" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered" style="max-width: 380px;">
                  <div class="modal-content">
                    <div class="tl-login-icon"><i class="bi bi-heart-fill"></i></div>
                    <h5 id="loginRequiredTitle">Bạn chưa đăng nhập</h5>
                    <p>Vui lòng đăng nhập để lưu tour yêu thích.</p>
                    <div class="d-flex justify-content-center gap-2">
                      <button type="button" class="btn btn-light" data-bs-dismiss="modal">Để sau</button>
                      <a href="pages-login.html" class="btn btn-login">Đăng nhập</a>
                    </div>
                  </div>
                </div>
              </div>`);
            el = document.getElementById('loginRequiredModal');
        }
        bootstrap.Modal.getOrCreateInstance(el).show();
    }

    // 1 listener cho cả trang: thẻ được vẽ lại liên tục (lọc, Xem thêm, đổi danh mục)
    document.addEventListener('click', async e => {
        const btn = e.target.closest('.tl-fav');
        if (!btn) return;

        // Chưa đăng nhập → nhắc đăng nhập, không đổi trạng thái tim
        if (!window.AuthUI?.getCurrentUser()) {
            showLoginRequired();
            return;
        }

        const id = Number(btn.dataset.tourId);
        if (pending.has(id)) return;
        const on = !favorites.has(id);

        // Đổi giao diện trước cho mượt
        on ? favorites.add(id) : favorites.delete(id);
        paintFav(id, on);
        closeFavPop();
        pending.add(id);

        try {
            const res = await fetch(`${FAV_API}/${id}`, { method: on ? 'PUT' : 'DELETE', headers: authHeader() });
            const json = await res.json().catch(() => null);
            if (!res.ok || !json || !json.success) {
                const err = new Error((json && json.message) || 'Không lưu được, vui lòng thử lại');
                err.status = res.status;
                throw err;
            }
            if (on && btn.isConnected) showFavPop(btn);   // chỉ báo khi THÊM; bỏ thích thì tim trắng lại là đủ rõ
            document.dispatchEvent(new CustomEvent('favorite:change', { detail: { tourId: id, on } }));
        } catch (err) {
            // Trả tim về như cũ
            on ? favorites.delete(id) : favorites.add(id);
            paintFav(id, !on);
            if (err.status === 401) {
                showLoginRequired();   // token hết hạn
            } else if (btn.isConnected) {
                showFavPop(btn, err.status === 403
                    ? 'Tài khoản nhân viên không dùng được chức năng này'
                    : (err.message === 'Failed to fetch' ? 'Không kết nối được tới máy chủ' : err.message));
            }
        } finally {
            pending.delete(id);
        }
    });

    /* ----- Bong bóng "Đã lưu vào: Tour yêu thích" ----- */
    const FAV_POP_MS = 4000;   // tự ẩn sau 4 giây
    let favPop = null, favPopTimer = null;

    /** errorText: có → bong bóng báo lỗi; không → "Đã lưu vào: Tour yêu thích" */
    function showFavPop(btn, errorText) {
        closeFavPop();
        favPop = document.createElement('div');
        favPop.className = 'tl-fav-pop' + (errorText ? ' is-error' : '');
        favPop.setAttribute('role', 'status');   // trình đọc màn hình đọc thông báo
        if (errorText) favPop.textContent = errorText;
        else favPop.innerHTML = 'Đã lưu vào:<a href="profile.html#favorites">Tour yêu thích</a>';
        btn.after(favPop);

        // Đang rê chuột vào bong bóng (định bấm link) thì không tự ẩn
        favPop.addEventListener('mouseenter', () => clearTimeout(favPopTimer));
        favPop.addEventListener('mouseleave', startFavPopTimer);
        startFavPopTimer();
    }

    function startFavPopTimer() {
        clearTimeout(favPopTimer);
        favPopTimer = setTimeout(closeFavPop, FAV_POP_MS);
    }

    function closeFavPop() {
        clearTimeout(favPopTimer);
        favPop?.remove();
        favPop = null;
    }

    // Bấm ra ngoài (trừ chính nút tim / bong bóng) hoặc Esc → đóng
    document.addEventListener('click', e => {
        if (favPop && !e.target.closest('.tl-fav-pop, .tl-fav')) closeFavPop();
    });
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeFavPop(); });

    // ----- Xuất ra ngoài -----
    window.scrollDates = scrollDates;
    window.updateDateNav = updateDateNav;
    window.TourCard = { html, initDateNavs, markFavorites, CLASS_LABELS, durationText };

    // Tự tải danh sách đã thích khi mở trang (script ở cuối body nên DOM đã sẵn)
    loadFavoriteIds();
})();