package com.efeerturk.intelliCar.handler;

import com.efeerturk.intelliCar.exception.ApiError;
import com.efeerturk.intelliCar.exception.BaseException;
import com.efeerturk.intelliCar.exception.Exception;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.efeerturk.intelliCar.exception.Exception;
import org.springframework.web.context.request.WebRequest;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ControllerAdvice
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
}
