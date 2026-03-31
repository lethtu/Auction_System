package com.auction.server.repository;

import com.auction.server.model.item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<item, Integer> {
    List<item> findByStatus(String status);
}
