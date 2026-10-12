package com.example.fitman.weather;

import java.util.List;

public class WeatherData {
    public String currentTime;
    public double currentTemp;
    public int currentCloud;
    public double currentPrecip;
    public List<WeatherSlot> slots;   // 15-min forecast for the rest of today
}