package com.auction.server.repository;

import com.auction.server.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Spring Data JPA đã cung cấp sẵn các hàm cơ bản như save(), findAll(), findById()...
    // Tạm thời với tính năng "Tạo phiên đấu giá", ta không cần viết thêm hàm nào ở đây cả.
    // Sau này nếu cần tìm kiếm riêng biệt, ví dụ: tìm sản phẩm theo tên, ta sẽ khai báo thêm.

}