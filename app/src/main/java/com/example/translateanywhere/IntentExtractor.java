package com.example.translateanywhere;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class IntentExtractor {
    private final Context context;
    private final OkHttpClient client;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final String NGROK_URL = "https://stably-oversusceptible-anne.ngrok-free.dev/api/chat";
    private static final String OLLAMA_MODEL = "mistral";

    // ✅ Static HashMaps for Keywords and Examples (Loads only once for max speed)
    private static final Map<String, String[]> keywordsMap = new HashMap<>();
    private static final Map<String, String> examplesMap = new HashMap<>();



    // 🔴 NEW: Fast Local Action Checker Keywords
    // 🔴 UPGRADED: Fast Local Action Checker Keywords (Removed trailing spaces, added System toggles)
    private static final String[] ACTION_KEYWORDS = {
            "call", "dial", "ring", "open", "launch", "start",
            "install", "download", "remind", "alert", "set",
            "play", "stop", "pause", "music", "song",
            "message", "text", "sms", "tell", "send", "share",
            "enable", "disable", "turn on", "turn off", "switch",
            "deactivate", "sleep", "clear memory", "delete memory",
            "wifi", "hotspot", "flashlight", "torch", "internet", "data"
    };

    static {
        // 1. KEYWORDS MAPPING
        keywordsMap.put("MESSAGE", new String[]{"text", "message", "sms", "tell", "reply"});
        keywordsMap.put("CALL", new String[]{"call", "dial", "phone", "ring"});
        keywordsMap.put("OPEN", new String[]{"open", "launch", "start app"});
        keywordsMap.put("REMINDER", new String[]{"remind", "alert", "set a", "wake"});
        keywordsMap.put("INSTALL", new String[]{"install", "download", "get"});
        keywordsMap.put("PLAY_MUSIC", new String[]{"play music", "play song", "start music", "play some"});
        keywordsMap.put("STOP_MUSIC", new String[]{"stop music", "pause song", "halt music", "stop playing"});
        keywordsMap.put("SHARE_CONTACT", new String[]{"share contact", "send number", "contact details"});
        keywordsMap.put("SEND_FILE", new String[]{"send this", "display in", "send file", "transfer"});
        keywordsMap.put("DEACTIVATE", new String[]{"go to sleep", "deactivate", "stop jarvis", "moodu"});
        keywordsMap.put("ENABLE_AUTO_REPLY", new String[]{"enable auto reply", "turn on auto reply", "enable auto sms", "auto reply on"});
        keywordsMap.put("DISABLE_AUTO_REPLY", new String[]{"disable auto reply", "turn off auto reply", "disable auto sms", "auto reply off", "stop replying"});
        // 2. EXAMPLES MAPPING
        examplesMap.put("MESSAGE",
                "Query: \"text ravi saying hello there\"\nOutput: {\"intent\":\"MESSAGE\", \"target\":\"ravi\", \"message\":\"hello there\", \"time\":\"\"}\n" +
                        "Query: \"send sms to mom i am coming home\"\nOutput: {\"intent\":\"MESSAGE\", \"target\":\"mom\", \"message\":\"i am coming home\", \"time\":\"\"}\n" +
                        "Query: \"message boss that I will be late\"\nOutput: {\"intent\":\"MESSAGE\", \"target\":\"boss\", \"message\":\"I will be late\", \"time\":\"\"}\n" +
                        "Query: \"tell gopi to join the match\"\nOutput: {\"intent\":\"MESSAGE\", \"target\":\"gopi\", \"message\":\"join the match\", \"time\":\"\"}\n");

        examplesMap.put("CALL",
                "Query: \"call my brother\"\nOutput: {\"intent\":\"CALL\", \"target\":\"brother\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"dial 9876543210\"\nOutput: {\"intent\":\"CALL\", \"target\":\"9876543210\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"make a phone call to dad\"\nOutput: {\"intent\":\"CALL\", \"target\":\"dad\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"ring up steve\"\nOutput: {\"intent\":\"CALL\", \"target\":\"steve\", \"message\":\"\", \"time\":\"\"}\n");

        examplesMap.put("OPEN",
                "Query: \"open youtube\"\nOutput: {\"intent\":\"OPEN\", \"target\":\"youtube\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"launch whatsapp\"\nOutput: {\"intent\":\"OPEN\", \"target\":\"whatsapp\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"start the calculator app\"\nOutput: {\"intent\":\"OPEN\", \"target\":\"calculator\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"open free fire max\"\nOutput: {\"intent\":\"OPEN\", \"target\":\"free fire max\", \"message\":\"\", \"time\":\"\"}\n");

        examplesMap.put("REMINDER",
                "Query: \"remind me to buy milk at 5 PM\"\nOutput: {\"intent\":\"REMINDER\", \"target\":\"\", \"message\":\"buy milk\", \"time\":\"5 PM\"}\n" +
                        "Query: \"set a reminder for meeting tomorrow morning\"\nOutput: {\"intent\":\"REMINDER\", \"target\":\"\", \"message\":\"meeting\", \"time\":\"tomorrow morning\"}\n" +
                        "Query: \"alert me to call steve at 9 pm\"\nOutput: {\"intent\":\"REMINDER\", \"target\":\"steve\", \"message\":\"call steve\", \"time\":\"9 pm\"}\n");

        examplesMap.put("INSTALL",
                "Query: \"install facebook\"\nOutput: {\"intent\":\"INSTALL\", \"target\":\"facebook\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"download instagram\"\nOutput: {\"intent\":\"INSTALL\", \"target\":\"instagram\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"get whatsapp from store\"\nOutput: {\"intent\":\"INSTALL\", \"target\":\"whatsapp\", \"message\":\"\", \"time\":\"\"}\n");

        examplesMap.put("PLAY_MUSIC",
                "Query: \"play music\"\nOutput: {\"intent\":\"PLAY_MUSIC\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"start playing songs\"\nOutput: {\"intent\":\"PLAY_MUSIC\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"play believer song\"\nOutput: {\"intent\":\"PLAY_MUSIC\", \"target\":\"believer\", \"message\":\"\", \"time\":\"\"}\n");

        examplesMap.put("STOP_MUSIC",
                "Query: \"stop music\"\nOutput: {\"intent\":\"STOP_MUSIC\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"pause the song\"\nOutput: {\"intent\":\"STOP_MUSIC\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n");

        examplesMap.put("SHARE_CONTACT",
                "Query: \"share mom's contact\"\nOutput: {\"intent\":\"SHARE_CONTACT\", \"target\":\"mom\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"send ravi's number\"\nOutput: {\"intent\":\"SHARE_CONTACT\", \"target\":\"ravi\", \"message\":\"\", \"time\":\"\"}\n");

        examplesMap.put("SEND_FILE",
                "Query: \"send this file\"\nOutput: {\"intent\":\"SEND_FILE\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"display in pc\"\nOutput: {\"intent\":\"SEND_FILE\", \"target\":\"pc\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"send image to laptop\"\nOutput: {\"intent\":\"SEND_FILE\", \"target\":\"laptop\", \"message\":\"\", \"time\":\"\"}\n");

        examplesMap.put("DEACTIVATE",
                "Query: \"go to sleep\"\nOutput: {\"intent\":\"DEACTIVATE\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"stop jarvis\"\nOutput: {\"intent\":\"DEACTIVATE\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"moodu\"\nOutput: {\"intent\":\"DEACTIVATE\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n");

        examplesMap.put("ENABLE_AUTO_REPLY",
                "Query: \"enable auto reply\"\nOutput: {\"intent\":\"ENABLE_AUTO_REPLY\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"turn on auto sms\"\nOutput: {\"intent\":\"ENABLE_AUTO_REPLY\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"jarvis start answering my messages\"\nOutput: {\"intent\":\"ENABLE_AUTO_REPLY\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n");

        examplesMap.put("DISABLE_AUTO_REPLY",
                "Query: \"disable auto reply\"\nOutput: {\"intent\":\"DISABLE_AUTO_REPLY\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"turn off auto sms\"\nOutput: {\"intent\":\"DISABLE_AUTO_REPLY\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"stop replying to my messages\"\nOutput: {\"intent\":\"DISABLE_AUTO_REPLY\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n");


        examplesMap.put("UNKNOWN",
                "Query: \"who are you\"\nOutput: {\"intent\":\"UNKNOWN\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n" +
                        "Query: \"what is the weather\"\nOutput: {\"intent\":\"UNKNOWN\", \"target\":\"\", \"message\":\"\", \"time\":\"\"}\n");
    }

    public interface ExtractorCallback {
        void onResult(String intent, String target, String message, String time);
    }

    public IntentExtractor(Context context) {
        this.context = context;
        client = new OkHttpClient.Builder()
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public void extract(String userQuery, ExtractorCallback callback) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            callback.onResult("UNKNOWN", "", "", "");
            return;
        }

        String query = userQuery.toLowerCase().trim();

        if (handleManualExtraction(query, callback)) {
            Log.d("IntentExtractor", "Action Handled Manually: " + query);
            return;
        }

        if (!isLikelyAnAction(userQuery)) {
            Log.d("IntentExtractor", "Local Gatekeeper: This is just a chat. Skipping API Intent check. (0ms delay)");
            callback.onResult("UNKNOWN", "", "", "");
            return;
        }

        processWithJarvisBrain(userQuery, callback);
    }

    private boolean isLikelyAnAction(String query) {
        for (String keyword : ACTION_KEYWORDS) {
            if (query.contains(keyword)) {
                Log.d("IntentExtractor", "Local Gatekeeper: This is an action.");
                return true;
                // Yep, looks like they want to do something on the phone
            }
            Log.d("IntentExtractor", "Local Gatekeeper: This is just a chat.");
        }
        return false; // Nope, just normal chatting. Skip the AI call!
    }
    private boolean handleManualExtraction(String q, ExtractorCallback callback) {


        if(q.startsWith("call to ") || q.startsWith("dial to ")){
            String target = q.replace("call to ", "").replace("dial to ", "").trim();
            callback.onResult("CALL",target,"","");
            return true;
        }
        // 1. CALL INTENT (Extracts target name/number)
        if (q.startsWith("call ") || q.startsWith("dial ") || q.startsWith("ring ")) {
            String target = q.replace("call ", "").replace("dial ", "").replace("ring ", "").trim();
            callback.onResult("CALL", target, "", "");
            return true;
        }

        // 2. OPEN INTENT (Extracts app name)
        if (q.startsWith("open ") || q.startsWith("launch ") || q.startsWith("start app ")) {
            String target = q.replace("open ", "").replace("launch ", "").replace("start app ", "").trim();
            callback.onResult("OPEN", target, "", "");
            return true;
        }

        // 3. INSTALL INTENT (Extracts app name to download)
        if (q.startsWith("install ") || q.startsWith("download ") || q.startsWith("get app ")) {
            String target = q.replace("install ", "").replace("download ", "").replace("get app ", "").trim();
            callback.onResult("INSTALL", target, "", "");
            return true;
        }

        // 4. MUSIC CONTROLS (Play specific song or just play/stop)
        if (q.equals("play music") || q.equals("start music") || q.equals("play a song") || q.equals("play songs")) {
            callback.onResult("PLAY_MUSIC", "", "", "");
            return true;
        }
        if (q.startsWith("play song ") || q.startsWith("play music ") || q.startsWith("play ")) {
            String target = q.replace("play song ", "").replace("play music ", "").replace("play ", "").trim();
            callback.onResult("PLAY_MUSIC", target, "", "");
            return true;
        }
        if (q.contains("stop music") || q.contains("pause song") || q.contains("pause music") || q.contains("stop playing")) {
            callback.onResult("STOP_MUSIC", "", "", "");
            return true;
        }

        // 5. AUTO REPLY TOGGLES
        if (q.contains("enable auto reply") || q.contains("turn on auto reply") || q.contains("auto reply on") || q.contains("start answering my messages")) {
            callback.onResult("ENABLE_AUTO_REPLY", "", "", "");
            return true;
        }
        if (q.contains("disable auto reply") || q.contains("turn off auto reply") || q.contains("auto reply off") || q.contains("stop replying")) {
            callback.onResult("DISABLE_AUTO_REPLY", "", "", "");
            return true;
        }

        // 6. SHARE CONTACT (Extracts name. e.g., "share mom's contact" -> "mom")
        if ((q.startsWith("share ") && q.contains(" contact")) || (q.startsWith("send ") && q.contains(" number"))) {
            String target = q.replace("share ", "").replace(" contact", "")
                    .replace("send ", "").replace(" number", "")
                    .replace("'s", "").trim();
            callback.onResult("SHARE_CONTACT", target, "", "");
            return true;
        }

        // 8. DEACTIVATE / SLEEP
        if (q.contains("deactivate") || q.contains("go to sleep") || q.contains("stop jarvis") || q.contains("go offline")) {
            callback.onResult("DEACTIVATE", "", "", "");
            return true;
        }

        // 9. MESSAGE INTENT (Pattern: "[Message] send to [Target]")
        // Example: "i will be late send to boss"
        if (q.contains(" send to ")) {
            // " send to " apdingra word-ah vechu string-ah rendu aakkurom
            String[] parts = q.split(" send to ");

            // Array la 2 parts irukka nu check pandrom (0 = message, 1 = target)
            if (parts.length == 2) {
                String message = parts[0].trim(); // Munnadi irukkurathu
                String target = parts[1].trim();  // Pinnadi irukkurathu

                // Rendum empty ah illana mattum extract panniduvom
                if (!message.isEmpty() && !target.isEmpty()) {
                    callback.onResult("MESSAGE", target, message, "");
                    return true;
                }
            }
        }

        if(q.startsWith("clear memory") || q.startsWith("delete memory")){
            callback.onResult("CLEAR_MEMORY", "", "", "");
            return true;
        }

        // 10. AUTOMATIONS (WiFi, Hotspot, Flashlight, Internet)
        if (q.contains("wifi")) {
            if (q.contains("on") || q.contains("enable") || q.contains("start")) {
                callback.onResult("TURN_ON_WIFI", "", "", "");
            } else if (q.contains("off") || q.contains("disable") || q.contains("stop")) {
                callback.onResult("TURN_OFF_WIFI", "", "", "");
            }
            return true;
        }

        if (q.contains("hotspot")) {
            if (q.contains("on") || q.contains("enable") || q.contains("start")) {
                callback.onResult("TURN_ON_HOTSPOT", "", "", "");
            } else if (q.contains("off") || q.contains("disable") || q.contains("stop")) {
                callback.onResult("TURN_OFF_HOTSPOT", "", "", "");
            }
            return true;
        }

        if (q.contains("flashlight") || q.contains("torch")) {
            if (q.contains("on") || q.contains("enable") || q.contains("start")) {
                callback.onResult("TURN_ON_FLASHLIGHT", "", "", "");
            } else if (q.contains("off") || q.contains("disable") || q.contains("stop")) {
                callback.onResult("TURN_OFF_FLASHLIGHT", "", "", "");
            }
            return true;
        }

        if (q.contains("internet") || q.contains("mobile data") || q.contains("data")) {
            if (q.contains("on") || q.contains("enable") || q.contains("start")) {
                callback.onResult("TURN_ON_DATA", "", "", "");
            } else if (q.contains("off") || q.contains("disable") || q.contains("stop")) {
                callback.onResult("TURN_OFF_DATA", "", "", "");
            }
            return true;
        }

        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void processWithJarvisBrain(String userQuery, ExtractorCallback callback) {
        Log.d("IntentExtractor", "Sending to Jarvis Brain...");

        executorService.execute(() -> {
            try {
                String systemPrompt = getDynamicSystemPrompt(userQuery);

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("model", OLLAMA_MODEL);
                jsonBody.put("stream", false);
                JSONArray messages = new JSONArray();

                JSONObject systemMsg = new JSONObject();
                systemMsg.put("role", "system");
                systemMsg.put("content", systemPrompt);
                messages.put(systemMsg);

                JSONObject currentMsg = new JSONObject();
                currentMsg.put("role", "user");
                currentMsg.put("content", "Query: \"" + userQuery + "\"\nOutput:");
                messages.put(currentMsg);

                jsonBody.put("messages", messages);

                RequestBody body = RequestBody.create(
                        jsonBody.toString(),
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(NGROK_URL)
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e("IntentExtractor", "API call failed", e);
                        callback.onResult("UNKNOWN", "", "", "");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.d("IntentExtractor", "API call successful" +response);
                        if (response.isSuccessful()) {
                            try {
                                String responseString = response.body().string();
                                JSONObject jsonResponse = new JSONObject(responseString);
                                String extractedContent = jsonResponse.getJSONObject("message").getString("content");
                                Log.d("IntentExtractor", "RAW AI Response Data: " + responseString);
                                extractedContent = extractedContent.replaceAll("(?s)<think>.*?</think>", "").trim();
                                extractedContent = extractedContent.replace("```json", "").replace("```", "").trim();

                                JSONObject intentJson = new JSONObject(extractedContent);
                                String intent = intentJson.optString("intent", "UNKNOWN").toUpperCase();
                                String target = intentJson.optString("target", "");
                                String message = intentJson.optString("message", "");
                                String time = intentJson.optString("time", "");

                                callback.onResult(intent, target, message, time);

                            } catch (Exception e) {
                                Log.e("IntentExtractor", "Failed to parse JSON", e);
                                callback.onResult("UNKNOWN", "", "", "");
                            }
                        } else {
                            callback.onResult("UNKNOWN", "", "", "");
                        }
                    }
                });

            } catch (Exception e) {
                Log.e("IntentExtractor", "Error preparing request", e);
                callback.onResult("UNKNOWN", "", "", "");
            }
        });
    }



    private String getDynamicSystemPrompt(String query) {
        String basePrompt = "You are an Intent Extraction engine. Extract intent, target, message, and time from the user query.\n" +
                "ALLOWED INTENTS: CALL, MESSAGE, OPEN, REMINDER, PLAY_MUSIC, STOP_MUSIC, INSTALL, SHARE_CONTACT, ENABLE AUTO REPLY , DISABLE AUTO REPLY , DEACTIVATE, UNKNOWN\n\n" +
                "CRITICAL RULE: Return ONLY a raw JSON object. No explanation, no markdown formatting.\n\n" +
                "EXAMPLES:\n";

        StringBuilder dynamicExamples = new StringBuilder();
        String q = query.toLowerCase();
        boolean matched = false;

        // Loop through our keywords map
        for (Map.Entry<String, String[]> entry : keywordsMap.entrySet()) {
            String intentName = entry.getKey();
            String[] triggers = entry.getValue();

            for (String trigger : triggers) {
                if (q.contains(trigger)) {
                    dynamicExamples.append(examplesMap.get(intentName)).append("\n");
                    matched = true;
                    break; // Break inner loop so we don't append same intent examples multiple times
                }
            }
        }

        // If nothing matches, fallback to UNKNOWN examples
        if (!matched) {
            dynamicExamples.append(examplesMap.get("UNKNOWN"));
        }

        return basePrompt + dynamicExamples.toString();
    }
}