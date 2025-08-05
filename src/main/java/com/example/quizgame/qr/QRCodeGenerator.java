package com.example.quizgame.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Component
public class QRCodeGenerator {

    /**
     * Tạo QR code dạng Base64 PNG từ nội dung cho trước.
     * @param content nội dung cần mã hóa (ví dụ: mã PIN phòng)
     * @return chuỗi base64 để gán vào frontend: "data:image/png;base64,..."
     */
    public String generateBase64QRCode(String content) {
        try {
            int width = 200;
            int height = 200;

            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream pngOutput = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", pngOutput);

            String base64Image = Base64.getEncoder().encodeToString(pngOutput.toByteArray());
            return "data:image/png;base64," + base64Image;

        } catch (Exception e) {
            throw new RuntimeException("Không tạo được mã QR", e);
        }
    }
}
