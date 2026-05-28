package com.auction.server.exception;

import com.auction.server.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<?> handleAuthenticationException(AuthenticationException ex) {
        logger.warn("Authentication failure: {}", ex.getMessage());
        return ApiResponse.error(401, safeMessage(ex));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<?> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return ApiResponse.error(404, safeMessage(ex));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<?> handlePermissionDeniedException(PermissionDeniedException ex) {
        logger.warn("Permission denied: {}", ex.getMessage());
        return ApiResponse.error(403, safeMessage(ex));
    }

    @ExceptionHandler(InvalidBidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleInvalidBidException(InvalidBidException ex) {
        logger.warn("Invalid bid: {}", ex.getMessage());
        return ApiResponse.error(400, safeMessage(ex));
    }

    @ExceptionHandler(AuctionClosedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleAuctionClosedException(AuctionClosedException ex) {
        logger.warn("Auction closed: {}", ex.getMessage());
        return ApiResponse.error(400, safeMessage(ex));
    }

    @ExceptionHandler(InvalidItemException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleInvalidItemException(InvalidItemException ex) {
        logger.warn("Invalid item: {}", ex.getMessage());
        return ApiResponse.error(400, safeMessage(ex));
    }

    @ExceptionHandler({
            ValidationException.class,
            BusinessException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleClientRuleException(ClientErrorException ex) {
        logger.warn("Client/business error: {}", ex.getMessage());
        return ApiResponse.error(ex.getStatus(), safeMessage(ex));
    }

    @ExceptionHandler(ClientErrorException.class)
    public ResponseEntity<ApiResponse<?>> handleClientErrorException(ClientErrorException ex) {
        logger.warn("Client error: {}", ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getStatus(), safeMessage(ex)));
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBadRequestException(RuntimeException ex) {
        logger.warn("Bad request: {}", ex.getMessage());
        return ApiResponse.error(400, safeMessage(ex));
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<?> handleSecurityException(SecurityException ex) {
        logger.warn("Forbidden request: {}", ex.getMessage());
        return ApiResponse.error(403, safeMessage(ex));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<?> handleNoResourceFoundException(NoResourceFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return ApiResponse.error(404, "Resource not found");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleGenericException(Exception ex) {
        logger.error("Unhandled server error", ex);
        return ApiResponse.error(500, "Internal server error");
    }

    private String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? "Invalid request" : message;
    }
}
