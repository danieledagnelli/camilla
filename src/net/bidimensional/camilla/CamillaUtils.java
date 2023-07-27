package net.bidimensional.camilla;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.jfree.data.json.impl.JSONArray;
import org.jfree.data.json.impl.JSONObject;

public class CamillaUtils {

    private static final String GPT_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "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";

    public static String getResponseFromGpt4(String xmlInput) throws IOException {
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "This is an XML representing graph of relationships for a Digital Forensic investigation case. Write a report based on it:");

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", xmlInput);

        JSONArray messages = new JSONArray();
        messages.add(systemMessage.toString());
        messages.add(userMessage.toString());

        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("messages", messages.toString());
        jsonPayload.put("max_tokens", 200);
        jsonPayload.put("model", "gpt-4");

        URL url = new URL(GPT_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
        connection.setDoOutput(true);

        try ( DataOutputStream writer = new DataOutputStream(connection.getOutputStream())) {
            writer.write(jsonPayload.toString().getBytes(StandardCharsets.UTF_8));
            writer.flush();
        }

        int status = connection.getResponseCode();

        BufferedReader reader;
        if (status > 299) {
            reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        }

        String line;
        StringBuilder response = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        if (status > 299) {
            throw new RuntimeException("HTTP response error: " + status + " - " + response.toString());
        }

        return response.toString();
    }
}
