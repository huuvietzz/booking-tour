/**
 * admin/assets/js/sidebar-mini.js — menu trái thu gọn còn icon (màn ≥ 1200px), có nhớ trạng thái.
 *
 * Nhúng TRONG <head> (ngay sau admin-auth.js), KHÔNG để cuối trang:
 *   <link href="assets/css/sidebar-mini.css" rel="stylesheet">   (sau style.css)
 *   <script src="assets/js/sidebar-mini.js"></script>
 * Vì sao trong <head>: gắn class lên <html> TRƯỚC khi trang vẽ ra → mở trang mới không bị
 * menu bung rộng rồi mới co lại (nháy).
 *
 * - Màn ≥ 1200px: bấm ☰ = thu gọn / mở rộng (thay cho "ẩn hẳn" của NiceAdmin), lưu localStorage.
 * - Màn < 1200px: để nguyên NiceAdmin (☰ = trượt menu ra / vào).
 * - Thu gọn: rê chuột vào icon hiện tooltip tên menu (Bootstrap Tooltip).
 */
(function () {
    'use strict';

    const KEY = 'admin.sidebar.mini';
    const DESKTOP = window.matchMedia('(min-width: 1200px)');
    const root = document.documentElement;

    let saved = false;
    try { saved = localStorage.getItem(KEY) === '1'; } catch (_) { /* bị chặn: mặc định mở rộng */ }
    root.classList.toggle('sb-mini', saved);   // chạy ngay trong <head> → không nháy

    const isMini = () => root.classList.contains('sb-mini');

    function setMini(on) {
        root.classList.toggle('sb-mini', on);
        try { localStorage.setItem(KEY, on ? '1' : '0'); } catch (_) { /* bỏ qua */ }
        syncTooltips();
        // Nội dung rộng / hẹp lại → biểu đồ ApexCharts, bảng tính lại kích thước sau khi chạy xong hiệu ứng .3s
        setTimeout(() => window.dispatchEvent(new Event('resize')), 320);
    }

    // Bắt cú bấm ☰ ở pha "capture" (chạy TRƯỚC listener của NiceAdmin main.js) → trên màn lớn
    // tự xử lý và chặn NiceAdmin ẩn hẳn menu; màn nhỏ thì để NiceAdmin làm như cũ.
    document.addEventListener('click', e => {
        if (!e.target.closest('.toggle-sidebar-btn') || !DESKTOP.matches) return;
        e.stopImmediatePropagation();
        e.preventDefault();
        document.body.classList.remove('toggle-sidebar');   // phòng trạng thái "ẩn hẳn" cũ còn sót
        setMini(!isMini());
    }, true);

    /* ----- Tooltip tên menu (chỉ bật khi đang thu gọn trên màn lớn) ----- */
    let tips = [];
    function syncTooltips() {
        const on = isMini() && DESKTOP.matches;
        tips.forEach(t => (on ? t.enable() : (t.hide(), t.disable())));
    }

    document.addEventListener('DOMContentLoaded', () => {
        if (!window.bootstrap?.Tooltip) return;   // chưa có bootstrap.bundle → bỏ qua tooltip
        tips = [...document.querySelectorAll('#sidebar .sidebar-nav .nav-link')].map(a => {
            const label = a.querySelector('span')?.textContent.trim();
            if (!label) return null;
            return new bootstrap.Tooltip(a, {
                title: label,
                placement: 'right',
                trigger: 'hover',          // không dùng focus: bấm chuyển trang xong tooltip không bị kẹt
                customClass: 'sb-tooltip',
                container: 'body'
            });
        }).filter(Boolean);
        syncTooltips();
    });

    // Kéo cửa sổ qua lại mốc 1200px → bật / tắt tooltip cho đúng
    DESKTOP.addEventListener('change', syncTooltips);
})();
