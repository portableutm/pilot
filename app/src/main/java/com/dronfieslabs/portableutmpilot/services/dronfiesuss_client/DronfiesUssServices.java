package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client;

import android.util.Log;

import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.GPSCoordinates;
import com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.ICompletitionCallback;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DronfiesUssServices {

    // Singleton Pattern
    private static DronfiesUssServices INSTANCE = null;

    private final IRetrofitAPI api;

    private String authToken = null;

    private static String utmEndpoint = null;

    private DronfiesUssServices() {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(DronfiesUssServices.utmEndpoint)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        api = retrofit.create(IRetrofitAPI.class);
    }

    public static DronfiesUssServices getInstance(String utmEndpoint){
        if(utmEndpoint == null){
            // utmEndpoint cant be null
            throw new RuntimeException("UTM endpoint can't be null");
        }
        boolean utmEndpointChanged = false;
        if(DronfiesUssServices.utmEndpoint == null || !DronfiesUssServices.utmEndpoint.equals(utmEndpoint)){
            utmEndpointChanged = true;
            DronfiesUssServices.utmEndpoint = utmEndpoint;
        }
        if(INSTANCE == null || utmEndpointChanged){
            // it means we have to regenerate the INSTANCE
            try{
                INSTANCE = new DronfiesUssServices();
            }catch(Exception ex){}
        }
        return INSTANCE;
    }

    //----------------------------------------------------------------------------------------------------
    //------------------------------------------ PUBLIC METHODS ------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public boolean isAuthenticated(){
        return authToken != null;
    }

    public void logout(){
        authToken = null;
    }

    public void login(String username, String password, final ICompletitionCallback<String> callback){
        api.login(new User(username, password)).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if(!response.isSuccessful()){
                    callback.onResponse(null, response.code() + "");
                    return;
                }
                authToken = response.body();
                callback.onResponse(response.body(), null);
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                callback.onResponse(null, t.getMessage());
            }
        });
    }

    // When we add an operation, the droneDescription is ignored. This field is used when we get the operations from the backend
    public void addOperation(com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.Operation operation, final ICompletitionCallback<String> callback){
        api.addOperation(authToken, transformOperation(operation)).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if(!response.isSuccessful()){
                    String errorBody = "";
                    try{
                        errorBody = response.errorBody().string();
                    }catch (Exception ex){}
                    callback.onResponse(null, response.code() + " (response.body: "+response.raw().body()+", response.errorBody: "+ errorBody + ")");
                    return;
                }
                // we update the authToken everytime a service responds succesfully
                //DronfiesUssServices.this.authToken = response.headers().get("token");

                if(response.body().toString().startsWith("{Error=")){
                    callback.onResponse(null, response.body().toString() + " (httpCode="+response.code()+")");
                }else{
                    callback.onResponse(response.body().toString() + " ("+response.getClass()+")", null);
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                callback.onResponse(null, t.getMessage());
            }
        });
    }

    public void sendPosition(double lon, double lat, double alt, double heading, String operationId, final ICompletitionCallback<String> callback){
        Date now = new Date();
        String timeSent = new SimpleDateFormat("yyyy-MM-dd").format(now) + "T" + new SimpleDateFormat("HH:mm:ss.SSS").format(now) + "Z";
        Position position = new Position(
                (int)Math.round(alt),
                new Location(
                    "Point",
                    new double[]{lon, lat}
                ),
                heading,
                timeSent,
                operationId
        );
        api.sendPosition(authToken, position).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if(!response.isSuccessful()){
                    callback.onResponse(null, response.code() + " ("+response.getClass()+")");
                    return;
                }
                // we update the authToken everytime a service responds succesfully
                //DronfiesUssServices.this.authToken = response.headers().get("token");

                callback.onResponse(response.body().toString() + " ("+response.getClass()+")", null);
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                callback.onResponse(null, t.getMessage());
            }
        });
    }

    public void getOperations(final ICompletitionCallback<List<com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.Operation>> callback) throws NoAuthenticatedException {
        if(authToken == null){
            throw new NoAuthenticatedException("You must call login method, before calling this method");
        }
        api.getOperations(authToken).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if(!response.isSuccessful()){
                    callback.onResponse(null, response.code() + " ("+response.getClass()+")");
                    return;
                }
                // we update the authToken everytime a service responds succesfully
                //DronfiesUssServices.this.authToken = response.headers().get("token");
                List<com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.Operation> listOperations = new ArrayList<>();
                JsonObject jsonObject = new Gson().toJsonTree((Map<?, List<?>>)response.body()).getAsJsonObject();
                for(JsonElement jsonElement : jsonObject.get("ops").getAsJsonArray()){
                    try{
                        listOperations.add(getOperationFromJsonObject(jsonElement.getAsJsonObject()));
                    }catch(Exception ex){}
                }
                callback.onResponse(listOperations, null);
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                callback.onResponse(null, t.getMessage());
            }
        });
    }

    public void deleteOperation(String operationId, final ICompletitionCallback<String> callback){
        api.deleteOperation(authToken, operationId).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if(!response.isSuccessful()){
                    callback.onResponse(null, response.code() + " ("+response.getClass()+")");
                    return;
                }
                callback.onResponse(response.body().toString(), null);
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                callback.onResponse(null, t.getMessage());
            }
        });
    }

    public List<Vehicle> getVehicles() throws Exception {
        String responseBody = api.getVehicles(authToken).execute().body().string();
        JSONArray jsonArrayVehicles = new JSONArray(responseBody);
        List<Vehicle> ret = new ArrayList<>();
        for(int i = 0; i < jsonArrayVehicles.length(); i++){
            JSONObject jsonObject = jsonArrayVehicles.getJSONObject(i);
            ret.add(parseVehicle(jsonObject));
        }
        return ret;
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------- PRIVATE METHODS  -----------------------------------------
    //----------------------------------------------------------------------------------------------------

    private Operation transformOperation(com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.Operation operation){
        List<List<Double>> polygonCoordinates = new ArrayList<>();
        for(GPSCoordinates latLng : operation.getPolygon()){
            // for the backend, we have to send (longitude, latitude)
            polygonCoordinates.add(Arrays.asList(latLng.getLongitude(), latLng.getLatitude()));
        }

        Date submitDate = new Date();
        List<String> uasRegistrations = new ArrayList<>();
        uasRegistrations.add(operation.getDroneId());
        PriorityElements priorityElements = new PriorityElements(
                1,
                "EMERGENCY_AIR_AND_GROUND_IMPACT"
        );

        List<ContingencyPlan> contingencyPlans = new ArrayList<ContingencyPlan>();
        /*List<ContingencyPlan> contingencyPlans = Arrays.asList(
                new ContingencyPlan(
                        Arrays.asList("ENVIRONMENTAL", "LOST_NAV"),
                        "",
                        new ContingencyPolygon(
                                "Polygon",
                                new ArrayList<List<List<Double>>>()
                        ),
                        "LANDING",
                        "",
                        -1,
                        -1,
                        Arrays.asList(1, 0),
                        formatDateForOperationObject(operation.getStartDatetime()),
                        formatDateForOperationObject(operation.getEndDatetime())
                )
        );*/
        List<OperationVolume> operationVolumes = Arrays.asList(
                new OperationVolume(
                    formatDateForOperationObject(operation.getStartDatetime()),
                        formatDateForOperationObject(operation.getEndDatetime()),
                        -1,
                        operation.getMaxAltitude(),
                        new OperationGeography(
                                "Polygon",
                                Arrays.asList(
                                        polygonCoordinates
                                )
                        ),
                        true
                )
        );
        List<NegotiationAgreement> negotiationAgreements = Arrays.asList(
                new NegotiationAgreement(
                        "",
                        "",
                        "",
                        "",
                        "",
                        ""
                )
        );
        return new Operation(
                operation.getDescription(),
                "",
                "Simple polygon",
                "",
                formatDateForOperationObject(submitDate),
                formatDateForOperationObject(submitDate),
                0,
                2,
                false,
                operation.getPilotName(),
                null,
                operation.getOwner(),
                uasRegistrations,
                priorityElements,
                contingencyPlans,
                operationVolumes,
                negotiationAgreements
        );
    }

    private com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.Operation getOperationFromJsonObject(JsonObject jsonObject) throws Exception {
        String id = jsonObject.get("gufi").getAsString();
        String description = jsonObject.get("name").getAsString();
        String pilotName = "";
        try{
            pilotName = jsonObject.get("contact").getAsString();
        }catch(Exception ex){}
        String droneId = null;
        String droneDescription = null;
        try{
            droneId = jsonObject.get("uas_registrations").getAsJsonArray().get(0).getAsJsonObject().get("uvin").getAsString();
            droneDescription = jsonObject.get("uas_registrations").getAsJsonArray().get(0).getAsJsonObject().get("vehicleName").getAsString();
        }catch(Exception ex){}
        JsonArray jsonArrayOperationVolumes = jsonObject.get("operation_volumes").getAsJsonArray();
        if(jsonArrayOperationVolumes.size() != 1){
            throw new Exception("operacion invalida");
        }
        JsonObject jsonObjectOperationVolume = jsonArrayOperationVolumes.get(0).getAsJsonObject();
        String strEffectiveTimeBegin = jsonObjectOperationVolume.get("effective_time_begin").getAsString();
        String strEffectiveTimeEnd = jsonObjectOperationVolume.get("effective_time_end").getAsString();
        int maxAltitude = (int)Math.round(jsonObjectOperationVolume.get("max_altitude").getAsDouble());
        com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.Operation.EnumOperationState state = com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.Operation.EnumOperationState.valueOf(jsonObject.get("state").getAsString());
        String owner = jsonObject.getAsJsonObject("owner").get("username").getAsString();
        // parse polygon
        List<GPSCoordinates> polygon = new ArrayList<>();
        try{
            JsonArray coordinates = jsonObjectOperationVolume.getAsJsonObject("operation_geography").getAsJsonArray("coordinates").get(0).getAsJsonArray();
            for(int i = 0; i < coordinates.size(); i++){
                // the order in the jsonObject is (longitude, latitude)
                GPSCoordinates latLng = new GPSCoordinates(
                        coordinates.get(i).getAsJsonArray().get(1).getAsDouble(),
                        coordinates.get(i).getAsJsonArray().get(0).getAsDouble()
                );
                polygon.add(latLng);
            }
        }catch(Exception ex){}
        // return operation
        return new com.dronfieslabs.portableutmpilot.services.dronfiesuss_client.entities.Operation(
                id,
                description,
                polygon,
                parseDate(strEffectiveTimeBegin),
                parseDate(strEffectiveTimeEnd),
                maxAltitude,
                pilotName,
                droneId,
                droneDescription,
                state,
                owner
        );
    }

    private Date parseDate(String strDatetime) throws Exception{
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(strDatetime.replaceAll("T", " ").replaceAll("Z", ""));
    }

    private String formatDateForOperationObject(Date date){
        // example: 2019-12-11T19:59:10Z
        return new SimpleDateFormat("yyyy-MM-dd").format(date) + "T" + new SimpleDateFormat("HH:mm:ss").format(date) + "Z";
    }

    private Vehicle parseVehicle(JSONObject jsonObjectVehicle) throws Exception{
        String uvin = getStringValueFromJSONObject(jsonObjectVehicle, "uvin");
        Date date = null;
        if(jsonObjectVehicle.has("date") && !jsonObjectVehicle.isNull("date")){
            date = parseDate(jsonObjectVehicle.getString("date"));
        }
        String nNumber = getStringValueFromJSONObject(jsonObjectVehicle, "nNumber");
        String faaNumber = getStringValueFromJSONObject(jsonObjectVehicle, "faaNumber");
        String vehicleName = getStringValueFromJSONObject(jsonObjectVehicle, "vehicleName");
        String manufacturer = getStringValueFromJSONObject(jsonObjectVehicle, "manufacturer");
        String model = getStringValueFromJSONObject(jsonObjectVehicle, "model");
        String strVehicleClass = getStringValueFromJSONObject(jsonObjectVehicle, "class");
        Vehicle.EnumVehicleClass vehicleClass = null;
        if(strVehicleClass != null){
            if(strVehicleClass.equalsIgnoreCase("MULTIROTOR")){
                vehicleClass = Vehicle.EnumVehicleClass.MULTIROTOR;
            }else if(strVehicleClass.equalsIgnoreCase("FIXEDWING")){
                vehicleClass = Vehicle.EnumVehicleClass.FIXEDWING;
            }
        }
        String registeredBy = null;
        if(jsonObjectVehicle.has("registeredBy") && !jsonObjectVehicle.isNull("registeredBy")){
            registeredBy = getStringValueFromJSONObject(jsonObjectVehicle.getJSONObject("registeredBy"), "username");
        }
        String owner = null;
        if(jsonObjectVehicle.has("owner") && !jsonObjectVehicle.isNull("owner")){
            owner = getStringValueFromJSONObject(jsonObjectVehicle.getJSONObject("owner"), "username");
        }
        return new Vehicle(uvin, date, nNumber, faaNumber, vehicleName, manufacturer, model, vehicleClass, registeredBy, owner);
    }

    private String getStringValueFromJSONObject(JSONObject jsonObject, String key) throws Exception {
        if(!jsonObject.has(key) || jsonObject.isNull(key)){
            return null;
        }
        return jsonObject.getString(key);
    }
}
