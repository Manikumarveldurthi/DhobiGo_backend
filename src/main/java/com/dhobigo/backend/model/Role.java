package com.dhobigo.backend.model;

/**
 * The three sides of the app, matching PROGRESS.md's frontend plan:
 * CUSTOMER -> services.html / payment.html / tracking.html
 * DHOBI    -> dhobi.html
 * ADMIN    -> future admin console
 */
public enum Role {
    CUSTOMER,
    DHOBI,
    ADMIN
}
