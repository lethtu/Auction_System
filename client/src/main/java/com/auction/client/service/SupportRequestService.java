package com.auction.client.service;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SupportRequestService {
    public static final String SUPPORT_EMAIL = "lethanhtungbot01@gmail.com";

    public boolean openSupportEmailDraft(String contactEmail, String subject, String message) throws IOException {
        if (!canUseDesktopMail()) {
            return false;
        }

        Desktop.getDesktop().mail(buildMailtoUri(contactEmail, subject, message));
        return true;
    }

    URI buildMailtoUri(String contactEmail, String subject, String message) {
        String normalizedEmail = normalize(contactEmail);
        String normalizedSubject = normalize(subject);
        String normalizedMessage = normalize(message);

        String mailSubject = "[Auction Support] " + normalizedSubject;
        String mailBody = "Contact email: " + normalizedEmail + "\n\n" + normalizedMessage;

        return URI.create("mailto:" + SUPPORT_EMAIL
                + "?subject=" + encode(mailSubject)
                + "&body=" + encode(mailBody));
    }

    public String buildManualSendInstruction(String subject) {
        return "No email app is available on this device. Please send your support request manually to "
                + SUPPORT_EMAIL
                + " with subject: "
                + normalize(subject);
    }

    private boolean canUseDesktopMail() {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL);
    }

    private String encode(String value) {
        return URLEncoder.encode(normalize(value), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
