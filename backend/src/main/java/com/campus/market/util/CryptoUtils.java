package com.campus.market.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密工具类
 *
 * 1. AES-GCM 加密：
 *    - 用于手机号等敏感字段的加密存储
 *    - 每次加密生成随机 12 字节 IV（初始化向量）
 *    - 密文格式：Base64(IV(12B) || Ciphertext || Tag(16B))
 *    - 解密时从密文头部提取 IV，再解密
 *
 * 2. HMAC-SHA256 盲索引：
 *    - 用于手机号等值查询（WHERE phone_hash = ?）
 *    - 不可逆，无法从 hash 反推手机号
 *    - 同一手机号始终生成相同的 hash（确定性）
 *
 * 安全设计：手机号在 DB 中存储为 phone（AES-GCM密文）+ phone_hash（HMAC哈希）
 * - 查询时用 phone_hash 做等值匹配
 * - 展示时用 AES-GCM 解密 phone 字段
 */
@Slf4j
@Component
public class CryptoUtils {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int GCM_IV_LENGTH = 12;   // GCM 推荐 12 字节 IV
    private static final int GCM_TAG_LENGTH = 128; // 认证标签长度（位）

    @Value("${campus.market.crypto.aes-key}")
    private String aesKeyBase64;

    @Value("${campus.market.crypto.hmac-key}")
    private String hmacKey;

    private SecretKey aesKey;
    private SecretKey hmacSecretKey;
    private SecureRandom secureRandom;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(aesKeyBase64);
        this.aesKey = new SecretKeySpec(keyBytes, "AES");
        this.hmacSecretKey = new SecretKeySpec(hmacKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        this.secureRandom = new SecureRandom();
        log.info("[CryptoUtils] 加密组件初始化完成");
    }

    // ================== AES-GCM 加解密 ==================

    /**
     * AES-GCM 加密
     * @param plaintext 明文
     * @return Base64 编码的密文（含 IV）
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        try {
            // 1. 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // 2. 初始化加密器
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

            // 3. 加密
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 4. 拼接 IV + 密文，Base64 编码
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("[CryptoUtils] AES-GCM 加密失败", e);
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * AES-GCM 解密
     * @param encryptedBase64 Base64 编码的密文（含 IV）
     * @return 明文
     */
    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return null;
        }
        try {
            // 1. Base64 解码
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);

            // 2. 提取 IV（前 12 字节）和密文
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            // 3. 解密
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[CryptoUtils] AES-GCM 解密失败", e);
            throw new RuntimeException("解密失败", e);
        }
    }

    // ================== HMAC-SHA256 盲索引 ==================

    /**
     * 生成 HMAC-SHA256 盲索引
     * 用于手机号等值查询：WHERE phone_hash = hmacSha256(phone)
     *
     * @param input 原始值（如手机号）
     * @return 64 字符的十六进制哈希字符串
     */
    public String hmacSha256(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacSecretKey);
            byte[] hashBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));

            // 转为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[CryptoUtils] HMAC-SHA256 计算失败", e);
            throw new RuntimeException("哈希计算失败", e);
        }
    }

    // ================== 手机号加密快捷方法 ==================

    /**
     * 加密手机号（存储到 phone 字段）
     */
    public String encryptPhone(String phone) {
        return encrypt(phone);
    }

    /**
     * 解密手机号（从 phone 字段还原明文）
     */
    public String decryptPhone(String encryptedPhone) {
        return decrypt(encryptedPhone);
    }

    /**
     * 生成手机号盲索引（存储到 phone_hash 字段）
     */
    public String phoneHash(String phone) {
        return hmacSha256(phone);
    }
}
