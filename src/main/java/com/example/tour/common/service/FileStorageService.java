package com.example.tour.common.service;

import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private static final long   MAX_AVATAR_SIZE   = 2 * 1024 * 1024;
    private static final long   MAX_IMAGE_SIZE    = 5 * 1024 * 1024;   // ảnh danh mục thường lớn hơn avatar
    private static final String URL_PREFIX        = "/avatars/";
    private static final String UPLOAD_URL_PREFIX = "/uploads/";

    private final Path avatarDir;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.avatarDir = Paths.get(uploadDir.strip(), "avatars").toAbsolutePath().normalize();
        try {
            Files.createDirectories(avatarDir);
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được thư mục upload: " + avatarDir, e);
        }
    }

    // =====================================================================
    // AVATAR: /avatars/uuid.ext
    // =====================================================================

    /** Lưu avatar, trả về URL công khai dạng /avatars/uuid.jpg */
    public String storeAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        try {
            String ext = readImageExt(file);

            // KHÔNG dùng tên file gốc: tránh trùng tên và tấn công "../../"
            String filename = UUID.randomUUID() + "." + ext;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, avatarDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
            return URL_PREFIX + filename;
        } catch (IOException e) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /** Xoá avatar theo URL đã lưu trong DB. Lỗi thì chỉ log, không làm hỏng request. */
    public void deleteAvatar(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) return;

        Path file = avatarDir.resolve(url.substring(URL_PREFIX.length())).normalize();
        if (!file.startsWith(avatarDir)) return;   // chặn đường dẫn trỏ ra ngoài thư mục

        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Không xoá được avatar cũ: {}", file, e);
        }
    }

    // =====================================================================
    // ẢNH KHÁC (danh mục, tour, ...): /uploads/{folder}/uuid.ext
    // =====================================================================

    /**
     * Lưu ảnh vào uploads/{folder}/uuid.ext, trả URL "/uploads/{folder}/uuid.ext".
     * folder: "categories", "tours", ... (do code truyền vào, KHÔNG nhận từ client)
     */
    public String storeImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        try {
            String ext = readImageExt(file);

            Path dir = uploadRoot().resolve(folder).normalize();
            Files.createDirectories(dir);

            String filename = UUID.randomUUID() + "." + ext;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
            return UPLOAD_URL_PREFIX + folder + "/" + filename;
        } catch (IOException e) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /** Xoá file theo URL "/uploads/...". Lỗi thì chỉ log. */
    public void deleteImage(String url) {
        if (url == null || !url.startsWith(UPLOAD_URL_PREFIX)) return;

        Path root = uploadRoot();
        Path file = root.resolve(url.substring(UPLOAD_URL_PREFIX.length())).normalize();
        if (!file.startsWith(root)) return;   // chặn "../"

        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Không xoá được ảnh cũ: {}", file, e);
        }
    }

    /**
     * Xoá file SAU KHI biết kết quả transaction (dùng cho ảnh /uploads/...):
     * commit → xoá ảnh cũ; rollback → xoá ảnh mới vừa lưu.
     */
    public void cleanupAfterTransaction(String oldUrl, String newUrl) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                deleteImage(status == STATUS_COMMITTED ? oldUrl : newUrl);
            }
        });
    }

    /**
     * Bản cho nhiều file. Danh sách được ĐỌC LÚC KẾT THÚC transaction,
     * nên có thể đăng ký trước rồi thêm URL dần trong lúc lưu file.
     * commit → xoá deleteOnCommit; rollback → xoá deleteOnRollback.
     */
    public void cleanupAfterTransaction(Collection<String> deleteOnCommit, Collection<String> deleteOnRollback) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                (status == STATUS_COMMITTED ? deleteOnCommit : deleteOnRollback).forEach(url -> deleteImage(url));
            }
        });
    }

    // =====================================================================
    // TIỆN ÍCH
    // =====================================================================

    private Path uploadRoot() {
        return avatarDir.getParent();   // = thư mục "uploads"
    }

    /**
     * Đọc 12 byte đầu để biết định dạng THẬT của file, không tin Content-Type / đuôi file
     * (cả hai đều do client tự khai, giả mạo được). Không phải ảnh hợp lệ → INVALID_FILE_TYPE.
     */
    private String readImageExt(MultipartFile file) throws IOException {
        String ext;
        try (InputStream in = file.getInputStream()) {
            ext = detectImageExt(in.readNBytes(12));
        }
        if (ext == null) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }
        return ext;
    }

    /** Trả về "jpg" / "png" / "webp" theo magic bytes, không phải ảnh hợp lệ thì null */
    private String detectImageExt(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (h.length >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
            return "png";
        }
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return "webp";
        }
        return null;
    }
}