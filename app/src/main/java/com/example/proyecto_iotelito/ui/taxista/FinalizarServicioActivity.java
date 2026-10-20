package com.example.proyecto_iotelito.ui.taxista;
import android.os.Bundle;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
/** El lector QR remoto sigue pendiente; se ofrece una validación local de demostración. */
public class FinalizarServicioActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);if(!TaxistaFlujo.sesion(this))return;setContentView(R.layout.activity_finalizar_servicio);
        TaxistaViewModel vm=TaxistaFlujo.model(this);
        findViewById(R.id.iv_back).setOnClickListener(v->finish());
        TaxistaFlujo.cargar(this,vm,e->{
            if(!"EN_TRASLADO".equals(e.estado)){TaxistaFlujo.abrir(this,e);finish();return;}
            // Código vinculado al servicio local, visible explícitamente como demostración.
            String codigo="TAXI-"+e.id;
            EditText input=findViewById(R.id.et_codigo_manual);
            input.setHint(getString(R.string.taxista_local_codigo,codigo));
            findViewById(R.id.btn_validar_codigo).setOnClickListener(v->{
                if(!codigo.equalsIgnoreCase(input.getText().toString().trim())) {
                    input.setError(getString(R.string.taxista_local_codigo_error,codigo));return;
                }
                new MaterialAlertDialogBuilder(this).setTitle(R.string.taxista_local_validacion_demo)
                        .setMessage(R.string.taxista_local_validacion_desc)
                        .setNegativeButton(android.R.string.cancel,null)
                        .setPositiveButton(R.string.taxista_local_confirmar,(d,w)->{
                            findViewById(R.id.btn_validar_codigo).setEnabled(false);
                            vm.repo.ejecutar(this,()->vm.repo.avanzar(e.id,EstadoServicio.FINALIZADO),ok->{
                                findViewById(R.id.btn_validar_codigo).setEnabled(true);
                                if(ok){startActivity(TaxistaFlujo.intent(this,ServicioFinalizadoActivity.class,e.id));finish();}
                            });
                        }).show();
            });
            vm.repo.error.observe(this,error->{if(error!=null)findViewById(R.id.btn_validar_codigo).setEnabled(true);});
        });
    }
}
