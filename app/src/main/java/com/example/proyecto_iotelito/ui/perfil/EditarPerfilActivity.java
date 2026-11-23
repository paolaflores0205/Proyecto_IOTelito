package com.example.proyecto_iotelito.ui.perfil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.proyecto_iotelito.MainActivity;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.*;
import com.example.proyecto_iotelito.model.UserProfile;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Solo modifica los campos permitidos; documento, nacimiento y correo nunca se envían al guardar. */
public class EditarPerfilActivity extends AppCompatActivity {
    private EditText nombres, apellidos, telefono, domicilio;
    private String uid, fotoPreparada;
    private boolean cargado, guardando, preparando, restaurado;
    private final ActivityResultLauncher<String> elegirFoto = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri == null) return;
        preparando = true; actualizarBotones();
        ClientLocalTasks.run(this,this,()->ClientProfilePhotos.preparar(this,uri), path->{
            if(isFinishing()||isDestroyed())return;
            fotoPreparada=path; preparando=false; actualizarBotones();
            HotelImageLoader.show(findViewById(R.id.iv_perfil_foto),android.net.Uri.fromFile(new java.io.File(path)).toString());
            estado("Vista previa. La foto se subirá al guardar.");
        },()->{preparando=false;actualizarBotones();estado("No se pudo preparar la imagen. Elige otra foto.");});
    });
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved); setContentView(R.layout.activity_editar_perfil);
        com.google.firebase.auth.FirebaseUser user=com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if(user==null){finish();return;}uid=user.getUid();restaurado=saved!=null;
        if(saved!=null && saved.containsKey("perfil_uid") && !uid.equals(saved.getString("perfil_uid"))){finish();return;}
        if(saved!=null)fotoPreparada=saved.getString("foto_preparada");
        nombres=findViewById(R.id.et_nombre);apellidos=findViewById(R.id.et_apellidos);
        telefono=findViewById(R.id.et_telefono);domicilio=findViewById(R.id.et_domicilio);
        com.example.proyecto_iotelito.ui.common.FormKeyboard.attach(this,findViewById(R.id.scroll_editar_perfil),
                nombres,apellidos,telefono,domicilio);
        nombres.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});
        for(int id:new int[]{R.id.actv_tipo_doc,R.id.et_num_doc,R.id.et_fecha_nacimiento,R.id.et_email}) {
            EditText field=findViewById(id);field.setKeyListener(null);field.setFocusable(false);
            field.setTextColor(getColor(R.color.io_text_secondary));
        }
        pintar(PerfilRepository.cargar(this),true);
        findViewById(R.id.iv_back).setOnClickListener(v->{if(!guardando)finish();});
        findViewById(R.id.btn_cancelar).setOnClickListener(v->finish());
        findViewById(R.id.btn_elegir_foto).setOnClickListener(v->{if(cargado&&!guardando)elegirFoto.launch("image/*");});
        findViewById(R.id.btn_guardar_cambios).setOnClickListener(v->guardar());
        findViewById(R.id.btn_descartar_foto).setOnClickListener(v->{fotoPreparada=null;ClientProfilePhotos.mostrar(findViewById(R.id.iv_perfil_foto),PerfilRepository.cargar(this).photoUrl);actualizarBotones();estado("");});
        BottomNavigationView nav=findViewById(R.id.bottom_nav);nav.setSelectedItemId(R.id.nav_perfil);
        nav.setOnItemSelectedListener(item->{if(guardando)return false;
            startActivity(new Intent(this,MainActivity.class).putExtra(MainActivity.EXTRA_OPEN_TAB,item.getItemId())
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();return true;});
        getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){
            @Override public void handleOnBackPressed(){if(guardando)Toast.makeText(EditarPerfilActivity.this,"Espera a que termine el guardado.",Toast.LENGTH_SHORT).show();else finish();}
        });
        cargar();
    }
    private void cargar(){
        estado("Cargando perfil…");actualizarBotones();
        PerfilRepository.cargarServidor(this).addOnSuccessListener(profile->{
            if(isFinishing()||isDestroyed())return;
            cargado=true;pintar(profile,!restaurado);restaurado=false;actualizarBotones();estado("");
            findViewById(R.id.tv_estado_perfil).setOnClickListener(null);
        }).addOnFailureListener(error->{
            if(isFinishing()||isDestroyed())return;
            estado(ConexionCliente.disponible(this)?"No se pudo cargar el perfil. Toca aquí para reintentar.":"Sin conexión. Conéctate y toca aquí para cargar tu perfil.");
            findViewById(R.id.tv_estado_perfil).setOnClickListener(v->cargar());
        });
    }
    private void pintar(UserProfile p,boolean editables){
        if(editables){nombres.setText(p.firstNames);apellidos.setText(p.lastNames);telefono.setText(p.phone);domicilio.setText(p.domicilio);}
        ((TextView)findViewById(R.id.tv_avatar_inicial)).setText(p.initials());
        ((TextView)findViewById(R.id.actv_tipo_doc)).setText(valor(p.tipoDocumento));
        ((TextView)findViewById(R.id.et_num_doc)).setText(valor(p.numeroDocumento));
        ((TextView)findViewById(R.id.et_fecha_nacimiento)).setText(valor(p.fechaNacimiento));
        ((TextView)findViewById(R.id.et_email)).setText(valor(p.email));
        if(fotoPreparada!=null)HotelImageLoader.show(findViewById(R.id.iv_perfil_foto),android.net.Uri.fromFile(new java.io.File(fotoPreparada)).toString());
        else ClientProfilePhotos.mostrar(findViewById(R.id.iv_perfil_foto),p.photoUrl);
    }
    private String valor(String value){return value==null||value.isEmpty()?"No registrado":value;}
    private void actualizarBotones(){
        for(EditText field:new EditText[]{nombres,apellidos,telefono,domicilio})if(field!=null)field.setEnabled(cargado&&!guardando);
        findViewById(R.id.btn_guardar_cambios).setEnabled(cargado&&!guardando&&!preparando);
        findViewById(R.id.btn_cancelar).setEnabled(!guardando);
        findViewById(R.id.btn_descartar_foto).setVisibility(fotoPreparada==null?View.GONE:View.VISIBLE);
        findViewById(R.id.btn_descartar_foto).setEnabled(!guardando&&!preparando);
        findViewById(R.id.btn_elegir_foto).setEnabled(cargado&&!guardando&&!preparando);
        ((TextView)findViewById(R.id.btn_guardar_cambios)).setText(guardando?"Guardando…":"Guardar");
    }
    private void estado(String text){TextView label=findViewById(R.id.tv_estado_perfil);label.setText(text);label.setVisibility(text.isEmpty()?View.GONE:View.VISIBLE);}
    private void guardar(){
        if(!cargado||guardando||preparando)return;
        if(!ConexionCliente.disponible(this)){estado(getString(R.string.cliente_sin_conexion));return;}
        // Cada error en rojo debajo de su campo (mismas reglas que el registro).
        java.util.Map<String,String> errores=ClientProfileCodec.erroresEditables(nombres.getText().toString(),
                apellidos.getText().toString(),telefono.getText().toString(),domicilio.getText().toString());
        boolean ok=com.example.proyecto_iotelito.ui.common.ErrorCampo.aplicar(nombres,errores.get("nombres"));
        ok&=com.example.proyecto_iotelito.ui.common.ErrorCampo.aplicar(apellidos,errores.get("apellidos"));
        ok&=com.example.proyecto_iotelito.ui.common.ErrorCampo.aplicar(telefono,errores.get("telefono"));
        ok&=com.example.proyecto_iotelito.ui.common.ErrorCampo.aplicar(domicilio,errores.get("domicilio"));
        if(!ok){estado("Revisa los campos marcados en rojo.");return;}
        guardando=true;actualizarBotones();estado("Guardando tu perfil…");
        PerfilRepository.guardarRemoto(this,uid,nombres.getText().toString(),apellidos.getText().toString(),telefono.getText().toString(),domicilio.getText().toString(),fotoPreparada)
            .addOnSuccessListener(profile->{if(isFinishing()||isDestroyed())return;guardando=false;Toast.makeText(this,R.string.toast_perfil_actualizado,Toast.LENGTH_SHORT).show();finish();})
            .addOnFailureListener(error->{if(isFinishing()||isDestroyed())return;guardando=false;actualizarBotones();estado(
                    ConexionCliente.disponible(this)?ClientProfilePhotos.mensajeError(error):getString(R.string.cliente_sin_conexion));});
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("foto_preparada",fotoPreparada);state.putString("perfil_uid",uid);}
}
