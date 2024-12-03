package com.example.projectapp;

import java.util.ArrayList;

public class DataClass {
    private String imageURL, documentURL, caption, userName, profileImageURL, uploadDateTime, accountNumber, uniquePostNumber;
    private int likeCount; // Field for storing like count
    private ArrayList<String> likedUsers; // Field for storing account numbers of liked users
    private long timestamp;

    private String documentFileName;



    // Default constructor to handle potential null values from Firebase
    public DataClass() {
        // Initialize likeCount to 0 and likedUsers to an empty list
        this.likeCount = 0;
        this.likedUsers = new ArrayList<>();
    }

    // Constructor to initialize all fields, including a null check for likedUsers
    public DataClass(String imageURL, String documentURL, String caption, String userName, String profileImageURL,
                     String uploadDateTime, String accountNumber, String uniquePostNumber,
                     ArrayList<String> likedUsers, long timestamp) {
        this.imageURL = imageURL;
        this.documentURL = documentURL;
        this.caption = caption;
        this.userName = userName;
        this.profileImageURL = profileImageURL;
        this.uploadDateTime = uploadDateTime;
        this.accountNumber = accountNumber;
        this.uniquePostNumber = uniquePostNumber;
        // Ensure likedUsers is never null, if it is, initialize it as an empty list
        this.likedUsers = (likedUsers != null) ? likedUsers : new ArrayList<>();
        this.timestamp = timestamp;
        this.documentFileName = documentFileName;
    }

    public String getDocumentFileName() {
        return documentFileName;
    }

    public void setDocumentFileName(String documentFileName) {
        this.documentFileName = documentFileName;
    }

    // Getter and Setter methods
    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getDocumentURL() {
        return documentURL;
    }

    public void setDocumentURL(String documentURL) {
        this.documentURL = documentURL;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getProfileImageURL() {
        return profileImageURL;
    }

    public void setProfileImageURL(String profileImageURL) {
        this.profileImageURL = profileImageURL;
    }

    public String getUploadDateTime() {
        return uploadDateTime;
    }

    public void setUploadDateTime(String uploadDateTime) {
        this.uploadDateTime = uploadDateTime;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getUniquePostNumber() {
        return uniquePostNumber;
    }

    public void setUniquePostNumber(String uniquePostNumber) {
        this.uniquePostNumber = uniquePostNumber;
    }

    public ArrayList<String> getLikedUsers() {
        return likedUsers;
    }

    public void setLikedUsers(ArrayList<String> likedUsers) {
        // Ensure likedUsers is never null
        this.likedUsers = (likedUsers != null) ? likedUsers : new ArrayList<>();
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Static method to generate a unique identifier for the post
    public static String getUniqueNumber() {
        // Using System.currentTimeMillis() to generate a unique number
        return String.valueOf(System.currentTimeMillis()); // Could replace with UUID for more guaranteed uniqueness
    }
}
