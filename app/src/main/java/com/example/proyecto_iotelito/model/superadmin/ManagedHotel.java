package com.example.proyecto_iotelito.model.superadmin;

public class ManagedHotel {
    public final int id;
    public final String name;
    public final String address;
    public final String district;
    public final String administrator;
    public final int rooms;
    public final int reservations;
    public final int imageRes;
    public boolean active;

    public ManagedHotel(int id, String name, String address, String district,
                        String administrator, int rooms, int reservations,
                        int imageRes, boolean active) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.district = district;
        this.administrator = administrator;
        this.rooms = rooms;
        this.reservations = reservations;
        this.imageRes = imageRes;
        this.active = active;
    }
}
