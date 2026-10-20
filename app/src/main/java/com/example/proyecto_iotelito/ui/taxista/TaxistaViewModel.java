package com.example.proyecto_iotelito.ui.taxista;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import com.example.proyecto_iotelito.data.taxista.ConductorRepository;
public class TaxistaViewModel extends AndroidViewModel {
    public final ConductorRepository repo;
    public TaxistaViewModel(@NonNull Application app) {super(app);repo=new ConductorRepository(app);}
}
