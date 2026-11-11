/**
 * assets/js/chatbot.js — Trợ lý AI VietTrip (nút tròn góc dưới bên phải, giống Vietravel)
 *
 * Nhúng vào MỌI trang phía khách, trước </body> (sau bootstrap + auth-ui.js):
 *   <script src="assets/js/chatbot.js"></script>
 * File tự nạp assets/css/chatbot.css. Muốn nút hiện ngay không chờ, thêm vào <head> (không bắt buộc):
 *   <link href="assets/css/chatbot.css" rel="stylesheet">
 *
 * ---------------------------------------------------------------
 * CHAT_MOCK = true: trả lời giả lập, không cần BE (dùng khi BE chưa chạy).
 * CHAT_MOCK = false: gọi BE thật:
 *
 *   POST /api/public/chatbot
 *   body: { message: "Tour Đà Nẵng tháng 12?",
 *           history: [ { role: "user" | "assistant", content: "..." }, ... ] }   // tối đa 10 tin gần nhất
 *   → ApiResponse.data: {
 *         reply: "Câu trả lời (được dùng **đậm** và [link](tours.html))",
 *         tours: [ { tourId, tourName, basePrice, thumbnailUrl } ],   // không bắt buộc: thẻ tour gợi ý
 *         suggestions: [ "Câu hỏi gợi ý tiếp", ... ]                  // không bắt buộc
 *     }
 *   Đã đăng nhập thì gửi kèm Authorization (BE có thể cá nhân hoá), chưa đăng nhập vẫn chat được.
 *   API key của AI (OpenAI / Gemini...) CHỈ nằm ở BE — để ở FE ai mở F12 cũng lấy được.
 * ---------------------------------------------------------------
 */
