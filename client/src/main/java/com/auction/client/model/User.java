package com.auction.client.model;

import java.math.BigDecimal;

public class User {
    private static Integer id;

    private static String username;

    private static String fullname;

    private static String email;

    private static String dob;

    private static String place_of_birth;

    private static String role;

    private static BigDecimal balance = BigDecimal.ZERO;

    private static BigDecimal currentMoney = BigDecimal.ZERO;

    private static BigDecimal pendingMoney = BigDecimal.ZERO;

    private static String avatarUrl;

    private static String sessionToken;

    private static boolean passwordSet = true;

    private static javafx.scene.image.Image cachedAvatarImage;
    private static String cachedAvatarUrl;

    public static javafx.scene.image.Image getCachedAvatarImage() {
        return cachedAvatarImage;
    }

    public static void setCachedAvatarImage(javafx.scene.image.Image img, String url) {
        cachedAvatarImage = img;
        cachedAvatarUrl = url;
    }

    public static String getCachedAvatarUrl() {
        return cachedAvatarUrl;
    }

    public static String getSessionToken() {
        return sessionToken;
    }

    public static void setSessionToken(String token) {
        sessionToken = token;
    }

    public static final java.util.Set<Integer> watchlistIds = new java.util.concurrent.ConcurrentSkipListSet<>();

    public static void setSession(Integer Id, String Username, String Fullname, String Email, String Dob, String Place_of_birth, String Role, String AvatarUrl){
        id = Id;
        username = Username;
        fullname = Fullname;
        email = Email;
        dob = Dob;
        place_of_birth = Place_of_birth;
        role = Role;
        avatarUrl = AvatarUrl;
        passwordSet = true;
        balance = BigDecimal.ZERO;
        currentMoney = BigDecimal.ZERO;
        pendingMoney = BigDecimal.ZERO;
        watchlistIds.clear();
        if (username != null) {
            watchlistIds.addAll(com.auction.client.service.ClientLogger.loadUserFavorites(username));
        }
        try {
            com.auction.client.service.NotificationCenterService.getInstance().switchUser(Id);
        } catch (Exception ignored) {
        }
    }

    public static void updateProfile(String Username, String Fullname, String Email, String Dob, String Place_of_birth){
        username = Username;
        fullname = Fullname;
        email = Email;
        dob = Dob;
        place_of_birth = Place_of_birth;
    }

    public static void updateProfile(String Username, String Fullname, String Email, String Dob, String Place_of_birth, BigDecimal Balance){
        updateProfile(Username, Fullname, Email, Dob, Place_of_birth);
        setBalance(Balance);
    }

    public static void updateProfile(String Username, String Fullname, String Email, String Dob, String Place_of_birth, BigDecimal Balance, String AvatarUrl){
        updateProfile(Username, Fullname, Email, Dob, Place_of_birth, Balance);
        avatarUrl = AvatarUrl;
    }

    public static void setBalance(BigDecimal Balance) {
        balance = Balance == null ? BigDecimal.ZERO : Balance;
        if (currentMoney == null || currentMoney.compareTo(BigDecimal.ZERO) == 0) {
            currentMoney = balance;
        }
    }

    public static void setWalletSummary(BigDecimal balanceFromServer, BigDecimal currentFromServer, BigDecimal pendingFromServer) {
        balance = balanceFromServer == null ? BigDecimal.ZERO : balanceFromServer;
        currentMoney = balance;
        pendingMoney = pendingFromServer == null ? BigDecimal.ZERO : pendingFromServer;
    }

    public static void setAvatarUrl(String AvatarUrl) {
        avatarUrl = AvatarUrl;
    }

    public static void clearSession(){
        try {
            com.auction.client.service.NotificationCenterService.getInstance().switchUser(null);
        } catch (Exception ignored) {
        }
        id = null;
        username = null;
        fullname = null;
        email = null;
        dob = null;
        place_of_birth = null;
        role = null;
        balance = BigDecimal.ZERO;
        currentMoney = BigDecimal.ZERO;
        pendingMoney = BigDecimal.ZERO;
        avatarUrl = null;
        sessionToken = null;
        passwordSet = true;
        watchlistIds.clear();
        cachedAvatarImage = null;
        cachedAvatarUrl = null;
        try {
            com.auction.client.service.NotificationSocketService.getInstance().stop();
        } catch (Exception ignored) {
        }
    }


    public static boolean isPasswordSet() {
        return passwordSet;
    }

    public static void setPasswordSet(boolean value) {
        passwordSet = value;
    }

    public static String getRole(){
        return role;
    }

    public static Integer getId(){
        return id;
    }

    public static String getFullname(){
        return fullname;
    }

    public static String getEmail(){
        return email;
    }

    public static String getDob(){
        return dob;
    }

    public static String getPlace_of_birth(){
        return place_of_birth;
    }

    public static String getUsername(){
        return username;
    }

    public static BigDecimal getBalance() {
        return balance == null ? BigDecimal.ZERO : balance;
    }

    public static BigDecimal getTotalMoney() {
        return getBalance().add(getPendingMoney());
    }

    public static BigDecimal getCurrentMoney() {
        return getBalance();
    }

    public static BigDecimal getPendingMoney() {
        return pendingMoney == null ? BigDecimal.ZERO : pendingMoney;
    }

    public static String getAvatarUrl() {
        return avatarUrl;
    }
}
