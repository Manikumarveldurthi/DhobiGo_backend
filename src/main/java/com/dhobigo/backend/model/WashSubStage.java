package com.dhobigo.backend.model;

/**
 * Garment-level micro-status shown only while an order's OrderStage is
 * WASHING — gives customers a finer-grained view than the 7 big stages
 * alone (e.g. "washing" vs "drying" vs "folding"), Swiggy/Zomato-"chef is
 * preparing your order"-style. Purely additive: existing orders/flows
 * that never set this just show NONE and behave exactly as before.
 */
public enum WashSubStage {
    NONE,
    WASHING,
    DRYING,
    FOLDING;

    public String label() {
        return switch (this) {
            case NONE -> "";
            case WASHING -> "Washing in the machine";
            case DRYING -> "Drying";
            case FOLDING -> "Folding & packing";
        };
    }
}
