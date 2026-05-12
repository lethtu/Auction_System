package com.auction.server.factory;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.model.Art;
import com.auction.server.model.Electronics;
import com.auction.server.model.Item;
import com.auction.server.model.Vehicle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    @Test
    void createElectronicsItem() {
        Item item = ItemFactory.createItem("electronics");
        assertInstanceOf(Electronics.class, item);
    }

    @Test
    void createArtItem() {
        Item item = ItemFactory.createItem("art");
        assertInstanceOf(Art.class, item);
    }

    @Test
    void createVehicleItem() {
        Item item = ItemFactory.createItem("vehicle");
        assertInstanceOf(Vehicle.class, item);
    }

    @Test
    void createItemWithParams() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setName("iPhone 15");
        request.setType("electronics");
        request.setDescription("Like new");

        Item item = ItemFactory.createItem("electronics", request);

        assertInstanceOf(Electronics.class, item);
        assertEquals("iPhone 15", item.getName());
        assertEquals("electronics", item.getType());
        assertEquals("Like new", item.getDescription());
    }

    @Test
    void unknownTypeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ItemFactory.createItem("food"));
    }
}
