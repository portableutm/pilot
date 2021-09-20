package com.dronfieslabs.portableutmpilot;

public interface IGenericCallback<T> {

    void onResult(T t, String errorMessage);
}
