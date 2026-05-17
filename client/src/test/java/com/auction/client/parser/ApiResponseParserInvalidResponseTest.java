package com.auction.client.parser;

import com.auction.client.dto.ApiResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseParserInvalidResponseTest {

    @Test
    void parseApiResponse_malformedJson_returnsInvalidResponseMessage() {
        ApiResult<Void> result = ApiResponseParser.parseApiResponse(
                "{bad json",
                200,
                "Thành công"
        );

        assertFalse(result.success);
        assertEquals(200, result.status);
        assertEquals("Phản hồi từ server không hợp lệ.", result.message);
        assertNull(result.data);
    }

    @Test
    void extractDataArray_malformedJson_returnsEmptyArrayAndInvalidResponseMessage() {
        ApiResult<JSONArray> result = ApiResponseParser.extractDataArray("{bad json", 200);

        assertFalse(result.success);
        assertEquals(200, result.status);
        assertEquals("Phản hồi từ server không hợp lệ.", result.message);
        assertNotNull(result.data);
        assertEquals(0, result.data.length());
    }

    @Test
    void extractDataObject_successWithObject_returnsObject() {
        String body = """
                {
                  "status": 200,
                  "message": "OK",
                  "data": {
                    "imagePath": "upload/images/item.png"
                  }
                }
                """;

        ApiResult<JSONObject> result = ApiResponseParser.extractDataObject(body, 200);

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("OK", result.message);
        assertNotNull(result.data);
        assertEquals("upload/images/item.png", result.data.getString("imagePath"));
    }

    @Test
    void extractDataObject_errorStatus_returnsEmptyObject() {
        String body = """
                {
                  "status": 500,
                  "message": "Server lỗi"
                }
                """;

        ApiResult<JSONObject> result = ApiResponseParser.extractDataObject(body, 500);

        assertFalse(result.success);
        assertEquals(500, result.status);
        assertEquals("Server lỗi", result.message);
        assertNotNull(result.data);
        assertEquals(0, result.data.length());
    }
}