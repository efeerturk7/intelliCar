package com.efeerturk.intelliCar.exception;

import com.efeerturk.intelliCar.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorMessage {

    private MessageType messageType;

    private String ofStatic;



    public String prepareErrorMessage() {
        StringBuilder builder = new StringBuilder();
        builder.append(messageType.getMessage());
        if(this.ofStatic!=null) {
            builder.append(" : " + ofStatic);
        }
        return builder.toString();
    }
}
