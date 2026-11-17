package com.example.proyecto_iotelito.ui.taxi;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.data.TaxiRepository;
import com.example.proyecto_iotelito.data.taxiapi.*;

import org.json.JSONObject;

/** Base de las pantallas de seguimiento: consultas solo mientras están en primer plano. */
public abstract class TaxiServiceScreen extends AppCompatActivity {
    protected int serviceId;
    protected TaxiApi api;

    protected void observeService() {
        serviceId = getIntent().getIntExtra(TaxiRepository.EXTRA_ID, -1);
        if (serviceId < 1) {
            TaxiUi.status(this, "No se recibió el identificador del servicio.");
            return;
        }
        try {
            api = TaxiApis.get(this);
        } catch (Exception e) {
            TaxiUi.status(this, TaxiTasks.message(e));
            return;
        }
        TaxiUi.status(this, null);
        TaxiTasks.poll(
                this,
                5000,
                () -> api.verServicio(serviceId),
                s -> {
                    TaxiUi.status(this, null);
                    render(s);
                },
                e -> TaxiUi.status(this, e));
    }

    protected abstract void render(JSONObject service);

    protected android.content.Intent intent(Class<?> target) {
        return new android.content.Intent(this, target)
                .putExtra(TaxiRepository.EXTRA_ID, serviceId);
    }
}
