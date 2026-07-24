package com.dhobigo.backend.model;

/**
 * Tracks whether the currently-assigned dhobi has actually agreed to take
 * this order — separate from the pickup/delivery OrderStage, since a dhobi
 * can decline before ever starting the stage timeline.
 */
public enum AcceptanceStatus {
    /** Assigned but the dhobi hasn't responded yet — dhobi.html shows Accept/Decline. */
    PENDING,
    /** Dhobi accepted — normal stage-advancing flow proceeds. */
    ACCEPTED,
    /** Dhobi declined — order.dhobi is cleared and the customer is asked to pick someone else. */
    DECLINED
}
