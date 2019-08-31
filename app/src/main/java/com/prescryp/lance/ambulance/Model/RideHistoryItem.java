package com.prescryp.lance.ambulance.Model;

public class RideHistoryItem {
    private String rideId;
    private Long timestamp;
    private String dateOfRide;
    private String customerProfileImageUrl;
    private String customerName;
    private String rideRating;
    private String distance;
    private String pickupLocationName;
    private String destinationLocationName;

    public RideHistoryItem(String rideId, Long timestamp, String dateOfRide, String customerProfileImageUrl, String customerName, String rideRating, String distance, String pickupLocationName, String destinationLocationName) {
        this.rideId = rideId;
        this.timestamp = timestamp;
        this.dateOfRide = dateOfRide;
        this.customerProfileImageUrl = customerProfileImageUrl;
        this.customerName = customerName;
        this.rideRating = rideRating;
        this.distance = distance;
        this.pickupLocationName = pickupLocationName;
        this.destinationLocationName = destinationLocationName;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDateOfRide() {
        return dateOfRide;
    }

    public void setDateOfRide(String dateOfRide) {
        this.dateOfRide = dateOfRide;
    }

    public String getCustomerProfileImageUrl() {
        return customerProfileImageUrl;
    }

    public void setCustomerProfileImageUrl(String customerProfileImageUrl) {
        this.customerProfileImageUrl = customerProfileImageUrl;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getRideRating() {
        return rideRating;
    }

    public void setRideRating(String rideRating) {
        this.rideRating = rideRating;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getPickupLocationName() {
        return pickupLocationName;
    }

    public void setPickupLocationName(String pickupLocationName) {
        this.pickupLocationName = pickupLocationName;
    }

    public String getDestinationLocationName() {
        return destinationLocationName;
    }

    public void setDestinationLocationName(String destinationLocationName) {
        this.destinationLocationName = destinationLocationName;
    }
}
