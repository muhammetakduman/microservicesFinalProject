package com.microservices.api_gateway.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

/**
 * HttpServletRequest immutable olduğu için header ekleyemeyiz.
 * Bu wrapper ile yeni header'lar eklenir (X-User-Id, X-User-Email, X-User-Role).
 * JwtAuthFilter, downstream'e bu wrapper'ı iletir.
 */
public class MutableHttpServletRequest extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders = new HashMap<>();

    public MutableHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    public void putHeader(String name, String value) {
        customHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        // Önce custom header'lara bak, yoksa original'a düş
        String value = customHeaders.get(name);
        return value != null ? value : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String value = customHeaders.get(name);
        if (value != null) {
            return Collections.enumeration(List.of(value));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> allNames = new HashSet<>(customHeaders.keySet());
        Enumeration<String> original = super.getHeaderNames();
        while (original.hasMoreElements()) {
            allNames.add(original.nextElement());
        }
        return Collections.enumeration(allNames);
    }
}

