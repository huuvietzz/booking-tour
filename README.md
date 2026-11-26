# VietTrip – Hệ thống đặt tour du lịch

Website đặt tour du lịch trực tuyến cho công ty lữ hành **VietTrip**, gồm 2 phần:

- **Trang khách hàng:** tìm và xem tour, đặt chỗ, thanh toán online, đánh giá tour, hỏi đáp với chatbot AI.
- **Trang quản trị:** nhân viên và quản lý điều hành tour, lịch khởi hành, đơn đặt, thanh toán, khuyến mãi, khách hàng.

- **Backend:** Java 17 + Spring Boot 3.5
- **Frontend:** HTML/CSS/JavaScript (Bootstrap 5)

---

## Mục lục

- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Chức năng](#chức-năng)
- [Điểm kỹ thuật nổi bật](#điểm-kỹ-thuật-nổi-bật)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Cài đặt và chạy](#cài-đặt-và-chạy)
- [Tài liệu API](#tài-liệu-api)
- [Hình ảnh](#hình-ảnh)

---

## Công nghệ sử dụng

- **Backend:** Java 17, Spring Boot 3.5, Spring Security + JWT, Spring Data JPA
- **Database / Cache:** MySQL 8.0, Redis
- **Tích hợp:** VNPAY, SePay, Spring AI (Groq), Spring Mail
- **Xuất file:** EasyExcel, iText (PDF), ZXing (QR)
- **Frontend:** HTML, CSS, JavaScript, Bootstrap 5
- **Công cụ:** Maven, Docker, Swagger

---

## Chức năng

### Khách hàng

- Đăng nhập, đăng ký
- Quản lý tài khoản
- Tìm kiếm tour theo nhiều tiêu chí: giá, điểm đến, ngày khởi hành,...
- Quản lý tour yêu thích
- Đặt tour
- Tích hợp thanh toán online qua VNPAY và SePay
- Xem đơn đặt tour
- Đánh giá, bình luận tour
- Tích hợp Chatbot AI hỗ trợ thông tin về tour: giá, lịch khởi hành,...

### Quản trị

#### ADMIN

- Quản lý đặt tour
- Quản lý thanh toán
- Quản lý khuyến mãi
- Quản lý tour
- Quản lý lịch khởi hành
- Quản lý điểm đến
- Quản lý danh mục
- Quản lý khách hàng
- Quản lý đánh giá
- Quản lý nhân viên

#### STAFF

- Quản lý đặt tour
- Quản lý thanh toán
- Quản lý khuyến mãi
- Quản lý tour
- Quản lý lịch khởi hành
- Quản lý điểm đến
- Quản lý danh mục
- Quản lý khách hàng
- Quản lý đánh giá

---

## Điểm kỹ thuật nổi bật

- **Chống đặt trùng chỗ:** nhiều người tranh chỗ cuối thì chỉ 1 người đặt được.
- **Thanh toán an toàn:** xác thực webhook SePay và chữ ký VNPAY, đối chiếu số tiền và mã đơn.
- **Tự huỷ đơn quá hạn:** đơn chưa thanh toán sau 1 giờ tự huỷ và trả lại chỗ.
- **Cache Redis:** cache danh mục, điểm đến, chi tiết tour; tự xoá khi dữ liệu thay đổi.

---

## Cấu trúc thư mục

Mã nguồn chia theo **module nghiệp vụ** (package-by-feature):

```
src/main/java/com/example/tour
├── booking/      # Đặt tour, giữ chỗ, tự huỷ đơn quá hạn
├── payment/      # SePay, VNPAY, lịch sử thanh toán
├── promotion/    # Mã khuyến mãi
├── review/       # Đánh giá tour
├── tour/         # Tour, lịch khởi hành, danh mục, điểm đến
├── user/         # Tài khoản, xác thực, yêu thích
├── chatbot/      # Chatbot AI (Spring AI + Groq)
└── common/       # Cấu hình, bảo mật, exception, tiện ích dùng chung
```

Mỗi module gồm các package `controller`, `service`, `repository`, `entity`, `dto`, `constant`.

```
src/main/resources/static
├── user/         # Trang khách hàng
└── admin/        # Trang quản trị
```

---

## Cài đặt và chạy

### Yêu cầu

- JDK 17
- Maven 3.9+ (hoặc dùng `mvnw` đi kèm)
- MySQL 8.0
- Docker (để chạy Redis)

### Các bước

**1. Clone dự án**

```bash
git clone https://github.com/huuvietzz/booking-tour.git
cd booking-tour
```

**2. Tạo database**

```sql
CREATE DATABASE booking_tour;
```

**3. Chạy Redis bằng Docker**

```bash
docker run -d --name viettrip-redis -p 6379:6379 --restart unless-stopped redis:latest
```

---

## Tài liệu API

Swagger UI tại `/swagger-ui.html`

---

## Hình ảnh

> _Thêm ảnh chụp màn hình vào thư mục `docs/images/` rồi cập nhật đường dẫn bên dưới._

| Trang chủ | Chi tiết tour |
|---|---|
| ![Trang chủ](docs/images/home.png) | ![Chi tiết tour](docs/images/tour-detail.png) |

| Đặt tour & thanh toán | Trang quản trị |
|---|---|
| ![Đặt tour](docs/images/booking.png) | ![Quản trị](docs/images/admin.png) |
