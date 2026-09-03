package com.example.ecommerce.exception;

import com.example.ecommerce.controller.WebController;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Centralized error handling for the Thymeleaf web layer (WebController).
 * Renders the shared error.html template with a friendly message instead of
 * exposing a stack trace.
 */
@ControllerAdvice(assignableTypes = WebController.class)
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ModelAndView handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("Web error [{}] on {} {}: {}", ex.getErrorCode(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(ex.getStatus());
        mav.addObject("errorTitle", humanize(ex.getErrorCode()));
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected web error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("errorTitle", "Something went wrong");
        mav.addObject("errorMessage", "An unexpected error occurred. Please try again later.");
        return mav;
    }

    private String humanize(ErrorCode code) {
        String[] parts = code.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase()).append(' ');
        }
        return sb.toString().trim();
    }
}
