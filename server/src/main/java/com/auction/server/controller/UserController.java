package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.exception.ClientErrorException;
import com.auction.server.model.User;
import com.auction.server.service.CloudinaryService;
import com.auction.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private static final int SUCCESS_STATUS = 200;
    private static final int ERROR_STATUS = 400;
    private static final String SUCCESS_MESSAGE = "Success";

    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");
    private static final String AVATAR_UPLOAD_DIR = "upload/avatar";
    private static final String AVATAR_PUBLIC_PREFIX = "/api/files/avatar/";

    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    public UserController(UserService userService, CloudinaryService cloudinaryService) {
        this.userService = Objects.requireNonNull(userService, "userService must not be null");
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(success(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.status(404).body(error("User not found", null));
        }
        return ResponseEntity.ok(success("Account information retrieved successfully", user));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        try {
            User updatedUser = userService.updateProfile(id, request);
            return ResponseEntity.ok(success("Account information updated successfully", updatedUser));
        } catch (ClientErrorException e) {
            return ResponseEntity.status(e.getStatus()).body(error(e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage(), null));
        }
    }

    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @PathVariable Integer id,
            @RequestParam("avatar") MultipartFile file) {
        try {
            // 1. Validate user exists
            User user = userService.getUserById(id);
            if (user == null) {
                return ResponseEntity.status(404).body(
                        new ApiResponse<>(404, "User not found", null));
            }

            // 2. Validate file not empty
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse<>(ERROR_STATUS, "Image file is empty.", null));
            }

            // 3. Validate file size (5MB)
            if (file.getSize() > MAX_AVATAR_SIZE) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse<>(ERROR_STATUS, "Image file exceeds the 5MB limit.", null));
            }

            // 4. Validate extension from original filename
            String extension = getExtension(file.getOriginalFilename());
            if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
                logger.warn("Avatar upload rejected: Invalid extension '{}' for file '{}'", extension, file.getOriginalFilename());
                return ResponseEntity.badRequest().body(
                        new ApiResponse<>(ERROR_STATUS, "Invalid file. Please select a PNG, JPG, JPEG or WEBP image under 5MB.", null));
            }

            // 5. Validate content-type or fallback to magic bytes
            String contentType = file.getContentType();
            boolean isValidContent = false;
            if (contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
                isValidContent = true;
            } else if (contentType == null || contentType.equalsIgnoreCase("application/octet-stream")) {
                // Fallback to magic bytes
                byte[] header = new byte[12];
                try (InputStream is = file.getInputStream()) {
                    int read = is.read(header);
                    if (read >= 4) {
                        if (extension.equals("png") && header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
                            isValidContent = true;
                        } else if ((extension.equals("jpg") || extension.equals("jpeg")) && header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
                            isValidContent = true;
                        } else if (extension.equals("webp") && read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F' && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                            isValidContent = true;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Avatar upload error reading magic bytes: {}", e.getMessage());
                }
            }

            if (!isValidContent) {
                logger.warn("Avatar upload rejected: Invalid content for file '{}', Content-Type: {}, Size: {}", file.getOriginalFilename(), contentType, file.getSize());
                return ResponseEntity.badRequest().body(
                        new ApiResponse<>(ERROR_STATUS, "Invalid file. Please select a PNG, JPG, JPEG or WEBP image under 5MB.", null));
            }

            // 6. Prefer Cloudinary when configured
            String oldAvatarUrl = user.getAvatarUrl();
            if (cloudinaryService != null && cloudinaryService.isConfigured()) {
                try {
                    String publicId = "user_" + id;
                    String cloudinaryUrl = cloudinaryService.uploadFileWithPublicId(
                            file,
                            "auction_system/users/avatars",
                            publicId,
                            false
                    );
                    userService.updateAvatarUrl(id, cloudinaryUrl);
                    logger.info("Avatar uploaded to Cloudinary for user {}: {}", id, cloudinaryUrl);
                    return ResponseEntity.ok(new ApiResponse<>(SUCCESS_STATUS,
                            "Avatar updated successfully.",
                            Map.of("avatarUrl", cloudinaryUrl)));
                } catch (Exception e) {
                    logger.warn("Cloudinary avatar upload failed, falling back to local storage: {}", e.getMessage(), e);
                }
            }

            // 7. Generate safe filename
            String safeFileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

            // 8. Ensure avatar directory exists
            Path avatarDir = Paths.get(AVATAR_UPLOAD_DIR).toAbsolutePath().normalize();
            Files.createDirectories(avatarDir);

            // 9. Resolve destination and check path traversal
            Path destination = avatarDir.resolve(safeFileName).normalize();
            if (!destination.startsWith(avatarDir)) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse<>(ERROR_STATUS, "Invalid file path.", null));
            }

            // 10. Save file to disk
            file.transferTo(destination);

            // 11. Build public URL and update DB
            String publicUrl = AVATAR_PUBLIC_PREFIX + safeFileName;
            userService.updateAvatarUrl(id, publicUrl);

            // 12. Delete old avatar file (only after successful DB update)
            deleteOldAvatar(oldAvatarUrl, avatarDir);

            logger.info("Avatar uploaded for user {}: originalFilename={}, contentType={}, extension={}, safeFileName={}", 
                    id, file.getOriginalFilename(), contentType, extension, safeFileName);
            logger.info("Avatar saved to absolute path: {}", destination.toAbsolutePath());
            logger.info("Avatar URL returned to client: {}", publicUrl);
            return ResponseEntity.ok(new ApiResponse<>(SUCCESS_STATUS,
                    "Avatar updated successfully.",
                    Map.of("avatarUrl", publicUrl)));

        } catch (ClientErrorException e) {
            return ResponseEntity.status(e.getStatus()).body(
                    new ApiResponse<>(e.getStatus(), e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(ERROR_STATUS, e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error uploading avatar for user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    new ApiResponse<>(500, "Avatar upload failed.", null));
        }
    }

    private void deleteOldAvatar(String oldAvatarUrl, Path avatarDir) {
        if (oldAvatarUrl == null || oldAvatarUrl.isBlank()) {
            return;
        }

        if (!oldAvatarUrl.startsWith(AVATAR_PUBLIC_PREFIX)) {
            return;
        }

        try {
            String oldFileName = oldAvatarUrl.substring(AVATAR_PUBLIC_PREFIX.length());
            Path oldFile = avatarDir.resolve(oldFileName).normalize();

            // Security: ensure old file is inside avatar directory
            if (!oldFile.startsWith(avatarDir)) {
                logger.warn("Old avatar path outside avatar dir, skipping delete: {}", oldFile);
                return;
            }

            if (Files.exists(oldFile)) {
                Files.delete(oldFile);
                logger.info("Deleted old avatar: {}", oldFile);
            }
        } catch (Exception e) {
            logger.warn("Could not delete old avatar: {}", e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private ApiResponse<List<User>> success(List<User> users) {
        return new ApiResponse<>(SUCCESS_STATUS, SUCCESS_MESSAGE, users);
    }

    private ApiResponse<User> success(String message, User user) {
        return new ApiResponse<>(SUCCESS_STATUS, message, user);
    }

    @PostMapping("/{id}/set-password")
    public ResponseEntity<ApiResponse<User>> setPassword(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("password");
            User updatedUser = userService.setPassword(id, newPassword);
            return ResponseEntity.ok(success("Password set successfully.", updatedUser));
        } catch (ClientErrorException e) {
            return ResponseEntity.status(e.getStatus()).body(error(e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error setting password for user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    new ApiResponse<>(500, "Failed to set password.", null));
        }
    }

    @PostMapping("/{id}/change-password")
    public ResponseEntity<ApiResponse<User>> changePassword(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        try {
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            User updatedUser = userService.changePassword(id, oldPassword, newPassword);
            return ResponseEntity.ok(success("Password changed successfully.", updatedUser));
        } catch (ClientErrorException e) {
            return ResponseEntity.status(e.getStatus()).body(error(e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error changing password for user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    new ApiResponse<>(500, "Failed to change password.", null));
        }
    }

    private ApiResponse<User> error(String message, User user) {
        return new ApiResponse<>(ERROR_STATUS, message, user);
    }
}
