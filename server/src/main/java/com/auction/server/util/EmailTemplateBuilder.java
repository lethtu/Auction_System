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
                "  <title>Welcome to BidPop!</title>\n" +
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
                "              <h1 style=\"color: #ffffff; margin: 0; font-size: 26px; font-weight: 700; letter-spacing: 2px;\">BIDPOP AUCTION</h1>\n"
                +
                "              <p style=\"color: #94a3b8; margin: 5px 0 0 0; font-size: 14px; text-transform: uppercase; letter-spacing: 1px;\">Online Auction Platform</p>\n"
                +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- Content -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding: 40px 30px; background-color: #ffffff;\">\n" +
                "              <div style=\"text-align: center; margin-bottom: 25px;\">\n" +
                "                <span style=\"font-size: 50px;\">🎉</span>\n" +
                "              </div>\n" +
                "              <h2 style=\"color: #0f172a; margin-top: 0; font-size: 22px; font-weight: 600; text-align: center;\">Account Registration Successful!</h2>\n"
                +
                "              <p style=\"color: #475569; font-size: 16px; line-height: 1.6; text-align: center; margin-bottom: 30px;\">\n"
                +
                "                Hello <strong>%s</strong>, welcome to BidPop online auction platform. Your account has been successfully initialized!\n"
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
                "                        <td width=\"35%%\" style=\"color: #64748b; font-size: 14px; padding-bottom: 10px;\"><strong>Username:</strong></td>\n"
                +
                "                        <td style=\"color: #0f172a; font-size: 14px; padding-bottom: 10px;\">%s</td>\n"
                +
                "                      </tr>\n" +
                "                      <tr>\n" +
                "                        <td style=\"color: #64748b; font-size: 14px;\"><strong>Status:</strong></td>\n"
                +
                "                        <td style=\"color: #10b981; font-size: 14px; font-weight: bold;\">Active (Verified)</td>\n"
                +
                "                      </tr>\n" +
                "                    </table>\n" +
                "                  </td>\n" +
                "                </tr>\n" +
                "              </table>\n" +
                "              \n" +
                "              <p style=\"color: #475569; font-size: 16px; line-height: 1.6; margin-bottom: 30px; text-align: center;\">\n"
                +
                "                We wish you an outstanding bidding experience and hope you win many valuable products!\n"
                +
                "              </p>\n" +
                "              \n" +
                "              <!-- CTA Button -->\n" +
                "              <div style=\"text-align: center;\">\n" +
                "                <a href=\"#\" style=\"background-color: #1e293b; color: #ffffff; text-decoration: none; padding: 12px 30px; border-radius: 6px; font-size: 16px; font-weight: bold; display: inline-block;\">Explore Marketplace</a>\n"
                +
                "              </div>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- Footer -->\n" +
                "          <tr>\n" +
                "            <td style=\"background-color: #f8fafc; padding: 25px 30px; text-align: center; border-top: 1px solid #e2e8f0;\">\n"
                +
                "              <p style=\"color: #64748b; margin: 0; font-size: 13px; line-height: 1.5;\">\n" +
                "                This is an automated email from the system. Please do not reply to this email.\n" +
                "              </p>\n" +
                "              <p style=\"color: #94a3b8; margin: 8px 0 0 0; font-size: 12px;\">\n" +
                "                &copy; 2026 BidPop System. All rights reserved.\n" +
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
                "  <title>Password Reset Verification Code</title>\n" +
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
                "              <h1 style=\"color: #ffffff; margin: 0; font-size: 26px; font-weight: 700; letter-spacing: 2px;\">BIDPOP AUCTION</h1>\n"
                +
                "              <p style=\"color: #94a3b8; margin: 5px 0 0 0; font-size: 14px; text-transform: uppercase; letter-spacing: 1px;\">Online Auction Platform</p>\n"
                +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- Content -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding: 40px 30px; background-color: #ffffff;\">\n" +
                "              <h2 style=\"color: #0f172a; margin-top: 0; font-size: 20px; font-weight: 600;\">Hello %s,</h2>\n"
                +
                "              <p style=\"color: #475569; font-size: 16px; line-height: 1.6; margin-bottom: 24px;\">\n"
                +
                "                We received a request to reset the password for your account. Please use the verification code below to proceed with setting up a new password:\n"
                +
                "              </p>\n" +
                "              \n" +
                "              <!-- Code Card -->\n" +
                "              <div style=\"text-align: center; margin: 30px 0;\">\n" +
                "                <div style=\"display: inline-block; background-color: #fffbeb; border: 2px dashed #f59e0b; padding: 15px 35px; border-radius: 8px;\">\n"
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
                "                    <strong>SECURITY NOTICE:</strong> Do NOT share this code with anyone, including support staff. This verification code can only be used once.\n"
                +
                "                  </td>\n" +
                "                </tr>\n" +
                "              </table>\n" +
                "              \n" +
                "              <p style=\"color: #475569; font-size: 15px; line-height: 1.6; margin-bottom: 0;\">\n" +
                "                If you did not request this change, please ignore this email or contact support to secure your account.\n"
                +
                "              </p>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- Footer -->\n" +
                "          <tr>\n" +
                "            <td style=\"background-color: #f8fafc; padding: 25px 30px; text-align: center; border-top: 1px solid #e2e8f0;\">\n"
                +
                "              <p style=\"color: #64748b; margin: 0; font-size: 13px; line-height: 1.5;\">\n" +
                "                This is an automated email from the system. Please do not reply to this email.\n" +
                "              </p>\n" +
                "              <p style=\"color: #94a3b8; margin: 8px 0 0 0; font-size: 12px;\">\n" +
                "                &copy; 2026 BidPop System. All rights reserved.\n" +
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
        return buildWelcomeEmail(fullname, email);
    }
}
