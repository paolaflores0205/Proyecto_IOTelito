package com.example.proyecto_iotelito.model;

import java.io.Serializable;

/**
 * Modelo de datos de un hotel. Por ahora la información es estática
 * (ver {@link com.example.proyecto_iotelito.data.SampleData}); en un
 * entregable posterior se reemplazará por la base de datos NoSQL.
 */
public class Hotel implements Serializable {

    public final int id;
    public final String city;
    public final String name;
    public final String address;
    public final double rating;
    public final int reviews;
    public final String description;
    public final double pricePerNight;
    public final String[] services;
    public final Attraction[] attractions;

    public final String roomName;
    public final String roomCapacity;
    public final String roomSize;
    public final String[] roomFeatures;

    public Hotel(int id, String city, String name, String address, double rating, int reviews,
                 String description, double pricePerNight, String[] services, Attraction[] attractions,
                 String roomName, String roomCapacity, String roomSize, String[] roomFeatures) {
        this.id = id;
        this.city = city;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.reviews = reviews;
        this.description = description;
        this.pricePerNight = pricePerNight;
        this.services = services;
        this.attractions = attractions;
        this.roomName = roomName;
        this.roomCapacity = roomCapacity;
        this.roomSize = roomSize;
        this.roomFeatures = roomFeatures;
    }
}
