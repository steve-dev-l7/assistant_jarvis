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
                "- PLAY MUSIC: {\"intent\": \"PLAY MUSIC\", \"target\": \"<what to play>\"}\n" +
                "- NEXT MUSIC: {\"intent\": \"NEXT MUSIC\"}\n" +
                "- PREVIOUS MUSIC: {\"intent\": \"PREVIOUS MUSIC\"}\n" +
                "- STOP MUSIC: {\"intent\": \"STOP MUSIC\"}\n" +
                "- REMINDER: {\"intent\": \"REMINDER\", \"task\": \"<what to remind>\", \"time\": \"<time or date>\"}\n\n" +
                "- TRANSLATE: {\"intent\": \"TRANSLATE\"}\n"+
                "- OPEN: {\"intent\": \"OPEN\", \"target\": \"<app name>\"}\n" +
                "- SAVE: {\"intent\": \"SAVE\", \"task\": \"<number>\" \"target\": \"<name>\"}\n" +
                "- DEACTIVATE: {\"intent\": \"DEACTIVATE\"}\\n\""+
                "- SHARE CONTACT: {\"intent\": \"SHARE CONTACT\", \"task\": \"<contact name>\" \"target\": \"<name>\"}\n" +
                "- COPY NUMBER: {\"intent\": \"COPY NUMBER\", \"task\": \"<name>\"}\n" +


                "Only include the relevant fields for each intent.\n" +
                "Never include explanations, extra text, or comments.\n" +


                "Examples:\n" +
                "User: \"Call Steve\" → {\"intent\": \"CALL\", \"target\": \"Steve\"}\n" +
                "User: \"Call \" → {\"intent\": \"CALL\", \"target\": \"null\"}\n" +
                "User: \"Send a message to Alex  good morning\" → {\"intent\": \"MESSAGE\", \"target\": \"Alex\", \"content\": \"good morning\"}\n" +
                "User: \"Send a message to Alex home 'or' Alex 2\" → {\"intent\": \"MESSAGE\", \"target\": \"Alex home 'or' Alex 2\", \"content\": \"null\"}\n" +
                "User: \"Where are you send this to Sarah\" → {\"intent\": \"MESSAGE\", \"target\": \"Sarah\", \"content\": \"Where are you\"}\n" +
                "User: \"Send hi to Steve\" → {\"intent\": \"MESSAGE\", \"target\": \"Steve\", \"content\": \"hi\"}\n" +
                "User: \"Play music\" → {\"intent\": \"PLAY MUSIC\", \"target\": \"null\"}\n" +
                "User: \"Next song\" → {\"intent\": \"NEXT MUSIC\"}\n" +
                "User: \"Stop music\" → {\"intent\": \"STOP MUSIC\"}\n" +
                "User: \"Remind me in 5 minutes\" → {\"intent\": \"REMINDER\", \"task\": \"null\", \"time\": \"5 minutes\"}\n\n" +
                "User: \"Can you open camera\" → {\"intent\": \"OPEN\", \"target\": \"CAMERA\"}\n" +
                "User: \"Save number 9876543210 as Steve\" → {\"intent\": \"SAVE\", \"target\": \"Steve\", \"task\": \"9876543210\"}\n" +
                "User: \"Jarvis, you can sleep now.\" → {\"intent\": \"DEACTIVATE\", \"target\": \"SHUTDOWN\"}\n" +
                "User: \"Switch off, Jarvis.\" → {\"intent\": \"DEACTIVATE\", \"target\": \"SHUTDOWN\"}\n" +
                "User: \"Share steve contact to lokesh\" → {\"intent\": \"SHARE CONTACT\", \"target\": \"Lokesh\",  \"task\": \"steve\"}\n" +
                "User: \"Can you copy steve number\" → {\"intent\": \"COPY NUMBER\", \"task\": \"steve\"}\n" +


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
        generativeModel = new GenerativeModel("gemini-2.0-flash", GeminiApikey);
        modelFutures = GenerativeModelFutures.from(generativeModel);
    }
}
