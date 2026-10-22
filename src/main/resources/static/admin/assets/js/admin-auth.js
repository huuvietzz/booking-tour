/**
 * admin/assets/js/admin-auth.js
 *
 * Quản lý phiên đăng nhập trang quản trị (ADMIN, STAFF).
 * Dùng key có tiền tố "admin_" để không đụng tới phiên của trang khách hàng
 * (auth-ui.js dùng "token", "email", "fullName"...), vì hai bên cùng origin.
 *
 * Nhúng trong <head> của MỌI trang admin, TRƯỚC các script khác:
 *   <script src="assets/js/admin-auth.js"></script>
 *   <script>AdminAuth.requireLogin();</script>
 *
 * Và ngay sau </header> (tránh nhấp nháy tên / ảnh mẫu):
 *   <script>AdminAuth.renderUser();</script>
 */
(function () {
    'use strict';

    const TOKEN_KEY  = 'admin_token';
    const USER_KEY   = 'admin_user';
    const LOGIN_PAGE = 'pages-login.html';
    const HOME_PAGE  = 'index.html';
    const ALLOWED_ROLES = ['ADMIN', 'STAFF'];

    /** Avatar mặc định kiểu Facebook (đầu + vai xám), dạng data URI để gán thẳng vào <img src> */
    const DEFAULT_AVATAR =
        'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
            '<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">' +
            '<rect width="100" height="100" fill="#e4e6eb"/>' +
            '<circle cx="50" cy="38" r="18" fill="#bcc0c4"/>' +
            '<ellipse cx="50" cy="94" rx="33" ry="27" fill="#bcc0c4"/>' +
            '</svg>');

    function getToken() {
        return localStorage.getItem(TOKEN_KEY);
    }

    function getUser() {
        try {
            return JSON.parse(localStorage.getItem(USER_KEY));
        } catch (e) {
            return null;
        }
    }

    function setUser(user) {
        localStorage.setItem(USER_KEY, JSON.stringify(user));
    }

    /** Đọc exp trong JWT. Chỉ để điều hướng cho tiện, server mới là nơi kiểm tra thật. */
    function isTokenExpired(token) {
        try {
            const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
            const payload = JSON.parse(atob(base64));
            return payload.exp ? payload.exp * 1000 <= Date.now() : false;
        } catch (e) {
            return true;
        }
    }

    function clearSession() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
    }

    /** Có token, chưa hết hạn, đúng role ADMIN/STAFF */
    function isLoggedIn() {
        const token = getToken();
        const user  = getUser();
        return !!(token && user && ALLOWED_ROLES.includes(user.role) && !isTokenExpired(token));
    }

    function isAdmin() {
        const user = getUser();
        return !!user && user.role === 'ADMIN';
    }

    /** Lưu sau khi đăng nhập thành công, hoặc khi vừa đổi email (có token mới) */
    function saveSession(data) {
        localStorage.setItem(TOKEN_KEY, data.token);
        setUser({
            userId:    data.userId,
            email:     data.email,
            fullName:  data.fullName,
            role:      data.role,
            avatarUrl: data.avatarUrl || null
        });
    }

    /**
     * Cập nhật một phần thông tin đã lưu (vd: đổi tên, đổi ảnh) rồi vẽ lại header.
     * Không đụng tới token.
     */
    function updateUser(patch) {
        const user = getUser();
        if (!user) return;
        setUser(Object.assign(user, patch));
        renderUser();
    }

    function logout() {
        clearSession();
        window.location.replace(LOGIN_PAGE);
    }

    /**
     * Gọi ở <head> mọi trang admin (trừ trang login).
     * adminOnly = true cho trang chỉ ADMIN được vào (vd: staff.html).
     */
    function requireLogin(adminOnly) {
        if (!isLoggedIn()) {
            clearSession();
            window.location.replace(LOGIN_PAGE);
            return null;
        }
        if (adminOnly && !isAdmin()) {
            window.location.replace(HOME_PAGE);
            return null;
        }
        return getUser();
    }

    function authHeaders() {
        const token = getToken();
        return token
            ? { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token }
            : { 'Content-Type': 'application/json' };
    }

    /**
     * fetch có sẵn token. Token hết hạn / bị từ chối (401) thì tự đăng xuất.
     * 403 KHÔNG đăng xuất: đó là thiếu quyền, trang tự xử lý.
     */
    async function apiFetch(url, options) {
        const opts = Object.assign({}, options);
        opts.headers = Object.assign(authHeaders(), opts.headers || {});

        const res = await fetch(url, opts);
        if (res.status === 401) {
            logout();
            throw new Error('Phiên đăng nhập đã hết hạn');
        }
        return res;
    }

    /** Gán ảnh cho <img>: có URL thì dùng, không có hoặc ảnh hỏng thì về hình người rỗng */
    function setAvatarImg(img, avatarUrl) {
        img.onerror = () => {
            img.onerror = null;            // tránh lặp vô hạn nếu ảnh mặc định cũng lỗi
            img.src = DEFAULT_AVATAR;
        };
        img.src = avatarUrl || DEFAULT_AVATAR;
        // Ảnh upload có thể không vuông → cắt cho tròn đều, không méo
        img.style.width = '36px';
        img.style.height = '36px';
        img.style.objectFit = 'cover';
    }

    /**
     * Điền tên / vai trò / ảnh đại diện lên header.
     * Gọi ngay sau </header> để không thấy chữ giữ chỗ và ảnh mẫu nhấp nháy.
     */
    function renderUser() {
        const user = getUser();
        if (!user) return;

        document.querySelectorAll('[data-admin-name]').forEach(el => {
            el.textContent = user.fullName || user.email;
        });
        document.querySelectorAll('[data-admin-role]').forEach(el => {
            el.textContent = user.role === 'ADMIN' ? 'Quản trị viên' : 'Nhân viên';
        });
        // Ảnh tròn trên header (NiceAdmin: .nav-profile img)
        document.querySelectorAll('.header .nav-profile img').forEach(img => {
            setAvatarImg(img, user.avatarUrl);
        });
    }

    /**
     * Tô xanh mục sidebar ứng với trang đang mở.
     * NiceAdmin: link có class "collapsed" = bình thường, không có = đang chọn.
     * Trang con (vd booking-pending.html) muốn sáng mục cha thì khai báo ở link cha:
     *   data-pages="booking-pending.html booking-refunds.html"
     */
    function highlightSidebar() {
        const current = window.location.pathname.split('/').pop() || HOME_PAGE;
        let activeLink = null;

        document.querySelectorAll('#sidebar .nav-link').forEach(link => {
            const href  = link.getAttribute('href');
            const extra = (link.dataset.pages || '').split(/\s+/).filter(Boolean);
            const isActive = href === current || extra.includes(current);
            link.classList.toggle('collapsed', !isActive);
            if (isActive) activeLink = link;
        });

        keepSidebarScroll(activeLink);
    }

    /**
     * Mỗi lần chuyển trang là tải lại toàn bộ, sidebar bị cuộn về đầu.
     * Lưu vị trí cuộn trước khi rời trang, khôi phục ở trang mới.
     * Nếu mục đang chọn vẫn bị khuất (vd: gõ URL trực tiếp) thì cuộn tới giữa mục đó.
     */
    const SIDEBAR_SCROLL_KEY = 'admin_sidebar_scroll';

    function keepSidebarScroll(activeLink) {
        const sidebar = document.getElementById('sidebar');
        if (!sidebar) return;

        const saved = sessionStorage.getItem(SIDEBAR_SCROLL_KEY);
        if (saved !== null) sidebar.scrollTop = parseInt(saved, 10) || 0;

        if (activeLink) {
            const box  = sidebar.getBoundingClientRect();
            const item = activeLink.getBoundingClientRect();
            if (item.top < box.top || item.bottom > box.bottom) {
                sidebar.scrollTop += item.top - box.top - (box.height - item.height) / 2;
            }
        }

        window.addEventListener('pagehide', () => {
            sessionStorage.setItem(SIDEBAR_SCROLL_KEY, String(sidebar.scrollTop));
        });
    }

    /** Sau khi DOM sẵn sàng: header, sidebar, ẩn mục chỉ dành cho ADMIN, gắn nút đăng xuất */
    function initPage() {
        highlightSidebar();

        if (!getUser()) return;

        renderUser();   // phòng khi trang chưa gọi sớm ngay sau </header>

        // Phần tử có data-admin-only bị gỡ khỏi trang nếu không phải ADMIN
        if (!isAdmin()) {
            document.querySelectorAll('[data-admin-only]').forEach(el => el.remove());
        }

        document.querySelectorAll('[data-admin-logout]').forEach(el => {
            el.addEventListener('click', e => {
                e.preventDefault();
                logout();
            });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initPage);
    } else {
        initPage();
    }

    window.AdminAuth = {
        getToken, getUser, isLoggedIn, isAdmin,
        saveSession, updateUser, renderUser, logout, requireLogin,
        authHeaders, apiFetch
    };
})();