package com.example.translateanywhere;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.gms.common.Feature;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

public class IntentExtractor {

    GenerativeModelFutures modelFutures;

    GenerativeModel generativeModel;

    String GeminiApikey;

    Context context;


    public IntentExtractor(Context context) {
        this.context = context;
        setUp();
    }

    public interface ExtractorCallback {
        void onExtracted(String JSOn);

        void onError(String e);
    }

    public void extractor(String userQuery, ExtractorCallback callback) {
        String prompt = "You are an intelligent assistant that extracts user intent from natural language input.\n" +
                "Your job is to convert the input into a JSON object using the following rules:\n\n" +

                "1. Always return a valid JSON object ONLY.\n" +
                "2. If the input is not actionable, return just this string: 'invalid'\n\n" +

                "Supported intents:\n" +
                "- MESSAGE: {\"intent\": \"MESSAGE\", \"target\": \"<name>\", \"content\": \"<message text>\"}\n" +
                "- CALL: {\"intent\": \"CALL\", \"target\": \"<name>\"}\n" +
                "- PLAY: {\"intent\": \"PLAY\", \"target\": \"<what to play>\"}\n" +
                "- NEXT: {\"intent\": \"NEXT\"}\n" +
                "- PREVIOUS: {\"intent\": \"PREVIOUS\"}\n" +
                "- STOP: {\"intent\": \"STOP\"}\n" +
                "- REMINDER: {\"intent\": \"REMINDER\", \"task\": \"<what to remind>\", \"time\": \"<time or date>\"}\n\n" +
                "- TRANSLATE: {\"intent\": \"TRANSLATE\"}\n"+
                "- OPEN: {\"intent\": \"OPEN\", \"target\": \"<app name>\"}\n" +

                "Only include the relevant fields for each intent.\n" +
                "Never include explanations, extra text, or comments.\n" +
                "Even if you're unsure, make your best guess in the correct format.\n\n" +


                "Examples:\n" +
                "User: \"Call Steve\" → {\"intent\": \"CALL\", \"target\": \"Steve\"}\n" +
                "User: \"Call \" → {\"intent\": \"CALL\", \"target\": \"null\"}\n" +
                "User: \"Send a message to Alex saying good morning\" → {\"intent\": \"MESSAGE\", \"target\": \"Alex\", \"content\": \"good morning\"}\n" +
                "User: \"Send a message to Alex home 'or' Alex 2\" → {\"intent\": \"MESSAGE\", \"target\": \"Alex home 'or' Alex 2\", \"content\": \"null\"}\n" +
                "User: \"Message Sarah I’m on my way\" → {\"intent\": \"MESSAGE\", \"target\": \"Sarah\", \"content\": \"I’m on my way\"}\n" +
                "User: \"Send this to John\" → {\"intent\": \"MESSAGE\", \"target\": \"John\", \"content\": \"this\"}\n" +
                "User: \"Send hi to Steve\" → {\"intent\": \"MESSAGE\", \"target\": \"Steve\", \"content\": \"hi\"}\n" +
                "User: \"Play Tamil hits\" → {\"intent\": \"PLAY\", \"target\": \"Tamil hits\"}\n" +
                "User: \"Next song\" → {\"intent\": \"NEXT\"}\n" +
                "User: \"Stop music\" → {\"intent\": \"STOP\"}\n" +
                "User: \"Remind me to charge phone at 9 PM\" → {\"intent\": \"REMINDER\", \"task\": \"charge phone\", \"time\": \"9 PM\"}\n\n" +
                "User: \"Can you open camera\" → {\"intent\": \"OPEN\", \"target\": \"CAMERA\"}\n" +

                "Now extract intent from:\n" +
                "User: \"" + userQuery + "\"";



        Content content=new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = modelFutures.generateContent(content);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    callback.onExtracted(result.getText());
                    Log.d("ExtractedIntent",result.getText());
                }

                @Override
                public void onFailure(Throwable t) {
                    callback.onError(t.getMessage());
                }
            },context.getMainExecutor());
        }


    }
    public void setUp(){
        SharedPreferences sharedPreferences=context.getSharedPreferences("AccessKeys",MODE_PRIVATE);
        GeminiApikey= sharedPreferences.getString("Key2",null);
        if(GeminiApikey==null)return;
        generativeModel = new GenerativeModel("gemini-1.5-flash", GeminiApikey);
        modelFutures = GenerativeModelFutures.from(generativeModel);
    }
}
