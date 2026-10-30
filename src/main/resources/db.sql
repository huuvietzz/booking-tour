-- Bảng người dùng
CREATE TABLE users (
   user_id INT PRIMARY KEY AUTO_INCREMENT,
   email VARCHAR(255) UNIQUE NOT NULL,
   password VARCHAR(255) NOT NULL,
   full_name VARCHAR(255) NOT NULL,
   address VARCHAR(255),
   phone_number VARCHAR(20) UNIQUE NOT NULL,
   date_of_birth DATE,
   gender ENUM('MALE', 'FEMALE', 'OTHER'),
   avatar_url VARCHAR(500),
   role ENUM('CUSTOMER', 'ADMIN', 'STAFF') DEFAULT 'CUSTOMER',
   is_active BOOLEAN DEFAULT TRUE,
   email_verified BOOLEAN DEFAULT FALSE,
   last_login TIMESTAMP NULL,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Bảng token (reset password, verify email)
CREATE TABLE tokens (
	 token_id INT PRIMARY KEY AUTO_INCREMENT,
	 user_id INT NOT NULL,
	 token VARCHAR(255) UNIQUE NOT NULL,
	 token_type ENUM('EMAIL_VERIFICATION', 'PASSWORD_RESET') NOT NULL,
	 expires_at TIMESTAMP NOT NULL,
	 used BOOLEAN DEFAULT FALSE,
	 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	 FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Bảng địa điểm tours
CREATE TABLE locations (
    location_id INT PRIMARY KEY AUTO_INCREMENT,
    location_name VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Bảng danh mục tour
CREATE TABLE categories (
    category_id INT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    image_url VARCHAR(500),
    display_order INT DEFAULT 1 UNIQUE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Bảng tour
CREATE TABLE tours (
   tour_id INT PRIMARY KEY AUTO_INCREMENT,
   category_id INT NOT NULL,
   location_id INT NOT NULL,
   tour_code VARCHAR(50) UNIQUE NOT NULL,
   tour_name VARCHAR(255) NOT NULL,
   tour_class ENUM('BUDGET', 'STANDARD', 'PREMIUM', 'GOOD_PRICE') DEFAULT 'STANDARD',
   departure_location VARCHAR(100) NOT NULL,
   duration VARCHAR(20) NOT NULL,
   base_price DECIMAL(15,0) NOT NULL,
   description TEXT,
   thumbnail_url VARCHAR(500),
   is_featured BOOLEAN DEFAULT FALSE,
   is_active BOOLEAN DEFAULT TRUE,
   view_count INT DEFAULT 0,
   average_rating DECIMAL(3,2) DEFAULT 0,
   total_reviews INT DEFAULT 0,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   FOREIGN KEY (category_id) REFERENCES categories(category_id),
   FOREIGN KEY (location_id) REFERENCES locations(location_id)
);

-- Bảng lịch trình tour theo ngày
CREATE TABLE itineraries (
	  itinerary_id INT PRIMARY KEY AUTO_INCREMENT,
	  tour_id INT NOT NULL,
	  day_number INT NOT NULL,
	  title VARCHAR(255) NOT NULL,
	  description TEXT, -- mô tả
	  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  FOREIGN KEY (tour_id) REFERENCES tours(tour_id) ON DELETE CASCADE,
	  UNIQUE KEY unique_tour_day (tour_id, day_number)
);

-- Bảng hình ảnh tour
CREATE TABLE images (
	 image_id INT PRIMARY KEY AUTO_INCREMENT,
	 tour_id INT NOT NULL,
	 image_url VARCHAR(500) NOT NULL,
	 display_order INT DEFAULT 0,
	 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	 FOREIGN KEY (tour_id) REFERENCES tours(tour_id) ON DELETE CASCADE
);

-- Bảng lịch khởi hành
CREATE TABLE schedules (
    schedule_id INT PRIMARY KEY AUTO_INCREMENT,
    tour_id INT NOT NULL,
    schedule_code VARCHAR(50) UNIQUE NOT NULL,
    departure_date DATE NOT NULL,
    return_date DATE NOT NULL,
    available_slots INT NOT NULL, 
    total_slots INT NOT NULL, 
    adult_price DECIMAL(15,0) NOT NULL,
    child_price DECIMAL(15,0) NOT NULL,
	toddler_price DECIMAL(15,0) NOT NULL,
    single_room_supplement DECIMAL(15,0) NOT NULL, 
    transport VARCHAR(100),
    status ENUM('AVAILABLE', 'FULL', 'CANCELLED') DEFAULT 'AVAILABLE', 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (tour_id) REFERENCES tours(tour_id) ON DELETE CASCADE,
    UNIQUE KEY unique_tour_departure (tour_id, departure_date)
);

-- Bảng khuyến mãi
CREATE TABLE promotions (
    promotion_id INT PRIMARY KEY AUTO_INCREMENT,
    promotion_code VARCHAR(50) UNIQUE NOT NULL,
	promotion_name VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    discount_type ENUM('PERCENT', 'FIXED') NOT NULL, 
    discount_value DECIMAL(15,0) NOT NULL,           
    max_discount_amount DECIMAL(15,0),               
    min_order_amount DECIMAL(15,0) DEFAULT 0,  
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
	quantity INT, 
	max_uses_per_user INT DEFAULT 1,
    used_count INT DEFAULT 0,
	is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Bang trung gian quan he n - n
CREATE TABLE promotion_tours (
    promotion_id INT NOT NULL,
    tour_id INT NOT NULL,
    PRIMARY KEY (promotion_id, tour_id),
    FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id) ON DELETE CASCADE,
    FOREIGN KEY (tour_id) REFERENCES tours(tour_id) ON DELETE CASCADE
);

-- Bảng đơn đặt tour
CREATE TABLE bookings (
    booking_id INT PRIMARY KEY AUTO_INCREMENT,
    booking_code VARCHAR(50) UNIQUE NOT NULL,
    user_id INT NULL,
    schedule_id INT NOT NULL, 
	promotion_id INT NULL,                   
	contact_name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    contact_address VARCHAR(255),
    total_adults INT NOT NULL DEFAULT 1,
    total_children INT DEFAULT 0,
	total_toddlers INT DEFAULT 0,
	temp_amount DECIMAL(15,0), 
    total_amount DECIMAL(15,0) NOT NULL,
    booking_status ENUM('PENDING', 'CONFIRMED', 'PAID', 'CANCELLED', 'COMPLETED') DEFAULT 'PENDING', 
	payment_status ENUM('PENDING', 'PAID', 'REFUNDED'),
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id),
    FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id) ON DELETE SET NULL
);

-- Bảng thông tin hành khách
CREATE TABLE participants (
    participant_id INT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender ENUM('MALE', 'FEMALE', 'OTHER'),
    participant_type ENUM('ADULT', 'CHILD', 'TODDLER') NOT NULL,
    phone_number VARCHAR(20),
    single_room BOOLEAN DEFAULT FALSE,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);

-- Bảng thanh toán
CREATE TABLE payments (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT NOT NULL,
	transaction_id VARCHAR(100),
    amount DECIMAL(15,0) NOT NULL,
    method ENUM('VNPAY','BANK_TRANSFER', 'CASH') NOT NULL,
	transaction_status ENUM('SUCCESS', 'FAILED', 'PENDING') DEFAULT 'PENDING',
    payment_date DATETIME(6),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE RESTRICT
);

-- Bảng bình luận, đánh giá
CREATE TABLE reviews (
    id INT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT NOT NULL UNIQUE,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
	admin_response TEXT,
    is_approved BOOLEAN DEFAULT FALSE,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE RESTRICT
);

-- Bảng tour yêu thích
CREATE TABLE favorites (
   favorite_id INT PRIMARY KEY AUTO_INCREMENT,
   user_id INT NOT NULL,
   tour_id INT NOT NULL,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
   FOREIGN KEY (tour_id) REFERENCES tours(tour_id) ON DELETE CASCADE,
   UNIQUE KEY unique_user_tour (user_id, tour_id)
);