package com.example.tour.common.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/** Tạo ảnh mã QR (PNG) bằng ZXing — dùng cho email đặt tour, sau này dùng lại cho phiếu PDF */
public final class QrCodeGenerator {

    private QrCodeGenerator() {}

    /**
     * @param content nội dung mã hoá (thường là 1 đường link)
     * @param size    cạnh ảnh (px)
     */
    public static byte[] png(String content, int size) throws WriterException, IOException {
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, Map.of(
                EncodeHintType.CHARACTER_SET, "UTF-8",
                EncodeHintType.MARGIN, 1,                                   // viền trắng mỏng
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));  // quét được dù ảnh hơi mờ / bị nén
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }
}
