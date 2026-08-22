package com.campus.market.controller;

import com.campus.market.common.BizException;
import com.campus.market.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/**
 * 文件上传控制器
 *
 * 功能：
 * - 商品图片上传（需登录）
 * - 保存到本地 ./uploads/ 目录
 * - 返回访问URL：/uploads/{filename}
 *
 * 安全措施：
 * - 校验文件类型（白名单）
 * - 校验文件大小
 * - 文件名重命名（UUID防冲突+防路径遍历攻击）
 */
@Slf4j
@RestController
@RequestMapping("/file")
@Tag(name = "文件上传", description = "图片上传接口")
public class FileController {

    @Value("${campus.market.upload.path}")
    private String uploadPath;

    @Value("${campus.market.upload.allowed-types}")
    private String allowedTypes;

    @Value("${campus.market.upload.max-size}")
    private long maxSize;

    /** 允许上传的图片扩展名 */
    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "webp"};

    /**
     * 图片上传
     * @param file MultipartFile
     * @return 图片访问URL
     */
    @PostMapping("/upload")
    @Operation(summary = "上传图片", description = "上传商品图片，返回图片URL。支持jpg/jpeg/png/gif/webp，最大5MB")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        // 1. 校验文件不为空
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择要上传的文件");
        }

        // 2. 校验文件大小
        if (file.getSize() > maxSize) {
            throw new BizException(400, "文件大小超过限制（最大5MB）");
        }

        // 3. 校验文件类型（扩展名白名单）
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        if (!isAllowedExtension(extension)) {
            throw new BizException(400, "不支持的文件类型，仅支持：" + Arrays.toString(ALLOWED_EXTENSIONS));
        }

        // 3.5 校验文件魔数（真实内容类型，防伪装）
        if (!isValidMagicNumber(file, extension)) {
            throw new BizException(400, "文件内容与扩展名不符，请上传真实图片文件");
        }

        // 4. 生成新文件名（UUID + 原扩展名）
        String newFilename = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        // 5. 确保上传目录存在（基于工作目录解析绝对路径）
        File uploadDir = new File(uploadPath);
        if (!uploadDir.isAbsolute()) {
            uploadDir = new File(System.getProperty("user.dir"), uploadPath);
        }
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
            log.info("[文件上传] 创建上传目录: {}", uploadDir.getAbsolutePath());
        }

        // 6. 保存文件
        File destFile = new File(uploadDir, newFilename);
        try {
            file.transferTo(destFile);
            log.info("[文件上传成功] originalName={}, savedAs={}, size={}", originalFilename, newFilename, file.getSize());
        } catch (IOException e) {
            log.error("[文件上传失败] {}", e.getMessage(), e);
            throw new BizException(500, "文件上传失败，请重试");
        }

        // 7. 返回访问URL（相对路径，前端拼接 baseUrl）
        String url = "/uploads/" + newFilename;
        return Result.success("上传成功", url);
    }

    /**
     * 获取文件扩展名（小写）
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 校验扩展名是否允许
     */
    private boolean isAllowedExtension(String extension) {
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验文件魔数（真实文件头），防止伪装成图片的可执行文件
     * 支持：JPEG(FF D8 FF) / PNG(89 50 4E 47) / GIF(47 49 46 38) / WEBP(52 49 46 46...57 45 42 50)
     */
    private boolean isValidMagicNumber(MultipartFile file, String extension) {
        byte[] header = new byte[12];
        try (java.io.InputStream in = file.getInputStream()) {
            int read = in.read(header, 0, header.length);
            if (read < 4) {
                return false;
            }
        } catch (IOException e) {
            log.error("[文件魔数校验] 读取文件头失败: {}", e.getMessage());
            return false;
        }

        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return (header[0] & 0xFF) == 0xFF
                        && (header[1] & 0xFF) == 0xD8
                        && (header[2] & 0xFF) == 0xFF;
            case "png":
                return (header[0] & 0xFF) == 0x89
                        && (header[1] & 0xFF) == 0x50
                        && (header[2] & 0xFF) == 0x4E
                        && (header[3] & 0xFF) == 0x47;
            case "gif":
                return (header[0] & 0xFF) == 0x47
                        && (header[1] & 0xFF) == 0x49
                        && (header[2] & 0xFF) == 0x46
                        && (header[3] & 0xFF) == 0x38;
            case "webp":
                return (header[0] & 0xFF) == 0x52
                        && (header[1] & 0xFF) == 0x49
                        && (header[2] & 0xFF) == 0x46
                        && (header[3] & 0xFF) == 0x46
                        && (header[8] & 0xFF) == 0x57
                        && (header[9] & 0xFF) == 0x45
                        && (header[10] & 0xFF) == 0x42
                        && (header[11] & 0xFF) == 0x50;
            default:
                return false;
        }
    }
}
