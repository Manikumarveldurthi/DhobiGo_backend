package com.dhobigo.backend.service;

import com.dhobigo.backend.config.RazorpayProperties;
import com.dhobigo.backend.exception.ApiException;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around the Razorpay SDK.
 *
 * SAFE BY DEFAULT: if app.razorpay.enabled is false (the default — see
 * RazorpayProperties), createOrder() throws a clear ApiException instead of
 * calling out to Razorpay; PaymentController/OrderService fall back to demo
 * mode in that case rather than letting the checkout button just fail.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String CURRENCY = "INR";

    private final RazorpayProperties props;

    public PaymentService(RazorpayProperties props) {
        this.props = props;
    }

    public boolean isEnabled() {
        return props.isEnabled();
    }

    public String getPublicKeyId() {
        return props.getKeyId();
    }

    /**
     * Creates a Razorpay order for the given amount (in rupees — converted
     * to paise here, since that's the unit Razorpay's API expects).
     * receipt is an internal reference string, not shown to the customer.
     */
    public JSONObject createOrder(int amountRupees, String receipt) {
        if (!props.isEnabled()) {
            throw new ApiException("Payment gateway is not configured on this server", HttpStatus.SERVICE_UNAVAILABLE);
        }
        try {
            RazorpayClient client = new RazorpayClient(props.getKeyId(), props.getKeySecret());
            JSONObject request = new JSONObject();
            request.put("amount", amountRupees * 100); // paise
            request.put("currency", CURRENCY);
            request.put("receipt", receipt);
            com.razorpay.Order rzpOrder = client.orders.create(request);
            return rzpOrder.toJson();
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new ApiException("Could not start payment — please try again", HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Verifies the signature Razorpay Checkout hands back on success, proving
     * the payment actually happened and wasn't just faked by the browser.
     * Throws if verification fails or can't be attempted (gateway disabled).
     */
    public void verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        if (!props.isEnabled()) {
            throw new ApiException("Payment gateway is not configured on this server", HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
            throw new ApiException("Missing payment verification details", HttpStatus.BAD_REQUEST);
        }
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);

            boolean valid = Utils.verifyPaymentSignature(attributes, props.getKeySecret());
            if (!valid) {
                throw new ApiException("Payment verification failed", HttpStatus.PAYMENT_REQUIRED);
            }
        } catch (RazorpayException e) {
            log.error("Razorpay signature verification error: {}", e.getMessage());
            throw new ApiException("Payment verification failed", HttpStatus.PAYMENT_REQUIRED);
        }
    }
}
