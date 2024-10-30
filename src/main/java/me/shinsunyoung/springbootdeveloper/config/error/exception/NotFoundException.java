package me.shinsunyoung.springbootdeveloper.config.error.exception;

import me.shinsunyoung.springbootdeveloper.config.error.ErrorCode;
import me.shinsunyoung.springbootdeveloper.config.error.ErrorResponse;

public class NotFoundException extends BusinessBaseException {
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode);
    }

    public NotFoundException() {
        super(ErrorCode.NOT_FOUND);
    }
}
