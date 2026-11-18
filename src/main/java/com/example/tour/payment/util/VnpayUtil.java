package com.example.tour.payment.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Ký / kiểm tra chữ ký VNPAY (v2.1.0) — làm đúng như code mẫu Java của VNPAY:
 *   1. Bỏ vnp_SecureHash, vnp_SecureHashType và các tham số rỗng
 *   2. Sắp xếp tăng dần theo tên tham số
 *   3. Nối "key=URLEncode(value, US-ASCII)" bằng "&"
 *   4. HMAC-SHA512 chuỗi đó với HashSecret → hex chữ thường
 * Lệch 1 bước (không sort, encode UTF-8...) là VNPAY báo "Sai chữ ký".
 */
public final class VnpayUtil {

    private VnpayUtil() {}

    public static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được chữ ký VNPAY", e);
        }
    }

    /** Chuỗi để ký — đồng thời cũng chính là query string gửi VNPAY */
    public static String buildHashData(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));
    }

    /** Kiểm tra tham số VNPAY gửi về (IPN / ReturnUrl) có đúng do VNPAY ký không */
    public static boolean verify(Map<String, String> allParams, String hashSecret) {
        String received = allParams.get("vnp_SecureHash");
        if (received == null || received.isBlank()) return false;

        Map<String, String> fields = new HashMap<>();
        allParams.forEach((k, v) -> {
            if (k.startsWith("vnp_") && !k.equals("vnp_SecureHash") && !k.equals("vnp_SecureHashType")) {
                fields.put(k, v);
            }
        });
        String expected = hmacSHA512(hashSecret, buildHashData(fields));
        // so sánh constant-time, không phân biệt hoa/thường
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                received.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }
}
