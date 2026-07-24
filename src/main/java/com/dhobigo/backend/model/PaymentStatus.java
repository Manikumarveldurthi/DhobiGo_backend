package com.dhobigo.backend.model;

/** Tracks real payment state separately from the order's pickup/delivery stage. */
public enum PaymentStatus {
    /** Order placed but online payment not yet confirmed (or Razorpay isn't configured — demo mode). */
    PENDING,
    /** Razorpay signature verified — money has actually moved. */
    PAID,
    /** Signature verification failed, or the gateway reported a failed payment. */
    FAILED,
    /** Cash on delivery — collected in person, never touches Razorpay. */
    COD
}
