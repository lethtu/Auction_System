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

    private static String avatarUrl;

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
        balance = BigDecimal.ZERO;
        watchlistIds.clear();
        if (username != null) {
            watchlistIds.addAll(com.auction.client.service.ClientLogger.loadUserFavorites(username));
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
    }

    public static void setAvatarUrl(String AvatarUrl) {
        avatarUrl = AvatarUrl;
    }

    public static void clearSession(){
        id = null;
        username = null;
        fullname = null;
        email = null;
        dob = null;
        place_of_birth = null;
        role = null;
        balance = BigDecimal.ZERO;
        avatarUrl = null;
        watchlistIds.clear();
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

    public static String getAvatarUrl() {
        return avatarUrl;
    }
}
