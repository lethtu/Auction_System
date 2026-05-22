package com.auction.server.util;

public class EmailTemplateBuilder {

    /**
     * Builds a beautiful, professional HTML email for successful user signup.
     */
    public static String buildWelcomeEmail(String fullname, String username) {
        String template = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\">\n" +
                "  <title>Chào mừng thành viên mới</title>\n" +
                "</head>\n" +
                "<body style=\"font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 40px 0; -webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale;\">\n"
                +
                "  <table width=\"100%%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #f1f5f9;\">\n"
                +
                "    <tr>\n" +
                "      <td align=\"center\">\n" +
                "        <table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); border: 1px solid #e2e8f0;\">\n"
                +
                "          <!-- Header -->\n" +
                "          <tr>\n" +
                "            <td style=\"background: linear-gradient(135deg, #1e293b 0%%, #0f172a 100%%); padding: 30px; text-align: center;\">\n"
                +
                "              <h1 style=\"color: #ffffff; margin: 0; font-size: 26px; font-weight: 700; letter-spacing: 2px;\">HƯNG YÊN AUCTION</h1>\n"
                +
                "              <p style=\"color: #94a3b8; margin: 5px 0 0 0; font-size: 14px; text-transform: uppercase; letter-spacing: 1px;\">Hệ Thống Đấu Giá Trực Tuyến</p>\n"
                +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- Content -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding: 40px 30px; background-color: #ffffff;\">\n" +
                "              <div style=\"text-align: center; margin-bottom: 25px;\">\n" +
                "                <span style=\"font-size: 50px;\">🎉</span>\n" +
                "              </div>\n" +
                "              <h2 style=\"color: #0f172a; margin-top: 0; font-size: 22px; font-weight: 600; text-align: center;\">Đăng Ký Tài Khoản Thành Công!</h2>\n"
                +
                "              <p style=\"color: #475569; font-size: 16px; line-height: 1.6; text-align: center; margin-bottom: 30px;\">\n"
                +
                "                Xin chào <strong>%s</strong>, chào mừng bạn tham gia hệ thống đấu giá trực tuyến. Tài khoản của bạn đã được khởi tạo thành công!\n"
                +
                "              </p>\n" +
                "              \n" +
                "              <!-- Info Box -->\n" +
                "              <table width=\"100%%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #f8fafc; border-radius: 8px; margin-bottom: 30px; border: 1px solid #e2e8f0;\">\n"
                +
                "                <tr>\n" +
                "                  <td style=\"padding: 20px;\">\n" +
                "                    <table width=\"100%%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
                "                      <tr>\n" +
                "                        <td width=\"35%%\" style=\"color: #64748b; font-size: 14px; padding-bottom: 10px;\"><strong>Tên tài khoản:</strong></td>\n"
                +
                "                        <td style=\"color: #0f172a; font-size: 14px; padding-bottom: 10px;\">%s</td>\n"
                +
                "                      </tr>\n" +
                "                      <tr>\n" +
                "                        <td style=\"color: #64748b; font-size: 14px;\"><strong>Trạng thái:</strong></td>\n"
                +
                "                        <td style=\"color: #10b981; font-size: 14px; font-weight: bold;\">Đang hoạt động (Kích hoạt)</td>\n"
                +
                "                      </tr>\n" +
                "                    </table>\n" +
                "                  </td>\n" +
                "                </tr>\n" +
                "              </table>\n" +
                "              \n" +
                "              <p style=\"color: #475569; font-size: 16px; line-height: 1.6; margin-bottom: 30px; text-align: center;\">\n"
                +
                "                Chúc bạn có những trải nghiệm đấu giá tuyệt vời và săn được nhiều sản phẩm giá trị!\n"
                +
                "              </p>\n" +
                "              \n" +
                "              <!-- CTA Button -->\n" +
                "              <div style=\"text-align: center;\">\n" +
                "                <a href=\"#\" style=\"background-color: #1e293b; color: #ffffff; text-decoration: none; padding: 12px 30px; border-radius: 6px; font-size: 16px; font-weight: bold; display: inline-block;\">Khám Phá Sàn Đấu Giá</a>\n"
                +
                "              </div>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- Footer -->\n" +
                "          <tr>\n" +
                "            <td style=\"background-color: #f8fafc; padding: 25px 30px; text-align: center; border-top: 1px solid #e2e8f0;\">\n"
                +
                "              <p style=\"color: #64748b; margin: 0; font-size: 13px; line-height: 1.5;\">\n" +
                "                Đây là email tự động từ hệ thống Đấu Giá. Vui lòng không phản hồi lại email này.\n" +
                "              </p>\n" +
                "              <p style=\"color: #94a3b8; margin: 8px 0 0 0; font-size: 12px;\">\n" +
                "                &copy; 2026 Auction System. All rights reserved.\n" +
                "              </p>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </table>\n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </table>\n" +
                "</body>\n" +
                "</html>";

        return String.format(template, fullname, username);
    }

