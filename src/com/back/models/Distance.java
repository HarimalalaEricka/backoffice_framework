package com.app.models;

public class Distance {

    private int idDistance;
    private int fromHotelId;
    private int toHotelId;
    private int distanceKm;

    // Constructors
    public Distance() {}

    public Distance(int idDistance, int fromHotelId, int toHotelId, int distanceKm) {
        this.idDistance = idDistance;
        this.fromHotelId = fromHotelId;
        this.toHotelId = toHotelId;
        this.distanceKm = distanceKm;
    }

    // Getters and Setters
    public int getIdDistance() {
        return idDistance;
    }

    public void setIdDistance(int idDistance) {
        this.idDistance = idDistance;
    }

    public int getFromHotelId() {
        return fromHotelId;
    }

    public void setFromHotelId(int fromHotelId) {
        this.fromHotelId = fromHotelId;
    }

    public int getToHotelId() {
        return toHotelId;
    }

    public void setToHotelId(int toHotelId) {
        this.toHotelId = toHotelId;
    }

    public int getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(int distanceKm) {
        this.distanceKm = distanceKm;
    }
}
