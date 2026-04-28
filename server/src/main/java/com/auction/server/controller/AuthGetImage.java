package com.auction.server.controller;

<<<<<<< HEAD
=======
import com.auction.server.ServerApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
public class AuthGetImage {
<<<<<<< HEAD

=======
    private static final Logger logger = LoggerFactory.getLogger(AuthGetImage.class);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
    private final Path rootLocation = Paths.get("upload/images");

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
<<<<<<< HEAD
            System.out.println("Đường dẫn file thực tế đang tìm: " + file.toAbsolutePath());
            if (resource.exists() || resource.isReadable()) {
=======
            logger.info("Đường dẫn file thực tế đang tìm: {}", file.toAbsolutePath());
            if (resource.exists() || resource.isReadable()) {
                logger.info("Đã tim thấy file {}", filename);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
                String contentType = Files.probeContentType(file);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
<<<<<<< HEAD

=======
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
<<<<<<< HEAD
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
=======
                logger.error("Không tìm thấy file: {}", filename);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Lỗi khi tìm file và trả về phản hồi: {}", e.getMessage(), e);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
            return ResponseEntity.internalServerError().build();
        }
    }
}