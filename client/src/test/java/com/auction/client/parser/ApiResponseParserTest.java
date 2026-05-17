package com.auction.client.parser;

import com.auction.client.dto.ApiResult;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseParserTest {

    @Test
    void parseApiResponse_successBody_returnsSuccessResult() {
        String body = """
                {
                  "status": 200,
                  "message": "OK"
                }
                """;

        ApiResult<Void> result = ApiResponseParser.parseApiResponse(body, 200, "Thành công");

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("OK", result.message);
        assertNull(result.data);
    }

    @Test
    void parseApiResponse_emptySuccessBody_usesDefaultSuccessMessage() {
        ApiResult<Void> result = ApiResponseParser.parseApiResponse("", 200, "Duyệt thành công");

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("Duyệt thành công", result.message);
        assertNull(result.data);
    }

    @Test
    void parseApiResponse_errorBody_returnsErrorResult() {
        String body = """
                {
                  "status": 400,
                  "message": "Dữ liệu không hợp lệ"
                }
                """;

        ApiResult<Void> result = ApiResponseParser.parseApiResponse(body, 200, "Thành công");

        assertFalse(result.success);
        assertEquals(400, result.status);
        assertEquals("Dữ liệu không hợp lệ", result.message);
        assertNull(result.data);
    }

    @Test
    void parseApiResponse_emptyErrorBody_usesDefaultErrorMessage() {
        ApiResult<Void> result = ApiResponseParser.parseApiResponse(null, 500, "Thành công");

        assertFalse(result.success);
        assertEquals(500, result.status);
        assertEquals("Thao tác thất bại.", result.message);
        assertNull(result.data);
    }

    @Test
    void extractDataArray_successWithArray_returnsArray() {
        String body = """
                {
                  "status": 200,
                  "message": "Lấy dữ liệu thành công.",
                  "data": [
                    {"id": 1},
                    {"id": 2}
                  ]
                }
                """;

        ApiResult<JSONArray> result = ApiResponseParser.extractDataArray(body, 200);

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("Lấy dữ liệu thành công.", result.message);
        assertEquals(2, result.data.length());
        assertEquals(1, result.data.getJSONObject(0).getInt("id"));
    }

    @Test
    void extractDataArray_successWithoutData_returnsEmptyArray() {
        String body = """
                {
                  "status": 200,
                  "message": "OK"
                }
                """;

        ApiResult<JSONArray> result = ApiResponseParser.extractDataArray(body, 200);

        assertTrue(result.success);
        assertEquals(200, result.status);
        assertEquals("OK", result.message);
        assertNotNull(result.data);
        assertEquals(0, result.data.length());
    }

    @Test
    void extractDataArray_errorStatus_returnsEmptyArray() {
        String body = """
                {
                  "status": 403,
                  "message": "Không có quyền"
                }
                """;

        ApiResult<JSONArray> result = ApiResponseParser.extractDataArray(body, 200);

        assertFalse(result.success);
        assertEquals(403, result.status);
        assertEquals("Không có quyền", result.message);
        assertNotNull(result.data);
        assertEquals(0, result.data.length());
    }

    @Test
    void extractDataArray_emptyBody_returnsNoDataMessage() {
        ApiResult<JSONArray> result = ApiResponseParser.extractDataArray("", 200);

        assertFalse(result.success);
        assertEquals(200, result.status);
        assertEquals("Không có dữ liệu từ server.", result.message);
        assertNotNull(result.data);
        assertEquals(0, result.data.length());
    }
}