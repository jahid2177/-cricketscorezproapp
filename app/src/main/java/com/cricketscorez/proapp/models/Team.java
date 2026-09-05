package com.cricketscorez.proapp.models;

import com.google.gson.annotations.SerializedName;

public class Team {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("short_name")
    private String shortName;

    @SerializedName("logo_url")
    private String logoUrl;

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getShortName() { return shortName; }
    public String getLogoUrl() { return logoUrl; }
}
