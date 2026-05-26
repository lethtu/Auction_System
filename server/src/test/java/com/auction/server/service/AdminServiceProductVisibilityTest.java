package com.auction.server.service;

import com.auction.server.model.Admin;
import com.auction.server.model.Item;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminServiceProductVisibilityTest {

    private final AuctionSessionRepository sessionRepository = Mockito.mock(AuctionSessionRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final ItemRepository itemRepository = Mockito.mock(ItemRepository.class);
    private final AdminService adminService = new AdminService(sessionRepository, userRepository, itemRepository);

    @Test
    void hideProduct_validAdminAndProduct_setsHiddenAndSavesItem() {
        Admin admin = admin(1);
        TestItem item = item(20, false);
        Mockito.when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        Mockito.when(itemRepository.findById(20)).thenReturn(Optional.of(item));

        adminService.hideProduct(20, 1);

        assertTrue(item.isHidden());
        Mockito.verify(itemRepository).save(item);
    }

    @Test
    void showProduct_validAdminAndProduct_clearsHiddenAndSavesItem() {
        Admin admin = admin(1);
        TestItem item = item(21, true);
        Mockito.when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        Mockito.when(itemRepository.findById(21)).thenReturn(Optional.of(item));

        adminService.showProduct(21, 1);

        assertFalse(item.isHidden());
        Mockito.verify(itemRepository).save(item);
    }

    @Test
    void hideProduct_withoutItemRepository_throwsClearException() {
        AdminService serviceWithoutItemRepository = new AdminService(sessionRepository, userRepository);
        Mockito.when(userRepository.findById(1)).thenReturn(Optional.of(admin(1)));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> serviceWithoutItemRepository.hideProduct(20, 1)
        );

        assertEquals("Product data repository not configured", ex.getMessage());
    }

    @Test
    void hideProduct_nullProductId_throwsProductNotFoundAndDoesNotSave() {
        Mockito.when(userRepository.findById(1)).thenReturn(Optional.of(admin(1)));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.hideProduct(null, 1)
        );

        assertEquals("Product not found", ex.getMessage());
        Mockito.verify(itemRepository, Mockito.never()).save(Mockito.any(Item.class));
    }

    @Test
    void showProduct_missingProduct_throwsProductNotFoundAndDoesNotSave() {
        Mockito.when(userRepository.findById(1)).thenReturn(Optional.of(admin(1)));
        Mockito.when(itemRepository.findById(99)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.showProduct(99, 1)
        );

        assertEquals("Product not found", ex.getMessage());
        Mockito.verify(itemRepository, Mockito.never()).save(Mockito.any(Item.class));
    }

    private Admin admin(Integer id) {
        Admin admin = new Admin();
        admin.setId(id);
        admin.setUsername("admin" + id);
        return admin;
    }

    private TestItem item(Integer id, boolean hidden) {
        TestItem item = new TestItem();
        item.setId(id);
        item.setName("Product " + id);
        item.setHidden(hidden);
        return item;
    }

    private static class TestItem extends Item {
        @Override
        public String getCategoryInfo() {
            return "TEST";
        }
    }
}
