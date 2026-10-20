package com.example.proyecto_iotelito.ui.taxista;
import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;
public class EditarFotoTaxistaActivity extends AppCompatActivity {
    private TaxistaViewModel vm;
    private final ActivityResultLauncher<PickVisualMediaRequest> picker=registerForActivityResult(
        new ActivityResultContracts.PickVisualMedia(),uri->{
            if(uri!=null){habilitar(false);vm.repo.ejecutar(this,()->ConductorFoto.guardar(this,vm.repo.cuenta,uri),ok->{habilitar(true);finish();});}
        });
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);if(!TaxistaFlujo.sesion(this))return;setContentView(R.layout.activity_editar_foto_taxista);
        vm=TaxistaFlujo.model(this);
        android.view.View header=findViewById(R.id.sub_header);
        header.findViewById(R.id.iv_back).setOnClickListener(v->finish());
        ((TextView)header.findViewById(R.id.tv_title)).setText(R.string.taxi_menu_editar_foto);
        ((TextView)findViewById(R.id.tv_avatar_inicial)).setText("MA");
        ConductorUi.foto(this,findViewById(android.R.id.content),vm);
        findViewById(R.id.btn_cambiar_foto).setOnClickListener(v->picker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        findViewById(R.id.btn_quitar_foto).setOnClickListener(v->{habilitar(false);vm.repo.ejecutar(this,()->{
            java.io.File f=ConductorFoto.archivo(this,vm.repo.cuenta);
            if(f.exists()&&!f.delete())throw new java.io.IOException("No se pudo quitar la foto");return true;
        },ok->{habilitar(true);finish();});});
        vm.repo.error.observe(this,error->{if(error!=null)habilitar(true);});
    }
    private void habilitar(boolean enabled){findViewById(R.id.btn_cambiar_foto).setEnabled(enabled);findViewById(R.id.btn_quitar_foto).setEnabled(enabled);}
}
