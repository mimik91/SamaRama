package com.samarama.bicycle.api.utils;

public class SecurityConstants {
    public static final String ROLE_CLIENT = "ROLE_CLIENT";
    public static final String ROLE_SERVICEMAN = "ROLE_SERVICEMAN";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    // Dla metod preAuthorize można użyć tych stałych bez prefiksu ROLE_
    public static final String CLIENT = "CLIENT";
    public static final String SERVICEMAN = "SERVICEMAN";
    public static final String ADMIN = "ADMIN";
}