package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities;

public interface ICompletitionCallback<T> {

    void onResponse(T t, String errorMessage);
}
