package com.example.proyecto_iotelito.data.superadmin.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;

@Entity(tableName = "admin_hotels")
public class AdminHotelEntity {
    @PrimaryKey(autoGenerate = true) public int id;
    public String name, address, district, administrator, city, phone, email, description;
    public int rooms, reservations, imageRes;
    public boolean active;
    public Double latitude, longitude;
    public Integer administratorId;

    public static AdminHotelEntity from(ManagedHotel hotel) {
        AdminHotelEntity row = new AdminHotelEntity();
        row.id = hotel.id;
        row.name = hotel.name;
        row.address = hotel.address;
        row.district = hotel.district;
        row.administrator = hotel.administrator;
        row.rooms = hotel.rooms;
        row.reservations = hotel.reservations;
        row.imageRes = hotel.imageRes;
        row.active = hotel.active;
        row.city = hotel.city;
        row.phone = hotel.phone;
        row.email = hotel.email;
        row.description = hotel.description;
        row.latitude = hotel.latitude;
        row.longitude = hotel.longitude;
        row.administratorId = hotel.administratorId;
        return row;
    }

    public ManagedHotel toModel() {
        ManagedHotel hotel = new ManagedHotel(id, name, address, district, administrator,
                rooms, reservations, imageRes, active);
        hotel.city = city;
        hotel.phone = phone;
        hotel.email = email;
        hotel.description = description;
        hotel.latitude = latitude;
        hotel.longitude = longitude;
        hotel.administratorId = administratorId;
        return hotel;
    }
}
