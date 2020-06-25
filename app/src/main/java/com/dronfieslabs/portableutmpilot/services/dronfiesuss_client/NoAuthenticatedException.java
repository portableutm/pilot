package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client;

public class NoAuthenticatedException extends Exception {

    public NoAuthenticatedException(String message){
        super(message);
    }

    public NoAuthenticatedException(String message, Exception ex){
        super(message, ex);
    }
}