    /**
     * Builds a beautiful, professional HTML email for password recovery.
     */
    public static String buildForgotPassEmail(String fullname, String otpCode) {
        String template = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\">\n" +
                "  <title>Mã Xác Minh Thiết Lập Lại Mật Khẩu</title>\n" +
                "</head>\n" +
                "<body style=\"font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 40px 0; -webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale;\">\n"
                +
                "  <table width=\"100%%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #f1f5f9;\">\n"
                +
                "    <tr>\n" +
                "      <td align=\"center\">\n" +
                "        <table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); border: 1px solid #e2e8f0;\">\n"
                +
                "          <!-- Header -->\n" +
                "          <tr>\n" +
                "            <td style=\"background: linear-gradient(135deg, #1e293b 0%%, #0f172a 100%%); padding: 30px; text-align: center;\">\n"
                +
                "              <h1 style=\"color: #ffffff; margin: 0; font-size: 26px; font-weight: 700; letter-spacing: 2px;\">HƯNG YÊN AUCTION</h1>\n"
                +
                "              <p style=\"color: #94a3b8; margin: 5px 0 0 0; font-size: 14px; text-transform: uppercase; letter-spacing: 1px;\">Hệ Thống Đấu Giá Trực Tuyến</p>\n"
                +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- Content -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding: 40px 30px; background-color: #ffffff;\">\n" +
                "              <h2 style=\"color: #0f172a; margin-top: 0; font-size: 20px; font-weight: 600;\">Xin chào %s,</h2>\n"
                +
                "              <p style=\"color: #475569; font-size: 16px; line-height: 1.6; margin-bottom: 24px;\">\n"
                +
                "                Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Vui lòng sử dụng mã xác minh dưới đây để tiến hành thiết lập lại mật khẩu:\n"
                +
                "              </p>\n" +
                "              \n" +
                "              <!-- Code Card -->\n" +
                "              <div style=\"text-align: center; margin: 30px 0;\">\n" +
                "                <div style=\"display: inline-block; background-color: #fef3c7; border: 2px dashed #f59e0b; padding: 15px 35px; border-radius: 8px;\">\n"
                +
                "                  <span style=\"font-family: 'Courier New', Courier, monospace; font-size: 32px; font-weight: bold; color: #d97706; letter-spacing: 6px;\">%s</span>\n"
                +
                "                </div>\n" +
                "              </div>\n" +
                "              \n" +
                "              <table width=\"100%%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #fffbeb; border-left: 4px solid #f59e0b; margin-bottom: 30px;\">\n"
                +
                "                <tr>\n" +
                "                  <td style=\"padding: 15px; font-size: 14px; color: #b45309; line-height: 1.5;\">\n" +
                "                    <strong>LƯU Ý BẢO MẬT:</strong> Tuyệt đối KHÔNG chia sẻ mã này với bất kỳ ai, kể cả nhân viên hệ thống. Mã xác minh này chỉ có hiệu lực sử dụng một lần.\n"
                +
                "                  </td>\n" +
                "                </tr>\n" +
                "              </table>\n" +
                "              \n" +
                "              <p style=\"color: #475569; font-size: 15px; line-height: 1.6; margin-bottom: 0;\">\n" +
                "                Nếu bạn không gửi yêu cầu này, vui lòng bỏ qua email này hoặc liên hệ với bộ phận hỗ trợ để bảo mật tài khoản.\n"
                +
                "              </p>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- Footer -->\n" +
                "          <tr>\n" +
                "            <td style=\"background-color: #f8fafc; padding: 25px 30px; text-align: center; border-top: 1px solid #e2e8f0;\">\n"
                +
                "              <p style=\"color: #64748b; margin: 0; font-size: 13px; line-height: 1.5;\">\n" +
                "                Đây là email tự động từ hệ thống Đấu Giá. Vui lòng không phản hồi lại email này.\n" +
                "              </p>\n" +
                "              <p style=\"color: #94a3b8; margin: 8px 0 0 0; font-size: 12px;\">\n" +
                "                &copy; 2026 Auction System. All rights reserved.\n" +
                "              </p>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </table>\n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </table>\n" +
                "</body>\n" +
                "</html>";

        return String.format(template, fullname, otpCode);
    }

