package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

interface IRetrofitAPI {

    @POST("auth/login")
    Call<String> login(@Body User user);

    @POST("operation")
    Call<Object> addOperation(@Header("auth") String authToken, @Body Operation operation);

    @POST("position")
    Call<Object> sendPosition(@Header("auth") String authToken, @Body Position position);

    @GET("operation/owner")
    Call<Object> getOperations(@Header("auth") String authToken);

    @DELETE("operation/{id}")
    Call<Object> deleteOperation(@Header("auth") String authToken, @Path("id") String id);
}
