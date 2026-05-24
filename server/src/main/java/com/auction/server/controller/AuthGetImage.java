package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.service.CloudinaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

    @Autowired(required = false)
    private CloudinaryService cloudinaryService;

    private static final String DEFAULT_UPLOAD_ROOT_DIRECTORY = "upload/images";
    private static final String UPLOAD_DIR_SYSTEM_PROPERTY = "auction.upload.dir";
    private static final String UPLOAD_DIR_ENV_NAME = "AUCTION_UPLOAD_DIR";
    private static final String FILE_REQUEST_PARAM = "file";
    private static final String IMAGE_PATH_KEY = "imagePath";
    private static final String IMAGE_URL_KEY = "imageUrl";

    private static final String DEFAULT_EXTENSION = ".png";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private static final String UPLOAD_SUCCESS_MESSAGE = "Image uploaded successfully.";
    private static final String UPLOAD_FAILED_MESSAGE = "Image upload failed.";
    private static final String EMPTY_FILE_MESSAGE = "Image file is empty.";
    private static final String INVALID_IMAGE_TYPE_MESSAGE = "Only image files are accepted.";
    private static final String INVALID_FILE_PATH_MESSAGE = "Invalid file path.";

    private static final int BAD_REQUEST_STATUS = 400;
    private static final int MAX_EXTENSION_LENGTH = 10;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam(FILE_REQUEST_PARAM) MultipartFile file,
            @RequestParam(value = "uuid", required = false) String uuid
    ) {
        try {
            validateImageFile(file);

            String safeUuid = normalizeOptionalUuid(uuid);
            if (safeUuid == null) {
                safeUuid = UUID.randomUUID().toString();
            }

            String cloudinaryUrl = null;
            if (cloudinaryService != null && cloudinaryService.isConfigured()) {
                try {
                    cloudinaryUrl = cloudinaryService.uploadFileWithPublicId(
                            file,
                            "auction_system/items/images",
                            safeUuid,
                            false
                    );
                    logger.info("Image file uploaded to Cloudinary: {}", cloudinaryUrl);
                } catch (Exception e) {
                    logger.warn("Cloudinary image upload failed, falling back to local storage: {}", e.getMessage(), e);
                }
            }

            String localImagePath = null;
            String localImageUrl = null;
            try {
                Path root = getRootLocation();
                Files.createDirectories(root);

                Path itemFolderPath = root.resolve(safeUuid).normalize();
                Files.createDirectories(itemFolderPath);

                String storedFileName = safeUuid + getSafeExtension(file.getOriginalFilename());
                Path destination = itemFolderPath.resolve(storedFileName).normalize();

                if (!isInsideRootLocation(destination)) {
                    return badRequest(INVALID_FILE_PATH_MESSAGE);
                }

                Files.copy(file.getInputStream(), destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                localImagePath = toClientImagePath(destination);
                localImageUrl = "/api/files/images/" + localImagePath;
                logger.info("Image file uploaded locally: {}", destination);
            } catch (Exception e) {
                if (cloudinaryUrl == null) {
                    throw e;
                }
                logger.warn("Local image mirror failed after Cloudinary upload: {}", e.getMessage(), e);
            }

            String imagePath = cloudinaryUrl != null ? cloudinaryUrl : localImagePath;
            String imageUrl = cloudinaryUrl != null ? cloudinaryUrl : localImageUrl;

            return ResponseEntity.ok(ApiResponse.success(
                    UPLOAD_SUCCESS_MESSAGE,
                    Map.of(
                            IMAGE_PATH_KEY, imagePath,
                            IMAGE_URL_KEY, imageUrl
                    )
            ));

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());

        } catch (Exception e) {
            logger.error("Error uploading image: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error(UPLOAD_FAILED_MESSAGE));
        }
    }


    @PostMapping(value = "/models-3d", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadModel3D(
            @RequestParam(FILE_REQUEST_PARAM) MultipartFile file,
            @RequestParam(value = "uuid", required = false) String uuid
    ) {
        try {
            validateModel3DFile(file);

            String safeUuid = normalizeOptionalUuid(uuid);
            if (safeUuid == null) {
                safeUuid = UUID.randomUUID().toString();
            }

            String cloudinaryUrl = null;
            if (cloudinaryService != null && cloudinaryService.isConfigured()) {
                try {
                    cloudinaryUrl = cloudinaryService.uploadFileWithPublicId(
                            file,
                            "auction_system/items/models_3d",
                            safeUuid,
                            true
                    );
                    logger.info("3D model uploaded to Cloudinary: {}", cloudinaryUrl);
                } catch (Exception e) {
                    logger.warn("Cloudinary 3D upload failed, falling back to local storage: {}", e.getMessage(), e);
                }
            }

            String localModelUrl = null;
            Path root = getModel3DRootLocation();
            try {
                Files.createDirectories(root);

                Path itemFolderPath = root.resolve(safeUuid).normalize();
                Files.createDirectories(itemFolderPath);

                String extension = getSafeModel3DExtension(file.getOriginalFilename());
                Path destination = itemFolderPath.resolve(safeUuid + extension).normalize();

                if (!destination.startsWith(root)) {
                    return badRequest(INVALID_FILE_PATH_MESSAGE);
                }

                Files.copy(file.getInputStream(), destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                localModelUrl = "/api/files/models-3d/" + safeUuid + "/" + safeUuid + extension;
                logger.info("3D model uploaded locally: {}", destination);
            } catch (Exception e) {
                if (cloudinaryUrl == null) {
                    throw e;
                }
                logger.warn("Local 3D model mirror failed after Cloudinary upload: {}", e.getMessage(), e);
            }

            String modelUrl = cloudinaryUrl != null ? cloudinaryUrl : localModelUrl;

            return ResponseEntity.ok(ApiResponse.success(
                    "3D model uploaded successfully.",
                    Map.of(
                            "model3dPath", safeUuid,
                            "model3dUrl", modelUrl
                    )
            ));

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error uploading 3D model: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("3D model upload failed."));
        }
    }


    @GetMapping("/models-3d/{folder}/{filename:.+}")
    public ResponseEntity<Resource> serveModel3D(
            @PathVariable String folder,
            @PathVariable String filename
    ) {
        try {
            Path modelsRoot = getModel3DRootLocation();
            Path file = modelsRoot.resolve(folder).resolve(filename).normalize();
            return serveFrom(file, modelsRoot);
        } catch (Exception e) {
            logger.error("Error processing 3D model path: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/images/{folder}/{filename:.+}")
    public ResponseEntity<Resource> serveFileInFolder(
            @PathVariable String folder,
            @PathVariable String filename
    ) {
        try {
            Path file = getRootLocation().resolve(folder).resolve(filename).normalize();
            return serve(file);
        } catch (Exception e) {
            logger.error("Error processing image path: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<Resource> serveAvatar(@PathVariable String filename) {
        try {
            Path avatarRoot = Paths.get("upload/avatar").toAbsolutePath().normalize();
            Path file = avatarRoot.resolve(filename).normalize();
            return serveFrom(file, avatarRoot);
        } catch (Exception e) {
            logger.error("Error processing avatar path: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveOldFile(@PathVariable String filename) {
        try {
            Path file = getRootLocation().resolve(filename).normalize();
            return serve(file);
        } catch (Exception e) {
            logger.error("Error processing legacy image path: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(EMPTY_FILE_MESSAGE);
        }

        String contentType = file.getContentType();
        if (!isImageContentType(contentType)) {
            throw new IllegalArgumentException(INVALID_IMAGE_TYPE_MESSAGE);
        }
    }

    private boolean isImageContentType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    private Path createItemFolder(Path root) throws IOException {
        Path itemFolderPath = root.resolve(UUID.randomUUID().toString()).normalize();
        Files.createDirectories(itemFolderPath);
        return itemFolderPath;
    }

    private String buildStoredFileName(String originalFileName) {
        return UUID.randomUUID() + getSafeExtension(originalFileName);
    }

    private String getSafeExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return DEFAULT_EXTENSION;
        }

        String extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();

        if (!isSafeExtension(extension)) {
            return DEFAULT_EXTENSION;
        }

        return extension;
    }

    private boolean isSafeExtension(String extension) {
        return extension.length() > 1
                && extension.length() <= MAX_EXTENSION_LENGTH
                && extension.matches("\\.[a-z0-9]+");
    }

    private String toClientImagePath(Path destination) {
        return getRootLocation()
                .relativize(destination)
                .toString()
                .replace('\\', '/');
    }

    private ResponseEntity<Resource> serve(Path file) {
        return serveFrom(file, getRootLocation());
    }

    private ResponseEntity<Resource> serveFrom(Path file, Path allowedRoot) {
        try {
            if (file == null || !file.normalize().startsWith(allowedRoot)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(file.toUri());
            logger.info("Actual file path being accessed: {}", file.toAbsolutePath());

            if (!resource.exists() || !resource.isReadable()) {
                logger.warn("File not found or not readable: {}", file);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(detectContentType(file)))
                    .header(HttpHeaders.CONTENT_DISPOSITION, buildInlineContentDisposition(resource))
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error finding file and returning response: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String detectContentType(Path file) throws IOException {
        String contentType = Files.probeContentType(file);
        return hasText(contentType) ? contentType : DEFAULT_CONTENT_TYPE;
    }

    private String buildInlineContentDisposition(Resource resource) {
        return "inline; filename=\"" + safeFileName(resource.getFilename()) + "\"";
    }

    private String safeFileName(String fileName) {
        if (!hasText(fileName)) {
            return "image";
        }

        return fileName
                .replace("\\", "_")
                .replace("/", "_")
                .replace("\"", "_")
                .replace("\r", "_")
                .replace("\n", "_");
    }

    private boolean isInsideRootLocation(Path file) {
        Path root = getRootLocation();
        return file != null && file.normalize().startsWith(root);
    }

    private Path getRootLocation() {
        return Paths.get(resolveUploadRootDirectory()).toAbsolutePath().normalize();
    }

    private String resolveUploadRootDirectory() {
        String propertyValue = System.getProperty(UPLOAD_DIR_SYSTEM_PROPERTY);
        if (hasText(propertyValue)) {
            return propertyValue.trim();
        }

        String envValue = System.getenv(UPLOAD_DIR_ENV_NAME);
        if (hasText(envValue)) {
            return envValue.trim();
        }

        return DEFAULT_UPLOAD_ROOT_DIRECTORY;
    }

    private void validateModel3DFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("3D model file is empty.");
        }

        getSafeModel3DExtension(file.getOriginalFilename());
    }

    private Path getModel3DRootLocation() {
        Path imageRoot = getRootLocation();
        Path uploadRoot = imageRoot.getParent();
        if (uploadRoot == null) {
            uploadRoot = Paths.get("upload").toAbsolutePath().normalize();
        }
        return uploadRoot.resolve("models-3d").toAbsolutePath().normalize();
    }

    private String getSafeModel3DExtension(String fileName) {
        if (!hasText(fileName)) {
            throw new IllegalArgumentException("Only .glb 3D model files are accepted.");
        }

        String lowerFileName = fileName.trim().toLowerCase();
        if (!lowerFileName.endsWith(".glb")) {
            throw new IllegalArgumentException("Only .glb 3D model files are accepted.");
        }

        return ".glb";
    }

    private String normalizeOptionalUuid(String uuid) {
        if (!hasText(uuid)) {
            return null;
        }

        try {
            return UUID.fromString(uuid.trim()).toString();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid uuid.");
        }
    }
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ResponseEntity<ApiResponse<Map<String, String>>> badRequest(String message) {
        return ResponseEntity.badRequest().body(ApiResponse.error(BAD_REQUEST_STATUS, message));
    }
}
