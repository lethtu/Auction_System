package com.auction.server.factory;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.model.Art;
import com.auction.server.model.Electronics;
import com.auction.server.model.Item;
import com.auction.server.model.Vehicle;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void createItem_trimsAndIgnoresCase() {
        Item item = ItemFactory.createItem("  ELECTRONICS  ");
        assertInstanceOf(Electronics.class, item);
    }

    @Test
    void createItemWithParams() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setName("iPhone 15");
        request.setType("electronics");
        request.setDescription("Like new");
        request.setImagePath("phone.png");

        Item item = ItemFactory.createItem("electronics", request);

        assertInstanceOf(Electronics.class, item);
        assertEquals("iPhone 15", item.getName());
        assertEquals("electronics", item.getType());
        assertEquals("Like new", item.getDescription());
    }

    @Test
    void createItemWithNullParams_returnsTypedItem() {
        Item item = ItemFactory.createItem("art", null);
        assertInstanceOf(Art.class, item);
    }

    @Test
    void nullTypeThrowsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ItemFactory.createItem(null)
        );

        assertEquals("Invalid product type", ex.getMessage());
    }

    @Test
    void blankTypeThrowsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ItemFactory.createItem("   ")
        );

        assertEquals("Invalid product type", ex.getMessage());
    }

    @Test
    void unknownTypeThrowsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ItemFactory.createItem("food")
        );

        assertEquals("Unknown item type: food", ex.getMessage());
    }

    @Test
    void privateConstructor_canBeInvokedByReflection() throws Exception {
        Constructor<ItemFactory> constructor = ItemFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}