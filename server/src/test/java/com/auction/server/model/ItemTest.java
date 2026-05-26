package com.auction.server.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ItemTest {

    private static final String UUID = "123e4567-e89b-12d3-a456-426614174000";

    @Test
    void settersAndGettersStoreBasicFields() {
        TestItem item = new TestItem();

        item.setId(3);
        item.setName("Camera");
        item.setType("ELECTRONIC");
        item.setDescription("Vintage camera");
        item.setHidden(Boolean.TRUE);

        assertEquals(3, item.getId());
        assertEquals("Camera", item.getName());
        assertEquals("ELECTRONIC", item.getType());
        assertEquals("Vintage camera", item.getDescription());
        assertTrue(item.isHidden());
        assertEquals("test-category", item.getCategoryInfo());
    }

    @Test
    void hiddenDefaultsToFalseAndNullHiddenIsTreatedAsFalse() {
        TestItem item = new TestItem();

        assertFalse(item.isHidden());

        item.setHidden(null);

        assertFalse(item.isHidden());
    }

    @Test
    void imagePathAcceptsDirectUuidAndTrimsWhitespace() {
        TestItem item = new TestItem();

        item.setImagePath("  " + UUID + "  ");

        assertEquals(UUID, item.getImagePath());
        assertEquals(UUID, item.getUuid());
    }

    @Test
    void imagePathExtractsUuidFromPath() {
        TestItem item = new TestItem();

        item.setImagePath("/uploads/images/" + UUID + ".jpg");

        assertEquals(UUID, item.getImagePath());
    }

    @Test
    void imagePathStoresRawTrimmedValueWhenNoUuidFound() {
        TestItem item = new TestItem();

        item.setImagePath("  relative/image.png  ");

        assertEquals("relative/image.png", item.getImagePath());
    }

    @Test
    void nullImagePathDoesNotOverrideExistingUuid() {
        TestItem item = new TestItem();
        item.setUuid(UUID);

        item.setImagePath(null);

        assertEquals(UUID, item.getUuid());
    }

    @Test
    void getUuidLazilyCreatesUuidWhenMissing() {
        TestItem item = new TestItem();

        String generated = item.getUuid();

        assertNotNull(generated);
        assertEquals(36, generated.length());
    }

    @Test
    void prePersistCreatesUuidOnlyWhenMissing() {
        TestItem missingUuid = new TestItem();
        TestItem existingUuid = new TestItem();
        existingUuid.setUuid(UUID);

        missingUuid.callOnCreate();
        existingUuid.callOnCreate();

        assertNotNull(missingUuid.getUuid());
        assertEquals(UUID, existingUuid.getUuid());
    }

    private static class TestItem extends Item {
        @Override
        public String getCategoryInfo() {
            return "test-category";
        }

        void callOnCreate() {
            onCreate();
        }
    }
}
