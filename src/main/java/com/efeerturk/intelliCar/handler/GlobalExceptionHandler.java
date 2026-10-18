package com.efeerturk.intelliCar.handler;

import com.efeerturk.intelliCar.enums.MessageType;
import com.efeerturk.intelliCar.exception.*;
import com.efeerturk.intelliCar.exception.Exception;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;

import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = { BaseException.class })
    public ResponseEntity<ApiError<?>> handleBaseException(BaseException ex, WebRequest request) {
        return ResponseEntity.badRequest().body(createApiError(ex.getMessage(), request));
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError>handlerNotValidException(MethodArgumentNotValidException validException, WebRequest webRequest){

        List<String> errorMessages = new ArrayList<>();

        for (FieldError error : validException.getBindingResult().getFieldErrors()) {

            errorMessages.add(error.getDefaultMessage());
        }
        String finalMessage = String.join(", ", errorMessages);

        return ResponseEntity
                .badRequest()
                .body(createApiError(finalMessage,webRequest));
    }

    private List<String> addValue(List<String> list, String newValue) {
        list.add(newValue);
        return list;
    }

    private String getHostName() {
        try {
            return Inet4Address.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return "";
    }

    public <E> ApiError<E> createApiError(E message, WebRequest request) {
        ApiError<E> apiError = new ApiError<>();
        apiError.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

        Exception<E> exception=new Exception<>();
        exception.setPath(request.getDescription(false).substring(4));
        exception.setCreateTime(new Date());
        exception.setMessage(message);
        exception.setHostName(getHostName());

        apiError.setException(exception);

        return apiError;
    }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorMessage> handleAuthenticationException(AuthenticationException ex) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorMessage(MessageType.INVALID_TOKEN, MessageType.INVALID_TOKEN.getMessage()));
    }
    /*public ResponseEntity<Map<String,String>> handleException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        fieldErrors.forEach(error -> {errors.put(error.getField(), error.getDefaultMessage());});
        return ResponseEntity.badRequest().body(errors);
    }*/
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String,String>> handleException(HandlerMethodValidationException ex) {
        Map<String, String> errors = new HashMap<>();
        List<ParameterValidationResult> results = ex.getParameterValidationResults();
        results.forEach(result -> {String paramName=result.getMethodParameter().getParameterName();String combinedMessages=result.getResolvableErrors().stream().map(error->error.getDefaultMessage()).collect(Collectors.joining(","));errors.put(paramName,combinedMessages);});
       return ResponseEntity.badRequest().body(errors);

}
}
