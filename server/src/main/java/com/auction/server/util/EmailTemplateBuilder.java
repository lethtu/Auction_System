package com.auction.server.util;

public class EmailTemplateBuilder {

    public static String buildWelcomeEmail(String fullname, String username) {
        String safeName = html(isBlank(fullname) ? "BidPop user" : fullname);
        String safeUsername = html(isBlank(username) ? "your account" : username);

        return officialTemplate(
                "Account Registration Successful!",
                "Hi <strong>" + safeName + "</strong>, welcome to BidPop. Your auction account has been created successfully.",
                "Account",
                safeUsername,
                "Status",
                "Active",
                "You can now explore live auctions, place bids, and manage your account in BidPop.",
                "Explore Auctions"
        );
    }

    public static String buildGoogleRegisterEmail(String fullname, String email, String regTime) {
        String safeName = html(isBlank(fullname) ? "BidPop user" : fullname);
        String safeEmail = html(isBlank(email) ? "your Google account" : email);
        String safeTime = html(isBlank(regTime) ? "Just now" : regTime);

        return officialTemplate(
                "Google Registration Successful!",
                "Hi <strong>" + safeName + "</strong>, your BidPop account has been created using Google sign-in.",
                "Email",
                safeEmail,
                "Registration time",
                safeTime,
                "You can now explore live auctions, place bids, and manage your account in BidPop.",
                "Explore Auctions"
        );
    }

    public static String buildForgotPassEmail(String fullname, String otpCode) {
        String safeName = html(isBlank(fullname) ? "BidPop user" : fullname);
        String safeCode = html(isBlank(otpCode) ? "------" : otpCode);

        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "  <title>Password Reset Verification Code</title>\n"
                + "</head>\n"
                + "<body style=\"font-family: Arial, Helvetica, sans-serif; background-color: #f1f5f9; margin: 0; padding: 40px 0; color: #334155;\">\n"
                + "  <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #f1f5f9;\">\n"
                + "    <tr><td align=\"center\">\n"
                + "      <table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08); border: 1px solid #e2e8f0;\">\n"
                + headerBlock()
                + "        <tr><td style=\"padding: 40px 30px; background-color: #ffffff;\">\n"
                + "          <h2 style=\"color: #0f172a; margin-top: 0; font-size: 20px; font-weight: 700;\">Hi " + safeName + ",</h2>\n"
                + "          <p style=\"color: #475569; font-size: 16px; line-height: 1.6; margin-bottom: 24px;\">We received a request to reset your password. Use the verification code below to continue.</p>\n"
                + "          <div style=\"text-align: center; margin: 30px 0;\">\n"
                + "            <div style=\"display: inline-block; background-color: #fef3c7; border: 2px dashed #f59e0b; padding: 15px 35px; border-radius: 8px;\">\n"
                + "              <span style=\"font-family: 'Courier New', Courier, monospace; font-size: 32px; font-weight: bold; color: #d97706; letter-spacing: 6px;\">" + safeCode + "</span>\n"
                + "            </div>\n"
                + "          </div>\n"
                + "          <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #fffbeb; border-left: 4px solid #f59e0b; margin-bottom: 30px;\">\n"
                + "            <tr><td style=\"padding: 15px; font-size: 14px; color: #b45309; line-height: 1.5;\"><strong>Security note:</strong> Do not share this code with anyone. BidPop staff will never ask for your verification code.</td></tr>\n"
                + "          </table>\n"
                + "          <p style=\"color: #475569; font-size: 15px; line-height: 1.6; margin-bottom: 0;\">If you did not request this, you can safely ignore this email.</p>\n"
                + "        </td></tr>\n"
                + footerBlock()
                + "      </table>\n"
                + "    </td></tr>\n"
                + "  </table>\n"
                + "</body>\n"
                + "</html>";
    }

