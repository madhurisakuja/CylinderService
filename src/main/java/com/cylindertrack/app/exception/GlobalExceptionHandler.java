package com.cylindertrack.app.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public String handleAccessDenied(OAuth2AuthenticationException ex) {
        log.warn("OAuth2 access denied: {}", ex.getMessage());
        return "redirect:/login?error=true";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("message", "Something went wrong. Please try again.");
        return mav;
    }
}
