package com.example.proyecto_iotelito.ui.superadmin;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.proyecto_iotelito.data.superadmin.SuperadminRepository;
import com.example.proyecto_iotelito.model.superadmin.ActivityLog;
import com.example.proyecto_iotelito.model.superadmin.AdminUser;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import java.util.List;

/** Conserva el estado de la operación al girar la pantalla y observa Room. */
public class SuperadminViewModel extends AndroidViewModel {
    public static class Result {
        public final int id;
        public final String error;
        private boolean consumed;
        Result(int id, String error) { this.id = id; this.error = error; }
        public boolean consume() {
            if (consumed) return false;
            consumed = true;
            return true;
        }
    }
    private final SuperadminRepository repository;
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Result> result = new MutableLiveData<>();

    public SuperadminViewModel(@NonNull Application app) {
        super(app);
        repository = SuperadminRepository.getInstance(app);
    }
    public LiveData<List<ManagedHotel>> hotels() { return repository.hotels(); }
    public LiveData<List<AdminUser>> users() { return repository.users(); }
    public LiveData<List<ActivityLog>> logs() { return repository.logs(); }
    public LiveData<ManagedHotel> hotel(int id) { return repository.hotel(id); }
    public LiveData<AdminUser> user(int id) { return repository.user(id); }
    public LiveData<Boolean> ready() { return repository.ready(); }
    public LiveData<String> error() { return repository.error(); }
    public LiveData<Boolean> busy() { return busy; }
    public LiveData<Result> result() { return result; }
    private boolean start() {
        if (Boolean.TRUE.equals(busy.getValue())) return false;
        busy.setValue(true);
        return true;
    }
    private final SuperadminRepository.Callback callback = (id, error) -> {
        busy.setValue(false);
        result.setValue(new Result(id, error));
    };
    public void saveHotel(ManagedHotel hotel) { if (start()) repository.saveHotel(hotel, callback); }
    public void assign(int hotel, int user) { if (start()) repository.assignAdministrator(hotel, user, callback); }
    public void userStatus(int id, boolean active, String reason) {
        if (start()) repository.setUserActive(id, active, reason, callback);
    }
    public void hotelStatus(int id, boolean active) { if (start()) repository.setHotelActive(id, active, callback); }
}
