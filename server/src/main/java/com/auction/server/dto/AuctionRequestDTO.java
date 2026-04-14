package com.auction.server.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class AuctionRequestDTO {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String productName;

    @NotBlank(message = "Loại sản phẩm không được để trống")
    private String productType;

    private String imageUrl;

    @Size(max = 1000, message = "Mô tả không được quá 1000 ký tự")
    private String description;

    @NotNull(message = "Giá khởi điểm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá khởi điểm phải lớn hơn 0")
    private BigDecimal startingPrice;

    @NotNull(message = "Bước giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Bước giá phải lớn hơn 0")
    private BigDecimal stepPrice;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    @Future(message = "Thời gian kết thúc phải ở tương lai")
    private LocalDateTime endTime;

    @NotNull(message = "ID người bán không được để trống")
    private Integer sellerId;

    public AuctionRequestDTO() {
    }

    public AuctionRequestDTO(String productName, String productType, String imageUrl, String description,
                             BigDecimal startingPrice, BigDecimal stepPrice, LocalDateTime endTime, Integer sellerId) {
        this.productName = productName;
        this.productType = productType;
        this.imageUrl = imageUrl;
        this.description = description;
        this.startingPrice = startingPrice;
        this.stepPrice = stepPrice;
        this.endTime = endTime;
        this.sellerId = sellerId;
    }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getStartingPrice() { return startingPrice; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }

    public BigDecimal getStepPrice() { return stepPrice; }
    public void setStepPrice(BigDecimal stepPrice) { this.stepPrice = stepPrice; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Integer getSellerId() { return sellerId; }
    public void setSellerId(Integer sellerId) { this.sellerId = sellerId; }
}