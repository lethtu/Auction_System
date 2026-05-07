package com.auction.server.factory;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.model.Art;
import com.auction.server.model.Electronics;
import com.auction.server.model.Item;
import com.auction.server.model.Vehicle;

public final class ItemFactory {

    private ItemFactory() {
    }

    public static Item createItem(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Loại sản phẩm không hợp lệ");
        }

        switch (type.trim().toLowerCase()) {
            case "electronics":
                return new Electronics();
            case "art":
                return new Art();
            case "vehicle":
                return new Vehicle();
            default:
                throw new IllegalArgumentException("Unknown item type: " + type);
        }
    }

    public static Item createItem(String type, CreateAuctionRequest params) {
        Item item = createItem(type);

        if (params != null) {
            item.setName(params.getName());
            item.setType(params.getType());
            item.setImagePath(params.getImagePath());
            item.setDescription(params.getDescription());
        }

        return item;
    }
}
