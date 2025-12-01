package com.cinema.hub.backend.payment.vietqr;

import com.cinema.hub.backend.payment.util.PaymentException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Base64;
import java.util.Locale;

@Component
public class PaymentQrService {

    private static final String AID = "A000000727";
    private static final String BANK_BIN = "970422";
    private static final String ACCOUNT_NUMBER = "0931630902";
    private static final String ACCOUNT_NAME = "DAO NAM HAI";
    private static final String COUNTRY_CODE = "VN";
    private static final String CURRENCY_CODE = "704";
    private static final String CITY = "HA NOI";
    private static final String SERVICE_CODE = "QRIBFTTA";
    private static final int QR_SIZE = 600;

    public String generateVietQR(BigDecimal amount, String content) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Invalid amount");
        }
        String payload = buildEmvQr(amount, content);
        return toBase64Png(payload);
    }

    public String renderPayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            throw new PaymentException("Invalid QR payload");
        }
        return toBase64Png(payload);
    }

    private String buildEmvQr(BigDecimal amount, String content) {
        String normalizedContent = sanitizeContent(content);
        StringBuilder builder = new StringBuilder();
        builder.append(tag("00", "01")); // Payload Format Indicator
        builder.append(tag("01", "12")); // Dynamic QR
        builder.append(buildMerchantAccount());
        builder.append(tag("53", CURRENCY_CODE));
        builder.append(tag("54", formatAmount(amount)));
        builder.append(tag("58", COUNTRY_CODE));
        builder.append(tag("59", normalize(ACCOUNT_NAME)));
        builder.append(tag("60", CITY));
        builder.append(buildAdditionalDataField(normalizedContent));

        String raw = builder.toString() + "6304";
        return raw + crc16(raw);
    }

    private String buildMerchantAccount() {
        // Merchant account info template (ID 38) requires GUID + org-specific data + service code
        String bankInfo = tag("00", BANK_BIN);
        String accountInfo = tag("01", ACCOUNT_NUMBER);
        String orgSpecific = tag("01", bankInfo + accountInfo);
        String payload = tag("00", AID) + orgSpecific + tag("02", SERVICE_CODE);
        return tag("38", payload);
    }

    private String buildAdditionalDataField(String reference) {
        // Tag 62 (Additional Data Field Template) must contain nested TLV entries per EMV spec
        return tag("62", tag("01", reference)); // 01 = Bill Number / payment reference
    }

    private String formatAmount(BigDecimal amount) {
        BigDecimal normalized = amount.setScale(0, RoundingMode.HALF_UP);
        return normalized.stripTrailingZeros().toPlainString();
    }

    private String sanitizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            return "PAYMENT";
        }
        String normalized = normalize(content)
                .replaceAll("[^A-Z0-9\\- ]", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isEmpty()) {
            return "PAYMENT";
        }
        return normalized.length() > 60 ? normalized.substring(0, 60) : normalized;
    }

    private String tag(String id, String value) {
        return id + String.format("%02d", value.length()) + value;
    }

    private String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
    }

    private String toBase64Png(String payload) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(img, "png", os);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (WriterException | IOException e) {
            throw new PaymentException("QR generation failed", e);
        }
    }

    private String crc16(String input) {
        int polynomial = 0x1021;
        int crc = 0xFFFF;
        for (byte b : input.getBytes()) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ polynomial;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
