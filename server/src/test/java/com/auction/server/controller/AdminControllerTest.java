package com.auction.server.controller;

import com.auction.server.dto.ApiResponse;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.dto.UserResponseDTO;
import com.auction.server.service.AdminService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdminControllerTest {

    @Test
    void getPendingSessions_success_returnsSuccessResponse() {
        FakeAdminService service = new FakeAdminService();
        AdminController controller = new AdminController(service);

        SessionResponseDTO dto = new SessionResponseDTO();
        dto.setId(1);
        dto.setProductName("Laptop");
        service.pendingSessions = List.of(dto);

        ApiResponse<List<SessionResponseDTO>> response = controller.getPendingSessions();

        assertEquals(200, response.getStatus());
        assertEquals("Lấy danh sách phiên chờ duyệt thành công", response.getMessage());
        assertEquals(1, response.getData().size());
        assertEquals("Laptop", response.getData().get(0).getProductName());
    }

    @Test
    void getAllSessions_success_passesStatusToService() {
        FakeAdminService service = new FakeAdminService();
        AdminController controller = new AdminController(service);

        SessionResponseDTO dto = new SessionResponseDTO();
        dto.setId(2);
        service.allSessions = List.of(dto);

        ApiResponse<List<SessionResponseDTO>> response = controller.getAllSessions("ACTIVE");

        assertEquals(200, response.getStatus());
        assertEquals("Lấy danh sách phiên thành công", response.getMessage());
        assertEquals(1, response.getData().size());
        assertEquals("ACTIVE", service.lastStatus);
    }

    @Test
    void getSessionDetail_success_returnsSession() {
        FakeAdminService service = new FakeAdminService();
        AdminController controller = new AdminController(service);

        SessionResponseDTO dto = new SessionResponseDTO();
        dto.setId(5);
        service.detail = dto;

        ApiResponse<SessionResponseDTO> response = controller.getSessionDetail(5);

        assertEquals(200, response.getStatus());
        assertEquals("Lấy chi tiết phiên thành công", response.getMessage());
        assertSame(dto, response.getData());
        assertEquals(5, service.lastSessionId);
    }

    @Test
    void approveSession_success_returnsSuccessResponse() {
        FakeAdminService service = new FakeAdminService();
        AdminController controller = new AdminController(service);

        ApiResponse<Void> response = controller.approveSession(10, 1);

        assertEquals(200, response.getStatus());
        assertEquals("Phê duyệt thành công! Phiên đấu giá đã bắt đầu.", response.getMessage());
        assertNull(response.getData());
        assertEquals(10, service.lastSessionId);
        assertEquals(1, service.lastAdminId);
        assertTrue(service.approveCalled);
    }

    @Test
    void rejectSession_illegalArgument_returnsBadRequest() {
        FakeAdminService service = new FakeAdminService();
        AdminController controller = new AdminController(service);

        service.rejectException = new IllegalArgumentException("Vui lòng nhập lý do từ chối");

        ApiResponse<Void> response = controller.rejectSession(10, 1, "");

        assertEquals(400, response.getStatus());
        assertEquals("Vui lòng nhập lý do từ chối", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void rejectSession_success_passesReasonToService() {
        FakeAdminService service = new FakeAdminService();
        AdminController controller = new AdminController(service);

        ApiResponse<Void> response = controller.rejectSession(10, 1, "Sai thông tin");

        assertEquals(200, response.getStatus());
        assertEquals("Đã từ chối phiên đấu giá.", response.getMessage());
        assertEquals(10, service.lastSessionId);
        assertEquals(1, service.lastAdminId);
        assertEquals("Sai thông tin", service.lastReason);
        assertTrue(service.rejectCalled);
    }

    @Test
    void banUser_runtimeException_returnsServerError() {
        FakeAdminService service = new FakeAdminService();
        AdminController controller = new AdminController(service);

        service.banException = new RuntimeException("Không được khóa tài khoản Admin khác");

        ApiResponse<Void> response = controller.banUser(2, 1);

        assertEquals(500, response.getStatus());
        assertEquals("Không được khóa tài khoản Admin khác", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void cancelAuction_success_returnsSuccessResponse() {
        FakeAdminService service = new FakeAdminService();
        AdminController controller = new AdminController(service);

        ApiResponse<Void> response = controller.cancelAuction(20, 1);

        assertEquals(200, response.getStatus());
        assertEquals("Đã hủy phiên đấu giá.", response.getMessage());
        assertNull(response.getData());
        assertEquals(20, service.lastSessionId);
        assertEquals(1, service.lastAdminId);
        assertTrue(service.cancelCalled);
    }

    @Test
    void getAllUsers_success_passesRoleToService() {
        FakeAdminService service = new FakeAdminService();
        AdminController controller = new AdminController(service);

        UserResponseDTO user = new UserResponseDTO();
        user.setId(3);
        user.setUsername("seller01");
        service.users = List.of(user);

        ApiResponse<List<UserResponseDTO>> response = controller.getAllUsers("SELLER");

        assertEquals(200, response.getStatus());
        assertEquals("Lấy danh sách người dùng thành công", response.getMessage());
        assertEquals(1, response.getData().size());
        assertEquals("seller01", response.getData().get(0).getUsername());
        assertEquals("SELLER", service.lastRole);
    }

    private static class FakeAdminService extends AdminService {
        private List<SessionResponseDTO> pendingSessions = List.of();
        private List<SessionResponseDTO> allSessions = List.of();
        private List<UserResponseDTO> users = List.of();
        private SessionResponseDTO detail;

        private Integer lastSessionId;
        private Integer lastAdminId;
        private String lastStatus;
        private String lastReason;
        private String lastRole;

        private boolean approveCalled;
        private boolean rejectCalled;
        private boolean cancelCalled;

        private RuntimeException banException;
        private IllegalArgumentException rejectException;

        FakeAdminService() {
            super(null, null);
        }

        @Override
        public List<SessionResponseDTO> getPendingSessions() {
            return pendingSessions;
        }

        @Override
        public List<SessionResponseDTO> getAllSessions(String status) {
            this.lastStatus = status;
            return allSessions;
        }

        @Override
        public SessionResponseDTO getSessionDetail(Integer sessionId) {
            this.lastSessionId = sessionId;
            return detail;
        }

        @Override
        public List<UserResponseDTO> getAllUsers(String role) {
            this.lastRole = role;
            return users;
        }

        @Override
        public void approveSession(Integer sessionId, Integer adminId) {
            this.lastSessionId = sessionId;
            this.lastAdminId = adminId;
            this.approveCalled = true;
        }

        @Override
        public void rejectSession(Integer sessionId, Integer adminId, String reason) {
            if (rejectException != null) {
                throw rejectException;
            }

            this.lastSessionId = sessionId;
            this.lastAdminId = adminId;
            this.lastReason = reason;
            this.rejectCalled = true;
        }

        @Override
        public void banUser(Integer targetUserId, Integer adminId) {
            if (banException != null) {
                throw banException;
            }

            this.lastSessionId = targetUserId;
            this.lastAdminId = adminId;
        }

        @Override
        public void cancelAuction(Integer sessionId, Integer adminId) {
            this.lastSessionId = sessionId;
            this.lastAdminId = adminId;
            this.cancelCalled = true;
        }
    }
}