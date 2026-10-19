package com.example.proyecto_iotelito.data.superadmin.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface SuperadminDao {
    @Query("SELECT * FROM admin_hotels ORDER BY id") LiveData<List<AdminHotelEntity>> observeHotels();
    @Query("SELECT * FROM admin_users ORDER BY id") LiveData<List<AdminUserEntity>> observeUsers();
    @Query("SELECT * FROM admin_events ORDER BY createdAt DESC, id DESC") LiveData<List<AdminLogEntity>> observeLogs();
    @Query("SELECT * FROM admin_alerts ORDER BY createdAt DESC") LiveData<List<AdminAlertEntity>> observeAlerts();
    @Query("SELECT * FROM admin_hotels WHERE id = :id") LiveData<AdminHotelEntity> observeHotel(int id);
    @Query("SELECT * FROM admin_users WHERE id = :id") LiveData<AdminUserEntity> observeUser(int id);
    @Query("SELECT * FROM admin_hotels WHERE id = :id") AdminHotelEntity hotel(int id);
    @Query("SELECT * FROM admin_users WHERE id = :id") AdminUserEntity user(int id);
    @Query("SELECT * FROM admin_hotels ORDER BY id") List<AdminHotelEntity> hotels();
    @Query("SELECT COUNT(*) FROM admin_hotels") int countHotels();
    @Query("SELECT COUNT(*) FROM admin_users") int countUsers();
    @Insert long insertHotel(AdminHotelEntity hotel);
    @Update int updateHotel(AdminHotelEntity hotel);
    @Insert(onConflict = OnConflictStrategy.IGNORE) void insertUser(AdminUserEntity user);
    @Update int updateUser(AdminUserEntity user);
    @Insert void insertLog(AdminLogEntity log);
    @Insert(onConflict = OnConflictStrategy.IGNORE) long insertAlert(AdminAlertEntity alert);
    @Query("SELECT * FROM admin_alerts WHERE notified = 0 AND `read` = 0") List<AdminAlertEntity> pendingAlerts();
    @Query("UPDATE admin_alerts SET notified = 1 WHERE `key` = :key") void markNotified(String key);
    @Query("UPDATE admin_alerts SET `read` = 1 WHERE hotelId = :hotelId") void markRead(int hotelId);
    @Query("DELETE FROM admin_alerts WHERE `key` = :key") void deleteAlert(String key);
    @Query("UPDATE admin_alerts SET message = :message WHERE `key` = :key AND message != :message")
    void updateAlertMessage(String key, String message);
}
