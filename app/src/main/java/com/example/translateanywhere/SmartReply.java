package com.example.translateanywhere;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SmartReply {

    private static final String HUGGING_FACE_API_URL =
            "https://api-inference.huggingface.co/models/t5-small";
    private static final String API_KEY = "Bearer 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";

    public interface SmartReplyCallback{
        void onReplyReceived(String response);

        void onError(String error);
    }

    public static void GenerateResponse(String message, SmartReplyCallback callback){
        OkHttpClient client=new OkHttpClient();

        MediaType mediaType=MediaType.parse("application/json");
        String jsonBody = "{\"inputs\": \"" + message + "\"}";
        RequestBody body=RequestBody.create(jsonBody,mediaType);


        Request request=new Request.Builder()
                .url(HUGGING_FACE_API_URL)
                .post(body)
                .addHeader("Authorization",API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()){
                    callback.onError("HTTP Error: " + response.code());
                    return;
                }
                if (response.body() != null) {
                    String responseStr=response.body().string();
                    String reply = extractGeneratedText(responseStr);
                    callback.onReplyReceived(reply);
                }
            }
        });
    }

    private static String extractGeneratedText(String responseStr) {
        try {
            JSONArray arr = new JSONArray(responseStr);
            JSONObject obj = arr.getJSONObject(0);
            return obj.getString("generated_text");
        } catch (Exception e) {
            return "Oops, looks like my brain just glitched.";
        }
    }

}
