package com.example.fitman.weather;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherService {



    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onResult(WeatherData data);
        void onError(Exception e);
    }

    public static void fetch(double lat, double lon, Callback callback) {
        executor.execute(() -> {
            try {
                WeatherData data = fetchSync(lat, lon);
                mainHandler.post(() -> callback.onResult(data));
            } catch (Exception e) {
                Log.e("WeatherService", "fetch failed", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private static WeatherData fetchSync(double lat, double lon) throws Exception {
        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + lat + "&longitude=" + lon
                + "&current=temperature_2m,cloud_cover,precipitation"
                + "&minutely_15=temperature_2m,cloud_cover,precipitation"
                + "&forecast_days=1&timezone=Asia%2FKolkata";

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestMethod("GET");

        int code = conn.getResponseCode();
        if (code != 200) throw new RuntimeException("Weather API returned " + code);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        conn.disconnect();

        return parse(new JSONObject(sb.toString()));
    }

    private static WeatherData parse(JSONObject root) throws Exception {
        WeatherData data = new WeatherData();

        JSONObject current = root.getJSONObject("current");
        data.currentTime = current.getString("time");
        data.currentTemp = current.getDouble("temperature_2m");
        data.currentCloud = current.getInt("cloud_cover");
        data.currentPrecip = current.getDouble("precipitation");

        JSONObject m15 = root.getJSONObject("minutely_15");
        JSONArray times = m15.getJSONArray("time");
        JSONArray temps = m15.getJSONArray("temperature_2m");
        JSONArray clouds = m15.getJSONArray("cloud_cover");
        JSONArray precs = m15.getJSONArray("precipitation");

        List<WeatherSlot> slots = new ArrayList<>();
        for (int i = 0; i < times.length(); i++) {
            WeatherSlot s = new WeatherSlot();
            s.time = times.getString(i);
            s.temp = temps.getDouble(i);
            s.cloudCover = clouds.getInt(i);
            s.precipitation = precs.getDouble(i);
            slots.add(s);
        }
        data.slots = slots;
        return data;
    }
}