(function () {
    'use strict';

    const CHAT_MOCK = false;   // true = trả lời giả lập (không cần BE)
    const API_URL = '/api/public/chatbot';
    const STORE_KEY = 'viettrip.chat';        // sessionStorage: chuyển trang vẫn giữ cuộc trò chuyện, đóng tab thì mất
    const MAX_LEN = 500;
    const HISTORY_SENT = 10;

    const GREETING = 'Xin chào! Mình là **trợ lý VietTrip** 👋\nMình có thể giúp bạn tìm tour, xem lịch khởi hành, mã khuyến mãi hoặc hướng dẫn đặt tour.';
    const START_CHIPS = ['Tour đang khuyến mãi', 'Tour Đà Nẵng', 'Cách đặt tour', 'Chính sách huỷ tour'];

    // Tự nạp CSS cạnh file JS: assets/js/chatbot.js → assets/css/chatbot.css
    // Trang nào đã tự thêm <link ... chatbot.css> trong <head> thì dùng luôn cái đó.
    const scriptSrc = document.currentScript && document.currentScript.src;
    let cssLink = document.querySelector('link[href*="chatbot.css"]');
    if (!cssLink && scriptSrc) {
        cssLink = document.createElement('link');
        cssLink.rel = 'stylesheet';
        cssLink.href = scriptSrc.replace(/js\/chatbot\.js(\?.*)?$/, 'css/chatbot.css');
        document.head.appendChild(cssLink);
    }

    // ----- Tiện ích -----
    const escapeHtml = s => String(s ?? '').replace(/[&<>"']/g, c =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    const fmtMoney = v => new Intl.NumberFormat('vi-VN').format(Number(v) || 0) + ' ₫';

    /**
     * Câu trả lời của AI là CHỮ TỰ DO → không bao giờ innerHTML thẳng (AI có thể bị dụ in ra <script>).
     * Escape hết trước, rồi chỉ mở lại 2 thứ an toàn: **đậm** và [chữ](link) với link nội bộ / http(s).
     */
    function renderText(text) {
        let html = escapeHtml(text);
        html = html.replace(/\*\*(.+?)\*\*/g, '<b>$1</b>');
        html = html.replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, (m, label, url) => {
            const raw = url.replace(/&amp;/g, '&');
            // http(s) hoặc đường dẫn nội bộ (không có ":" → chặn được javascript:, data:...)
            const ok = /^https?:\/\//i.test(raw) || /^[\w\-./#?=&%]+$/.test(raw);
            if (!ok) return label;
            // Link tuyệt đối nhưng cùng trang web (http://localhost:8080/user/...) vẫn mở ngay tab hiện tại
            let ext = false;
            try { ext = new URL(raw, location.href).origin !== location.origin; } catch (_) { /* giữ false */ }
            return `<a href="${escapeHtml(raw)}"${ext ? ' target="_blank" rel="noopener"' : ''}>${label}</a>`;
        });
        return html;
    }

    // ----- Lưu cuộc trò chuyện -----
    let messages = load();   // [{ role: 'user'|'assistant', content, tours?, suggestions? }]

    function load() {
        try { return JSON.parse(sessionStorage.getItem(STORE_KEY)) || []; } catch (_) { return []; }
    }
    function save() {
        try { sessionStorage.setItem(STORE_KEY, JSON.stringify(messages.slice(-40))); } catch (_) { /* ẩn danh: bỏ qua */ }
    }

    // ----- Dựng giao diện -----
    const root = document.createElement('div');
    root.className = 'cb-root';
    root.innerHTML = `
        <button type="button" class="cb-launcher" aria-label="Mở trợ lý VietTrip" aria-expanded="false" aria-controls="cbPanel">
            <i class="bi bi-robot"></i>
        </button>
        <div class="cb-panel" id="cbPanel" role="dialog" aria-label="Trợ lý VietTrip" aria-hidden="true">
            <div class="cb-head">
                <div class="cb-head-avatar"><i class="bi bi-robot"></i></div>
                <div class="flex-grow-1">
                    <div class="cb-head-title">Trợ lý VietTrip</div>
                    <div class="cb-head-status">Trực tuyến</div>
                </div>
                <button type="button" class="cb-head-btn" data-cb-close title="Thu nhỏ" aria-label="Đóng trợ lý">
                    <i class="bi bi-chevron-down"></i></button>
            </div>
            <div class="cb-body" aria-live="polite"></div>
            <div class="cb-foot">
                <form class="cb-form">
                    <textarea class="cb-input" rows="1" maxlength="${MAX_LEN}" placeholder="Nhập câu hỏi của bạn..." aria-label="Nhập câu hỏi"></textarea>
                    <button type="submit" class="cb-send" aria-label="Gửi" disabled><i class="bi bi-send-fill"></i></button>
                </form>
            </div>
        </div>`;
    /* CSS nạp bằng JS là bất đồng bộ: nếu chèn khung chat ngay thì trong lúc chờ CSS, khung chat hiện ra
       dạng chưa có style (bung hết) rồi mới thu về nút tròn → nháy. Ẩn cả cụm đến khi CSS tải xong. */
    root.style.visibility = 'hidden';
    document.body.appendChild(root);
    const launcherEl = root.querySelector('.cb-launcher');
    // CSS đã áp dụng ⇔ nút tròn đã thành position: fixed (link.sheet không tin được: Chrome tạo sheet rỗng khi đang tải)
    const cssReady = () => getComputedStyle(launcherEl).position === 'fixed';
    const reveal = () => { root.style.visibility = ''; };
    if (!cssLink || cssReady()) {
        reveal();
    } else {
        cssLink.addEventListener('load', reveal, { once: true });
        cssLink.addEventListener('error', reveal, { once: true });   // CSS lỗi vẫn hiện, còn hơn mất hẳn nút chat
        setTimeout(reveal, 3000);                                     // phòng sự kiện load không bắn
    }

    const launcher = root.querySelector('.cb-launcher');
    const panel = root.querySelector('.cb-panel');
    const body = root.querySelector('.cb-body');
    const form = root.querySelector('.cb-form');
    const input = root.querySelector('.cb-input');
    const sendBtn = root.querySelector('.cb-send');
    let busy = false;

    // ----- Mở / đóng -----
    function open() {
        hideHello();
        document.body.classList.add('cb-open');
        launcher.setAttribute('aria-expanded', 'true');
        panel.setAttribute('aria-hidden', 'false');
        if (!body.childElementCount) renderAll();
        scrollBottom();
        setTimeout(() => input.focus(), 230);
    }
    function close() {
        document.body.classList.remove('cb-open');
        launcher.setAttribute('aria-expanded', 'false');
        panel.setAttribute('aria-hidden', 'true');
        launcher.focus();
    }
    launcher.addEventListener('click', open);
    root.querySelector('[data-cb-close]').addEventListener('click', close);
    document.addEventListener('keydown', e => {
        if (e.key === 'Escape' && document.body.classList.contains('cb-open')) close();
    });
    // ----- Vẽ tin nhắn -----
    function scrollBottom() { body.scrollTop = body.scrollHeight; }

    function addBubble(role, content, extraClass = '') {
        const row = document.createElement('div');
        row.className = `cb-msg ${role === 'user' ? 'user' : 'bot'} ${extraClass}`;
        row.innerHTML = (role === 'user' ? '' : '<div class="cb-msg-avatar"><i class="bi bi-robot"></i></div>')
            + `<div class="cb-bubble">${renderText(content)}</div>`;
        body.appendChild(row);
        return row;
    }

    function addTours(tours) {
        if (!tours || !tours.length) return;
        const box = document.createElement('div');
        box.className = 'cb-tours';
        box.innerHTML = tours.slice(0, 3).map(t => `
            <a class="cb-tour" href="tour-details.html?id=${encodeURIComponent(t.tourId)}">
                ${t.thumbnailUrl ? `<img class="cb-tour-img" src="${escapeHtml(t.thumbnailUrl)}" alt="" loading="lazy">` : '<span class="cb-tour-img"></span>'}
                <span>
                    <span class="cb-tour-name">${escapeHtml(t.tourName)}</span>
                    <span class="cb-tour-price">Từ ${fmtMoney(t.basePrice)}</span>
                </span>
            </a>`).join('');
        body.appendChild(box);
    }

    function addChips(list) {
        body.querySelectorAll('.cb-chips').forEach(c => c.remove());   // chỉ giữ gợi ý của câu trả lời mới nhất
        if (!list || !list.length) return;
        const box = document.createElement('div');
        box.className = 'cb-chips';
        list.slice(0, 4).forEach(text => {
            const b = document.createElement('button');
            b.type = 'button';
            b.className = 'cb-chip';
            b.textContent = text;
            b.addEventListener('click', () => send(text));
            box.appendChild(b);
        });
        body.appendChild(box);
    }

    function renderAll() {
        body.innerHTML = '';
        addBubble('assistant', GREETING);
        messages.forEach(m => {
            addBubble(m.role, m.content);
            if (m.role === 'assistant') addTours(m.tours);
        });
        const last = [...messages].reverse().find(m => m.role === 'assistant');
        addChips(last ? last.suggestions : START_CHIPS);
        scrollBottom();
    }

    // ----- Gửi -----
    function autoGrow() {
        input.style.height = 'auto';
        input.style.height = Math.min(input.scrollHeight, 110) + 'px';
        sendBtn.disabled = busy || !input.value.trim();
    }
    input.addEventListener('input', autoGrow);
    input.addEventListener('keydown', e => {
        // Enter gửi, Shift+Enter xuống dòng. isComposing: đang gõ tiếng Việt bằng bộ gõ thì Enter là chốt chữ, không gửi
        if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
            e.preventDefault();
            form.requestSubmit();
        }
    });
    form.addEventListener('submit', e => {
        e.preventDefault();
        send(input.value);
    });

    async function send(text) {
        text = String(text || '').trim().slice(0, MAX_LEN);
        if (!text || busy) return;
        busy = true;
        input.value = '';
        autoGrow();

        body.querySelectorAll('.cb-chips, .cb-msg.error').forEach(el => el.remove());
        const history = messages.slice(-HISTORY_SENT).map(m => ({ role: m.role, content: m.content }));
        messages.push({ role: 'user', content: text });
        save();
        addBubble('user', text);

        const typing = document.createElement('div');
        typing.className = 'cb-msg bot cb-typing';
        typing.innerHTML = '<div class="cb-msg-avatar"><i class="bi bi-robot"></i></div><div class="cb-bubble"><span></span><span></span><span></span></div>';
        body.appendChild(typing);
        scrollBottom();

        try {
            const data = CHAT_MOCK ? await mockReply(text) : await callApi(text, history);
            typing.remove();
            const msg = { role: 'assistant', content: data.reply || '', tours: data.tours || [], suggestions: data.suggestions || [] };
            messages.push(msg);
            save();
            addBubble('assistant', msg.content);
            addTours(msg.tours);
            addChips(msg.suggestions);
        } catch (err) {
            typing.remove();
            // Câu hỏi đã gửi không thành công → bỏ khỏi lịch sử, cho bấm thử lại
            messages.pop();
            save();
            const row = addBubble('assistant', err.message || 'Có lỗi xảy ra, bạn thử lại nhé.', 'error');
            const retry = document.createElement('button');
            retry.type = 'button';
            retry.className = 'cb-retry';
            retry.textContent = 'Gửi lại';
            retry.addEventListener('click', () => {
                row.previousElementSibling?.remove();   // bóng chat câu hỏi cũ
                row.remove();
                send(text);
            });
            row.querySelector('.cb-bubble').appendChild(document.createElement('br'));
            row.querySelector('.cb-bubble').appendChild(retry);
        } finally {
            busy = false;
            autoGrow();
            scrollBottom();
        }
    }

    async function callApi(message, history) {
        const headers = { 'Content-Type': 'application/json' };
        const token = localStorage.getItem('token');
        if (token) headers.Authorization = 'Bearer ' + token;

        let res;
        try {
            res = await fetch(API_URL, { method: 'POST', headers, body: JSON.stringify({ message, history }) });
        } catch (_) {
            throw new Error('Không kết nối được tới máy chủ.');
        }
        const json = await res.json().catch(() => null);
        if (res.status === 429) throw new Error('Bạn gửi hơi nhanh, đợi vài giây rồi thử lại nhé.');
        if (!res.ok || !json || !json.success) throw new Error((json && json.message) || 'Trợ lý đang bận, bạn thử lại sau nhé.');
        return json.data;
    }

    // ----- Trả lời giả lập (xoá khi có BE) -----
    const MOCK_TOURS = {
        'da nang': [
            { tourId: 2, tourName: 'Đà Nẵng - Hội An - Bà Nà Hills 4N3Đ', basePrice: 6290000 },
            { tourId: 9, tourName: 'Đà Nẵng - Huế - Động Phong Nha 4N3Đ', basePrice: 5890000 }
        ],
        'phu quoc': [{ tourId: 3, tourName: 'Phú Quốc - Grand World - Hòn Thơm 3N2Đ', basePrice: 5490000 }],
        'sa pa': [{ tourId: 1, tourName: 'Hà Nội - Sa Pa - Fansipan 3N2Đ', basePrice: 4590000 }],
        'ha long': [{ tourId: 4, tourName: 'Hạ Long - Du thuyền 5 sao 2N1Đ', basePrice: 3890000 }]
    };
    const noAccent = s => s.normalize('NFD').replace(/\p{M}/gu, '').replace(/đ/gi, 'd').toLowerCase();

    async function mockReply(text) {
        await new Promise(r => setTimeout(r, 700 + Math.random() * 600));
        const q = noAccent(text);
        const place = Object.keys(MOCK_TOURS).find(k => q.includes(k) || q.includes(k.replace(' ', '')));
        if (place) {
            return {
                reply: `Đây là các tour **${MOCK_TOURS[place][0].tourName.split(' - ')[0]}** đang mở bán. Bấm vào tour để xem lịch khởi hành và giá chi tiết nhé!`,
                tours: MOCK_TOURS[place],
                suggestions: ['Tour này có lịch tháng sau không?', 'Có mã giảm giá không?']
            };
        }
        if (/khuyen mai|giam gia|ma |voucher|uu dai/.test(q)) {
            return {
                reply: 'Các mã đang áp dụng nằm ở mục [Mã khuyến mãi](profile.html#promo) trong tài khoản và phần **Ưu đãi** trên trang chủ. Bạn sao chép mã rồi nhập ở bước đặt tour là được giảm ngay.',
                suggestions: ['Cách đặt tour', 'Tour Phú Quốc']
            };
        }
        if (/dat tour|cach dat|book/.test(q)) {
            return {
                reply: 'Đặt tour chỉ 3 bước:\n1. Chọn tour ở trang [Tours](tours.html) rồi chọn **ngày khởi hành**.\n2. Nhập thông tin liên hệ và hành khách.\n3. Kiểm tra lại và bấm **Đặt tour**.\nĐơn đã đặt xem ở mục [Đơn đặt tour](profile.html#bookings).',
                suggestions: ['Chính sách huỷ tour', 'Tour đang khuyến mãi']
            };
        }
        if (/huy|hoan tien|doi lich/.test(q)) {
            return {
                reply: 'Đơn **Chờ xác nhận** bạn tự huỷ được trong mục [Đơn đặt tour](profile.html#bookings). Đơn đã thanh toán thì phí huỷ tuỳ thời điểm, chi tiết ở [Điều khoản dịch vụ](terms.html) hoặc gọi hotline **+84 123 456 789**.',
                suggestions: ['Cách đặt tour']
            };
        }
        if (/xin chao|hello|^hi\b|chao/.test(q)) {
            return { reply: 'Chào bạn! Bạn muốn đi đâu? Mình gợi ý tour phù hợp cho bạn nhé 😊', suggestions: START_CHIPS };
        }
        return {
            reply: 'Mình chưa hiểu rõ câu hỏi 😅 Bạn thử hỏi về **điểm đến** (Đà Nẵng, Phú Quốc, Sa Pa, Hạ Long...), **khuyến mãi** hoặc **cách đặt tour** nhé. Cần gấp thì gọi hotline **+84 123 456 789**.',
            suggestions: START_CHIPS
        };
    }

    // ----- Thẻ chào khi rê chuột vào nút tròn (giống Vietravel) -----
    const hello = document.createElement('div');
    hello.className = 'cb-hello';
    hello.setAttribute('role', 'tooltip');
    hello.innerHTML = `
        <button type="button" class="cb-hello-close" aria-label="Ẩn lời chào"><i class="bi bi-x-lg"></i></button>
        <div class="cb-hello-title">Chào bạn!</div>
        <div class="cb-hello-text">Bạn muốn đi đâu? Mình giúp bạn nhé.</div>`;
    root.appendChild(hello);

    let helloTimer = null;
    function showHello() {
        if (document.body.classList.contains('cb-open')) return;
        clearTimeout(helloTimer);
        hello.classList.add('show');
    }
    // Trễ một chút: rê chuột từ nút sang thẻ (để bấm ×) không bị tắt giữa đường
    function hideHelloSoon() {
        clearTimeout(helloTimer);
        helloTimer = setTimeout(hideHello, 200);
    }
    function hideHello() {
        clearTimeout(helloTimer);
        hello.classList.remove('show');
    }

    // Chỉ máy có chuột mới có "rê chuột"; điện thoại bấm là mở chat luôn
    if (window.matchMedia('(hover: hover)').matches) {
        launcher.addEventListener('mouseenter', showHello);
        launcher.addEventListener('mouseleave', hideHelloSoon);
        hello.addEventListener('mouseenter', showHello);
        hello.addEventListener('mouseleave', hideHelloSoon);
    }
    launcher.addEventListener('focus', showHello);   // dùng phím Tab cũng thấy lời chào
    launcher.addEventListener('blur', hideHelloSoon);
    hello.addEventListener('click', e => {
        if (e.target.closest('.cb-hello-close')) hideHello();
        else open();
    });
})();