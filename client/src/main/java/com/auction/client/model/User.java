package com.auction.client.model;

public class User {
    private static Integer id;

    private static String username;

    private static String fullname;

    private static String email;

    private static String dob;

    private static String place_of_birth;

    private static String role;

    public static final java.util.Set<Integer> watchlistIds = new java.util.concurrent.ConcurrentSkipListSet<>();

    public static void setSession(Integer Id, String Username, String Fullname, String Email, String Dob, String Place_of_birth, String Role){
        id = Id;
        username = Username;
        fullname = Fullname;
        email = Email;
        dob = Dob;
        place_of_birth = Place_of_birth;
        role = Role;
        watchlistIds.clear();
        if (username != null) {
            watchlistIds.addAll(com.auction.client.service.ClientLogger.loadUserFavorites(username));
        }
    }

    public static void clearSession(){
        id = null;
        username = null;
        fullname = null;
        email = null;
        dob = null;
        place_of_birth = null;
        role = null;
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
}
