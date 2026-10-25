package com.foodbridge.model;

import java.time.LocalDateTime;

/**
 * Model representing a claimed food listing by an NGO.
 */
public class Claim {
    private int claimId;
    private int listingId;
    private int ngoId;
    private LocalDateTime claimedTime;

    public Claim() {
    }

    public Claim(int claimId, int listingId, int ngoId, LocalDateTime claimedTime) {
        this.claimId = claimId;
        this.listingId = listingId;
        this.ngoId = ngoId;
        this.claimedTime = claimedTime;
    }

    public int getClaimId() {
        return claimId;
    }

    public void setClaimId(int claimId) {
        this.claimId = claimId;
    }

    public int getListingId() {
        return listingId;
    }

    public void setListingId(int listingId) {
        this.listingId = listingId;
    }

    public int getNgoId() {
        return ngoId;
    }

    public void setNgoId(int ngoId) {
        this.ngoId = ngoId;
    }

    public LocalDateTime getClaimedTime() {
        return claimedTime;
    }

    public void setClaimedTime(LocalDateTime claimedTime) {
        this.claimedTime = claimedTime;
    }
}
