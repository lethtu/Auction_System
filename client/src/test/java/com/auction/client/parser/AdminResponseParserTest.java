package com.auction.client.parser;

import com.auction.client.model.AdminSessionRow;
import com.auction.client.model.AdminUserRow;
import com.auction.client.model.PendingSessionRow;
import javafx.beans.value.ObservableValue;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdminResponseParserTest {

    @Test
    void parsePendingSessions_validData_returnsRows() {
        JSONArray data = new JSONArray("""
                [
                  {
                    "id": 1,
                    "productName": "Laptop",
                    "startingPrice": 1000000
                  }
                ]
                """);

        List<PendingSessionRow> result = AdminResponseParser.parsePendingSessions(data);

        assertEquals(1, result.size());
        Object row = result.get(0);

        assertEquals(1, read(row, "id", "sessionId"));
        assertEquals("Laptop", read(row, "productName", "product"));
        assertEquals(new BigDecimal("1000000"), read(row, "startingPrice", "price"));
    }

    @Test
    void parsePendingSessions_nullData_returnsEmptyList() {
        assertTrue(AdminResponseParser.parsePendingSessions(null).isEmpty());
    }

    @Test
    void parsePendingSessions_missingAndInvalidPrice_usesDefaultValues() {
        JSONArray data = new JSONArray("""
                [
                  {
                    "startingPrice": "abc"
                  }
                ]
                """);

        List<PendingSessionRow> result = AdminResponseParser.parsePendingSessions(data);

        assertEquals(1, result.size());
        Object row = result.get(0);

        assertEquals(0, read(row, "id", "sessionId"));
        assertEquals("Không rõ", read(row, "productName", "product"));
        assertEquals(BigDecimal.ZERO, read(row, "startingPrice", "price"));
    }

    @Test
    void parseAllSessions_validData_returnsRows() {
        JSONArray data = new JSONArray("""
                [
                  {
                    "id": 2,
                    "productName": "Phone",
                    "sellerUsername": "seller01",
                    "startingPrice": 500000,
                    "status": "APPROVED"
                  }
                ]
                """);

        List<AdminSessionRow> result = AdminResponseParser.parseAllSessions(data);

        assertEquals(1, result.size());
        Object row = result.get(0);

        assertEquals(2, read(row, "id", "sessionId"));
        assertEquals("Phone", read(row, "productName", "product"));
        assertEquals("seller01", read(row, "sellerUsername"));
        assertEquals(new BigDecimal("500000"), read(row, "startingPrice", "price"));
        assertEquals("APPROVED", read(row, "status"));
    }

    @Test
    void parseUsers_validData_returnsRows() {
        JSONArray data = new JSONArray("""
                [
                  {
                    "id": 3,
                    "username": "user01",
                    "fullname": "Nguyen Van A",
                    "email": "a@gmail.com",
                    "accountType": "SELLER",
                    "banned": true
                  }
                ]
                """);

        List<AdminUserRow> result = AdminResponseParser.parseUsers(data);

        assertEquals(1, result.size());
        Object row = result.get(0);

        assertEquals(3, read(row, "id", "userId"));
        assertEquals("user01", read(row, "username"));
        assertEquals("Nguyen Van A", read(row, "fullname", "fullName"));
        assertEquals("a@gmail.com", read(row, "email"));
        assertEquals("SELLER", read(row, "accountType", "role"));
        assertEquals(true, read(row, "banned"));
    }

    @Test
    void parseUsers_nullData_returnsEmptyList() {
        assertTrue(AdminResponseParser.parseUsers(null).isEmpty());
    }

    private Object read(Object target, String... names) {
        for (String name : names) {
            Object value = tryRead(target, name);
            if (value != null) {
                return value;
            }
        }

        fail("Không đọc được field/property: " + String.join(", ", names));
        return null;
    }

    private Object tryRead(Object target, String name) {
        String capitalized = name.substring(0, 1).toUpperCase() + name.substring(1);

        String[] methodNames = {
                name,
                name + "Property",
                "get" + capitalized,
                "is" + capitalized
        };

        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);

                if (value instanceof ObservableValue<?> observableValue) {
                    return observableValue.getValue();
                }

                return value;
            } catch (Exception ignored) {
            }
        }

        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(target);

            if (value instanceof ObservableValue<?> observableValue) {
                return observableValue.getValue();
            }

            return value;
        } catch (Exception ignored) {
        }

        return null;
    }
}