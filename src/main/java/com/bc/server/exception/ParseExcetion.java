package com.bc.server.exception;

public class ParseExcetion extends Exception {
    public ParseExcetion(){

    }
    public ParseExcetion(String message){
        super(message);
    }

    public ParseExcetion(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseExcetion(Throwable cause) {
        super(cause);
    }
}
