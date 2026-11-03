package com.example.proyecto_iotelito.ui.taxista;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import com.example.proyecto_iotelito.data.taxista.ConductorRepository;
import com.example.proyecto_iotelito.data.taxista.DisponibilidadRepository;
public class TaxistaViewModel extends AndroidViewModel {
    public final ConductorRepository repo;
    public final DisponibilidadRepository disponibilidad;
    public TaxistaViewModel(@NonNull Application app) {
        super(app);repo=new ConductorRepository(app);
        disponibilidad=DisponibilidadRepository.adquirir(app,repo.cuenta);
    }
    @Override protected void onCleared() { repo.close();disponibilidad.close();super.onCleared(); }
}
