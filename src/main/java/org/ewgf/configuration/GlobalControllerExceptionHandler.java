package org.ewgf.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalControllerExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleGenericException(Exception ex) {
        log.error("Unhandled exception caught: {} ", ex.getMessage());
        return ResponseEntity.internalServerError().build();
    }
}