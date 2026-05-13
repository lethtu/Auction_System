package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class AuthGetImage {
    private static final Logger logger = LoggerFactory.getLogger(AuthGetImage.class);
    private final Path rootLocation = Paths.get("upload/images").toAbsolutePath().normalize();

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "File ảnh đang trống."));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Chỉ chấp nhận file ảnh."));
            }

            Files.createDirectories(rootLocation);

            String itemFolder = UUID.randomUUID().toString();
            Path itemFolderPath = rootLocation.resolve(itemFolder).normalize();
            Files.createDirectories(itemFolderPath);

            String originalName = file.getOriginalFilename();
            String extension = getExtension(originalName);
            String storedFileName = UUID.randomUUID() + extension;

            Path destination = itemFolderPath.resolve(storedFileName).normalize();

            if (!destination.startsWith(rootLocation)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Đường dẫn file không hợp lệ."));
            }

            file.transferTo(destination);

            String imagePath = itemFolder + "/" + storedFileName;

            logger.info("Đã upload file ảnh: {}", destination);

            return ResponseEntity.ok(ApiResponse.success(
                    "Upload ảnh thành công.",
                    Map.of("imagePath", imagePath)
            ));
        } catch (IOException e) {
            logger.error("Lỗi khi upload ảnh: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Upload ảnh thất bại."));
        }
    }

    @GetMapping("/images/{folder}/{filename:.+}")
    public ResponseEntity<Resource> serveFileInFolder(
            @PathVariable String folder,
            @PathVariable String filename
    ) {
        return serve(rootLocation.resolve(folder).resolve(filename).normalize());
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveOldFile(@PathVariable String filename) {
        return serve(rootLocation.resolve(filename).normalize());
    }

    private ResponseEntity<Resource> serve(Path file) {
        try {
            if (!file.startsWith(rootLocation)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(file.toUri());

            logger.info("Đường dẫn file thực tế đang tìm: {}", file.toAbsolutePath());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(file);

                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            }

            logger.error("Không tìm thấy file: {}", file);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("Lỗi khi tìm file và trả về phản hồi: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".png";
        }

        return fileName.substring(fileName.lastIndexOf('.'));
    }
}