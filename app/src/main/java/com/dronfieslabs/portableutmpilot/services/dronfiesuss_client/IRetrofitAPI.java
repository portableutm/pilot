package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

interface IRetrofitAPI {

    @POST("auth/login")
    Call<String> login(@Body User user);

    @POST("operation")
    Call<Object> addOperation(@Header("auth") String authToken, @Body Operation operation);

    @POST("position")
    Call<Object> sendPosition(@Header("auth") String authToken, @Body Position position);

    @POST("paraglidingposition")
    Call<Object> sendParaglidingPosition(@Header("auth") String authToken, @Body ParaglidingPosition position);

    @GET("operation/owner")
    Call<Object> getOperations(@Header("auth") String authToken);

    @GET("operation/owner")
    Call<Object> getOperations(@Header("auth") String authToken, @Query("limit") int limit, @Query("offset") int offset);

    @DELETE("operation/{id}")
    Call<Object> deleteOperation(@Header("auth") String authToken, @Path("id") String id);

    @GET("vehicle")
    Call<ResponseBody> getVehicles(@Header("auth") String authToken);

    @GET("restrictedflightvolume")
    Call<ResponseBody> getRestrictedFlightVolumes(@Header("auth") String authToken);
}