    private static String officialTemplate(
            String title,
            String greetingHtml,
            String firstLabel,
            String firstValue,
            String secondLabel,
            String secondValue,
            String description,
            String buttonText
    ) {
        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "  <title>Welcome to BidPop</title>\n"
                + "</head>\n"
                + "<body style=\"font-family: Arial, Helvetica, sans-serif; background-color: #f1f5f9; margin: 0; padding: 40px 0; color: #334155;\">\n"
                + "  <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #f1f5f9;\">\n"
                + "    <tr><td align=\"center\">\n"
                + "      <table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08); border: 1px solid #e2e8f0;\">\n"
                + headerBlock()
                + "        <tr><td style=\"padding: 40px 30px; background-color: #ffffff;\">\n"
                + "          <div style=\"text-align: center; margin-bottom: 25px;\"><span style=\"font-size: 50px;\">&#127881;</span></div>\n"
                + "          <h2 style=\"color: #0f172a; margin-top: 0; font-size: 22px; font-weight: 700; text-align: center;\">" + html(title) + "</h2>\n"
                + "          <p style=\"color: #475569; font-size: 16px; line-height: 1.6; text-align: center; margin-bottom: 30px;\">" + greetingHtml + "</p>\n"
                + "          <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #f8fafc; border-radius: 8px; margin-bottom: 30px; border: 1px solid #e2e8f0;\">\n"
                + "            <tr><td style=\"padding: 20px;\">\n"
                + "              <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n"
                + "                <tr><td width=\"35%\" style=\"color: #64748b; font-size: 14px; padding-bottom: 10px;\"><strong>" + html(firstLabel) + ":</strong></td><td style=\"color: #0f172a; font-size: 14px; padding-bottom: 10px;\">" + firstValue + "</td></tr>\n"
                + "                <tr><td style=\"color: #64748b; font-size: 14px;\"><strong>" + html(secondLabel) + ":</strong></td><td style=\"color: #10b981; font-size: 14px; font-weight: bold;\">" + secondValue + "</td></tr>\n"
                + "              </table>\n"
                + "            </td></tr>\n"
                + "          </table>\n"
                + "          <p style=\"color: #475569; font-size: 16px; line-height: 1.6; margin-bottom: 30px; text-align: center;\">" + html(description) + "</p>\n"
                + "          <div style=\"text-align: center;\"><a href=\"#\" style=\"background-color: #1e293b; color: #ffffff; text-decoration: none; padding: 12px 30px; border-radius: 6px; font-size: 16px; font-weight: bold; display: inline-block;\">" + html(buttonText) + "</a></div>\n"
                + "        </td></tr>\n"
                + footerBlock()
                + "      </table>\n"
                + "    </td></tr>\n"
                + "  </table>\n"
                + "</body>\n"
                + "</html>";
    }

    private static String headerBlock() {
        return "        <tr>\n"
                + "          <td style=\"background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%); padding: 30px; text-align: center;\">\n"
                + "            <h1 style=\"color: #ffffff; margin: 0; font-size: 26px; font-weight: 700; letter-spacing: 2px;\">BIDPOP AUCTION</h1>\n"
                + "            <p style=\"color: #94a3b8; margin: 8px 0 0 0; font-size: 14px; text-transform: uppercase; letter-spacing: 1px;\">Online Auction Platform</p>\n"
                + "          </td>\n"
                + "        </tr>\n";
    }

    private static String footerBlock() {
        return "        <tr>\n"
                + "          <td style=\"background-color: #f8fafc; padding: 25px 30px; text-align: center; border-top: 1px solid #e2e8f0;\">\n"
                + "            <p style=\"color: #64748b; margin: 0; font-size: 13px; line-height: 1.5;\">This is an automated email from BidPop. Please do not reply to this email.</p>\n"
                + "            <p style=\"color: #94a3b8; margin: 8px 0 0 0; font-size: 12px;\">&copy; 2026 Auction System. All rights reserved.</p>\n"
                + "          </td>\n"
                + "        </tr>\n";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String html(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}