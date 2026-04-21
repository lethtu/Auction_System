package com.auction.server.factory;

import com.auction.server.model.Art;
import com.auction.server.model.Electronics;
import com.auction.server.model.Item;
import com.auction.server.model.Vehicle;

public class ItemFactory {
    
    public static Item createItem(String type) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        
        switch (type.toLowerCase()) {
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
}
