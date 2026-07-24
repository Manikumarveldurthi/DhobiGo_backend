package com.dhobigo.backend.model;

/**
 * Matches STAGE_LABELS in tracking.js exactly, in the same order (0-6),
 * so the frontend's ordinal-based timeline rendering keeps working
 * once it switches from localStorage to this API.
 */
public enum OrderStage {
    PLACED,              // 0 - Order placed
    DHOBI_ON_THE_WAY,    // 1 - Dhobi on the way for pickup
    COLLECTED,           // 2 - Clothes collected
    WASHING,             // 3 - Washing / cleaning in progress
    IRONED_PACKED,       // 4 - Ironed & packed
    OUT_FOR_DELIVERY,    // 5 - Out for delivery
    DELIVERED;           // 6 - Delivered

    /** Matches STAGE_LABELS in the frontend's services-data.js — keep in sync. */
    public String label() {
        return switch (this) {
            case PLACED -> "Order placed";
            case DHOBI_ON_THE_WAY -> "Dhobi on the way for pickup";
            case COLLECTED -> "Clothes collected";
            case WASHING -> "Washing / cleaning in progress";
            case IRONED_PACKED -> "Ironed & packed";
            case OUT_FOR_DELIVERY -> "Out for delivery";
            case DELIVERED -> "Delivered";
        };
    }
}