    /**
     * Builds a beautiful, professional HTML email for successful Google registration.
     */
    public static String buildGoogleRegisterEmail(String fullname, String email, String regTime) {
        String template = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\">\n" +
                "  <title>Chào Mừng Bạn Đến Với BidPop</title>\n" +
                "</head>\n" +
                "<body style=\"font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 40px 0; -webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale;\">\n" +
                "  <table width=\"100%%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #f1f5f9;\">\n" +
                "    <tr>\n" +
                "      <td align=\"center\">\n" +
                "        <table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); border: 1px solid #e2e8f0;\">\n" +
                "          <!-- Header -->\n" +
                "          <tr>\n" +
                "            <td style=\"background: linear-gradient(135deg, #4285F4 0%%, #34A853 100%%); padding: 30px; text-align: center;\">\n" +
                "              <h1 style=\"color: #ffffff; margin: 0; font-size: 26px; font-weight: 700; letter-spacing: 2px;\">BIDPOP AUCTION</h1>\n" +
                "              <p style=\"color: #e2e8f0; margin: 5px 0 0 0; font-size: 14px; text-transform: uppercase; letter-spacing: 1px;\">Đăng Ký Tài Khoản Google Thành Công</p>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- Content -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding: 40px 30px; background-color: #ffffff;\">\n" +
                "              <h2 style=\"color: #0f172a; margin-top: 0; font-size: 20px; font-weight: 600;\">Xin chào %s,</h2>\n" +
                "              <p style=\"color: #475569; font-size: 16px; line-height: 1.6; margin-bottom: 24px;\">\n" +
                "                Chúc mừng bạn đã tạo thành công tài khoản BidPop thông qua liên kết Google Sign-In:\n" +
                "              </p>\n" +
                "              \n" +
                "              <!-- Details Table -->\n" +
                "              <table width=\"100%%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #f8fafc; border-radius: 8px; margin-bottom: 30px; border: 1px solid #e2e8f0;\">\n" +
                "                <tr>\n" +
                "                  <td style=\"padding: 20px;\">\n" +
                "                    <table width=\"100%%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
                "                      <tr>\n" +
                "                        <td width=\"40%%\" style=\"color: #64748b; font-size: 14px; padding-bottom: 10px;\"><strong>Email:</strong></td>\n" +
                "                        <td style=\"color: #0f172a; font-size: 14px; padding-bottom: 10px;\">%s</td>\n" +
                "                      </tr>\n" +
                "                      <tr>\n" +
                "                        <td style=\"color: #64748b; font-size: 14px; padding-bottom: 10px;\"><strong>Phương thức:</strong></td>\n" +
                "                        <td style=\"color: #4285F4; font-size: 14px; font-weight: bold; padding-bottom: 10px;\">Google OAuth 2.0</td>\n" +
                "                      </tr>\n" +
                "                      <tr>\n" +
                "                        <td style=\"color: #64748b; font-size: 14px;\"><strong>Thời gian:</strong></td>\n" +
                "                        <td style=\"color: #0f172a; font-size: 14px;\">%s</td>\n" +
                "                      </tr>\n" +
                "                    </table>\n" +
                "                  </td>\n" +
                "                </tr>\n" +
                "              </table>\n" +
                "              \n" +
                "              <p style=\"color: #475569; font-size: 15px; line-height: 1.6; margin-bottom: 0;\">\n" +
                "                Để đảm bảo an toàn tối đa cho tài khoản, vui lòng truy cập vào phần Cài đặt trong ứng dụng để thiết lập mật khẩu riêng cho lần đầu tiên.\n" +
                "              </p>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- Footer -->\n" +
                "          <tr>\n" +
                "            <td style=\"background-color: #f8fafc; padding: 25px 30px; text-align: center; border-top: 1px solid #e2e8f0;\">\n" +
                "              <p style=\"color: #64748b; margin: 0; font-size: 13px; line-height: 1.5;\">\n" +
                "                Đây là email tự động từ hệ thống Đấu Giá. Vui lòng không phản hồi lại email này.\n" +
                "              </p>\n" +
                "              <p style=\"color: #94a3b8; margin: 8px 0 0 0; font-size: 12px;\">\n" +
                "                &copy; 2026 Auction System. All rights reserved.\n" +
                "              </p>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </table>\n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </table>\n" +
                "</body>\n" +
                "</html>";
        return String.format(template, fullname, email, regTime);
    }
}
