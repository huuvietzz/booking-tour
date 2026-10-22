/**
 * assets/js/auth-ui.js
 *
 * Hiển thị trạng thái đăng nhập trên header.
 * - Chưa đăng nhập: giữ nguyên nút "Đăng nhập"
 * - Đã đăng nhập:  thay bằng avatar tròn (ảnh đại diện, chưa có thì hình người mặc định) + tên
 *
 * Nhúng vào MỌI trang có header, đặt sau main.js:
 *   <script src="assets/js/auth-ui.js"></script>
 */
(function () {
    'use strict';

    const TOKEN_KEY = 'token';

    /** Avatar mặc định kiểu Facebook: đầu + vai xám, khung tròn cắt phần thừa */
    const DEFAULT_AVATAR_SVG =
        '<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" ' +
        'style="width:100%;height:100%;display:block" aria-hidden="true">' +
        '<rect width="100" height="100" fill="#e4e6eb"/>' +
        '<circle cx="50" cy="38" r="18" fill="#bcc0c4"/>' +
        '<ellipse cx="50" cy="94" rx="33" ry="27" fill="#bcc0c4"/>' +
        '</svg>';

    /** HTML bên trong ô avatar: ảnh thật nếu có, không thì hình người rỗng */
    function avatarInnerHtml(avatarUrl) {
        if (avatarUrl) {
            const img = document.createElement('img');
            img.src = avatarUrl;
            img.alt = '';
            img.style.cssText = 'width:100%;height:100%;border-radius:50%;object-fit:cover;display:block';
            return img.outerHTML;
        }
        return DEFAULT_AVATAR_SVG;
    }

    function getCurrentUser() {
        const token = localStorage.getItem(TOKEN_KEY);
        if (!token) return null;

        return {
            token:    token,
            userId:   localStorage.getItem('userId'),
            email:    localStorage.getItem('email')    || '',
            fullName: localStorage.getItem('fullName') || '',
            role:     localStorage.getItem('role')     || 'CUSTOMER',
            avatarUrl: localStorage.getItem('avatarUrl') || ''
        };
    }

    /** Lấy chữ cái đầu của email, viết hoa. VD: "viet@gmail.com" -> "V" */
    function getAvatarLetter(email) {
        const ch = (email || '').trim().charAt(0);
        return ch ? ch.toUpperCase() : '?';
    }

    /** Tên dài thì cắt bớt để không phá vỡ layout header */
    function shortenName(fullName, maxLength) {
        if (!fullName) return 'Tài khoản';
        if (fullName.length <= maxLength) return fullName;
        return fullName.substring(0, maxLength).trim() + '…';
    }

    function renderLoggedIn(anchor, user) {
        anchor.href = 'profile.html';
        anchor.classList.add('btn-user');
        anchor.title = user.fullName + ' (' + user.email + ')';

        const avatar = document.createElement('span');
        avatar.className = 'user-avatar';
        avatar.innerHTML = avatarInnerHtml(user.avatarUrl);   // URL/SVG do mình sinh, không phải text user gõ

        // Ảnh hỏng (file bị xoá, URL sai) → quay về hình người rỗng thay vì icon ảnh vỡ
        const img = avatar.querySelector('img');
        if (img) img.addEventListener('error', () => { avatar.innerHTML = DEFAULT_AVATAR_SVG; });

        const name = document.createElement('span');
        name.className = 'user-name';
        name.textContent = shortenName(user.fullName, 10);   // textContent: không bao giờ chạy HTML

        anchor.replaceChildren(avatar, name);
    }

    function init() {
        const anchor = document.querySelector('.btn-getstarted');
        if (!anchor) return;

        const user = getCurrentUser();
        if (user) renderLoggedIn(anchor, user);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // Cho các trang khác dùng lại
    window.AuthUI = {
        getCurrentUser: getCurrentUser,
        getAvatarLetter: getAvatarLetter,
        avatarInnerHtml: avatarInnerHtml,

        /** Xoá phiên đăng nhập rồi quay về trang chủ */
        logout: function () {
            ['token', 'userId', 'email', 'fullName', 'role', 'avatarUrl']
                .forEach(k => localStorage.removeItem(k));
            window.location.href = 'index.html';
        },

        /** Gọi ở đầu các trang bắt buộc đăng nhập (profile.html, my-bookings.html...) */
        requireLogin: function () {
            if (!localStorage.getItem(TOKEN_KEY)) {
                window.location.href = 'pages-login.html';
                return null;
            }
            return getCurrentUser();
        },

        /** Header Authorization cho các lời gọi API cần đăng nhập */
        authHeaders: function () {
            const token = localStorage.getItem(TOKEN_KEY);
            return token
                ? { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token }
                : { 'Content-Type': 'application/json' };
        }
    };
})();