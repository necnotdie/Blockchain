package com.bc.server.exception;

public class JSONparseException extends Exception{
    public JSONparseException(){

    }
    public JSONparseException(String message){
        super(message);
    }

    public JSONparseException(String message, Throwable cause) {
        super(message, cause);
    }

    public JSONparseException(Throwable cause) {
        super(cause);
    }
}
