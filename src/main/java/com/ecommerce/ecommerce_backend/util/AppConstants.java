// src/main/java/com/ecommerce/util/AppConstants.java
package com.ecommerce.ecommerce_backend.util;

public class AppConstants {

    // Private constructor to prevent instantiation
    private AppConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ============ API Constants ============
    public static final String API_BASE_PATH = "/api";
    public static final String API_VERSION = "v1";
    public static final String API_PREFIX = API_BASE_PATH + "/" + API_VERSION;

    // ============ Endpoint Constants ============
    public static final String AUTH_ENDPOINT = API_BASE_PATH + "/auth";
    public static final String PRODUCT_ENDPOINT = API_BASE_PATH + "/products";
    public static final String CATEGORY_ENDPOINT = API_BASE_PATH + "/categories";
    public static final String ORDER_ENDPOINT = API_BASE_PATH + "/orders";
    public static final String CART_ENDPOINT = API_BASE_PATH + "/cart";
    public static final String PAYMENT_ENDPOINT = API_BASE_PATH + "/payments";
    public static final String USER_ENDPOINT = API_BASE_PATH + "/users";
    public static final String ADMIN_ENDPOINT = API_BASE_PATH + "/admin";

    // ============ JWT Constants ============
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_TYPE = "Bearer";
    public static final String TOKEN_SECRET = "jwt.secret";
    public static final String TOKEN_EXPIRATION = "jwt.expiration";
    public static final String REFRESH_TOKEN_EXPIRATION = "jwt.refresh-expiration";

    // ============ Security Constants ============
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String AUTHORITIES_KEY = "auth";

    // ============ HTTP Headers ============
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String X_TOTAL_COUNT = "X-Total-Count";
    public static final String X_REQUEST_ID = "X-Request-ID";

    // ============ Pagination Constants ============
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 50;
    public static final String DEFAULT_SORT_BY = "id";
    public static final String DEFAULT_SORT_DIRECTION = "desc";

    // ============ Order Status Constants ============
    public static final String ORDER_STATUS_PENDING = "PENDING";
    public static final String ORDER_STATUS_PROCESSING = "PROCESSING";
    public static final String ORDER_STATUS_SHIPPED = "SHIPPED";
    public static final String ORDER_STATUS_DELIVERED = "DELIVERED";
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED";

    // ============ Payment Status Constants ============
    public static final String PAYMENT_STATUS_PENDING = "PENDING";
    public static final String PAYMENT_STATUS_PAID = "PAID";
    public static final String PAYMENT_STATUS_FAILED = "FAILED";
    public static final String PAYMENT_STATUS_REFUNDED = "REFUNDED";

    // ============ File Upload Constants ============
    public static final String UPLOAD_DIR = "uploads/";
    public static final String PRODUCT_IMAGE_DIR = UPLOAD_DIR + "products/";
    public static final String USER_IMAGE_DIR = UPLOAD_DIR + "users/";
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final String ALLOWED_IMAGE_EXTENSIONS = "jpg,jpeg,png,gif,webp";
    public static final String DEFAULT_PRODUCT_IMAGE = "default-product.png";

    // ============ Cache Constants ============
    public static final String CACHE_NAME_PRODUCTS = "products";
    public static final String CACHE_NAME_CATEGORIES = "categories";
    public static final String CACHE_NAME_USERS = "users";
    public static final long CACHE_TTL = 3600; // 1 hour

    // ============ Regular Expression Patterns ============
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    public static final String PHONE_PATTERN = "^\\+?[0-9]{10,15}$";
    public static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9._-]{3,50}$";
    public static final String ORDER_NUMBER_PATTERN = "^ORD-\\d{13}$";

    // ============ Date/Time Constants ============
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String ISO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String TIME_ZONE_UTC = "UTC";

    // ============ Error Messages ============
    public static final String ERROR_RESOURCE_NOT_FOUND = "Resource not found";
    public static final String ERROR_UNAUTHORIZED = "Unauthorized access";
    public static final String ERROR_FORBIDDEN = "Access denied";
    public static final String ERROR_VALIDATION = "Validation failed";
    public static final String ERROR_INTERNAL_SERVER = "Internal server error";
    public static final String ERROR_DUPLICATE_ENTRY = "Duplicate entry";
    public static final String ERROR_INVALID_INPUT = "Invalid input";
    public static final String ERROR_FILE_UPLOAD = "File upload failed";
    public static final String ERROR_PAYMENT_FAILED = "Payment processing failed";

    // ============ Success Messages ============
    public static final String SUCCESS_OPERATION = "Operation completed successfully";
    public static final String SUCCESS_CREATED = "Resource created successfully";
    public static final String SUCCESS_UPDATED = "Resource updated successfully";
    public static final String SUCCESS_DELETED = "Resource deleted successfully";
    public static final String SUCCESS_AUTHENTICATED = "User authenticated successfully";
    public static final String SUCCESS_REGISTERED = "User registered successfully";

    // ============ Stripe Constants ============
    public static final String STRIPE_CURRENCY = "usd";
    public static final String STRIPE_PAYMENT_METHOD_CARD = "card";
    public static final String STRIPE_WEBHOOK_SECRET = "stripe.webhook.secret";
    public static final String STRIPE_EVENT_PAYMENT_SUCCEEDED = "payment_intent.succeeded";
    public static final String STRIPE_EVENT_PAYMENT_FAILED = "payment_intent.payment_failed";
    public static final String STRIPE_EVENT_REFUND_SUCCEEDED = "charge.refunded";

    // ============ Email Constants ============
    public static final String EMAIL_SUBJECT_ORDER_CONFIRMATION = "Order Confirmation #%s";
    public static final String EMAIL_SUBJECT_PAYMENT_RECEIPT = "Payment Receipt #%s";
    public static final String EMAIL_SUBJECT_WELCOME = "Welcome to E-Commerce!";
    public static final String EMAIL_SUBJECT_PASSWORD_RESET = "Password Reset Request";

    // ============ Application Constants ============
    public static final String APP_NAME = "E-Commerce Application";
    public static final String APP_VERSION = "1.0.0";
    public static final String APP_DEFAULT_LOCALE = "en";
    public static final String APP_DEFAULT_TIMEZONE = "UTC";

    // ============ Logging Constants ============
    public static final String LOG_START = "Starting operation: {}";
    public static final String LOG_END = "Completed operation: {}";
    public static final String LOG_ERROR = "Error in operation: {}";

    // ============ Custom Header Constants ============
    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_APP_VERSION = "X-App-Version";
    public static final String HEADER_DEVICE_ID = "X-Device-Id";
}