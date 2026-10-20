package com.example.proyecto_iotelito.ui.taxista;
import android.app.*;
import android.content.*;
import android.os.*;
import android.graphics.Bitmap;
import android.widget.*;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.*;
import com.example.proyecto_iotelito.data.taxista.*;
import com.example.proyecto_iotelito.data.taxista.local.*;
import com.example.proyecto_iotelito.model.auth.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TaxistaUiTest {
    private final Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    private final Context context=inst.getTargetContext();
    private final List<Activity> screens=new ArrayList<>();
    private String cuenta;
    @Before public void setup() throws Exception {
        cuenta=UUID.randomUUID()+"@taxista-ui.test";
        SessionRepository.guardar(context,new AuthenticatedUser(cuenta,"Conductor UI",UserRole.TAXISTA));
        ConductorPreferencias.get(context,cuenta).edit().putBoolean("permission_asked",true).commit();
        try(ParcelFileDescriptor fd=inst.getUiAutomation().executeShellCommand("pm grant "+context.getPackageName()+" android.permission.POST_NOTIFICATIONS");
                InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
    }
    @After public void cleanup() throws Exception {
        inst.runOnMainSync(()->{for(Activity a:screens)if(!a.isFinishing())a.finish();});inst.waitForIdleSync();
        // Se elimina exclusivamente la cuenta ficticia creada por esta prueba.
        ConductorDatabase db=ConductorDatabase.get(context);
        for(String table:new String[]{"conductor_servicios","conductor_avisos","conductor_mensajes","conductor_inicio"})
            db.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM "+table+" WHERE cuenta=?",new Object[]{cuenta});
        for(int id=1;id<=6;id++){
            context.getSystemService(NotificationManager.class).cancel("taxista:"+cuenta+":solicitud:"+id,0);
            context.getSystemService(NotificationManager.class).cancel("taxista:"+cuenta+":finalizado:"+id,0);
        }
        java.io.File foto=ConductorFoto.archivo(context,cuenta);if(foto.exists())foto.delete();
        context.deleteSharedPreferences("taxista_"+cuenta);SessionRepository.cerrar(context);
    }
    private TaxistaMainActivity main(){
        TaxistaMainActivity a=(TaxistaMainActivity)inst.startActivitySync(new Intent(context,TaxistaMainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));screens.add(a);return a;
    }
    private void waitFor(BooleanSupplier condition) {
        long end=SystemClock.uptimeMillis()+15000;AtomicBoolean ok=new AtomicBoolean();
        do{inst.waitForIdleSync();inst.runOnMainSync(()->ok.set(condition.getAsBoolean()));if(ok.get())return;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<end);
        fail("La pantalla no mostró el resultado esperado");
    }
    private Activity next(Activity from,int button,Class<?> type){
        Instrumentation.ActivityMonitor m=inst.addMonitor(type.getName(),null,false);
        inst.runOnMainSync(()->from.findViewById(button).performClick());
        Activity a=m.waitForActivityWithTimeout(15000);inst.removeMonitor(m);assertNotNull(type.getSimpleName(),a);screens.add(a);inst.waitForIdleSync();return a;
    }
    private String text(Activity a,int id){return ((TextView)a.findViewById(id)).getText().toString();}
    private void tab(TaxistaMainActivity a,int id){inst.runOnMainSync(()->((BottomNavigationView)a.findViewById(R.id.bottom_nav)).setSelectedItemId(id));inst.waitForIdleSync();}

    @Test public void filtrosAvisoRecorridoYRecuperacion() throws Exception {
        TaxistaMainActivity a=main();tab(a,R.id.nav_taxi_solicitudes);
        RecyclerView list=a.findViewById(R.id.rv_solicitudes);
        waitFor(()->list.getAdapter()!=null&&list.getAdapter().getItemCount()>=6);
        int total=list.getAdapter().getItemCount();
        inst.runOnMainSync(()->((ChipGroup)a.findViewById(R.id.chip_group_filtros)).check(R.id.chip_menos_10_min));
        waitFor(()->list.getAdapter().getItemCount()>=2&&list.getAdapter().getItemCount()<total);
        inst.runOnMainSync(()->((ChipGroup)a.findViewById(R.id.chip_group_filtros)).check(R.id.chip_mayor_tarifa));
        waitFor(()->list.findViewHolderForAdapterPosition(0)!=null&&((TextView)list.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.tv_hotel_nombre)).getText().toString().contains("Palacio"));
        tab(a,R.id.nav_taxi_historial);
        RecyclerView history=a.findViewById(R.id.rv_historial);waitFor(()->history.getAdapter().getItemCount()==7);
        inst.runOnMainSync(()->((TabLayout)a.findViewById(R.id.tab_layout_historial)).getTabAt(2).select());
        waitFor(()->history.getAdapter().getItemCount()==2);
        tab(a,R.id.nav_taxi_perfil);
        inst.runOnMainSync(()->((android.widget.CompoundButton)a.findViewById(R.id.switch_disponibilidad_perfil)).setChecked(false));
        tab(a,R.id.nav_taxi_inicio);
        waitFor(()->!((android.widget.CompoundButton)a.findViewById(R.id.switch_disponibilidad)).isChecked());
        inst.runOnMainSync(()->((android.widget.CompoundButton)a.findViewById(R.id.switch_disponibilidad)).setChecked(true));
        // Tocar el PendingIntent real de la notificación de Carlos.
        NotificationManager nm=context.getSystemService(NotificationManager.class);
        android.service.notification.StatusBarNotification selected=null;
        for(int retry=0;retry<100&&selected==null;retry++){
            for(android.service.notification.StatusBarNotification n:nm.getActiveNotifications())
                if(("taxista:"+cuenta+":solicitud:2").equals(n.getTag()))selected=n;
            if(selected==null)SystemClock.sleep(100);
        }
        assertNotNull("Debe publicarse el aviso del taxista",selected);
        Instrumentation.ActivityMonitor monitor=inst.addMonitor(DetalleServicioActivity.class.getName(),null,false);
        selected.getNotification().contentIntent.send();
        Activity detalle=monitor.waitForActivityWithTimeout(15000);inst.removeMonitor(monitor);assertNotNull(detalle);screens.add(detalle);
        waitFor(()->text(detalle,R.id.tv_pasajero_nombre).equals("Carlos Mendoza"));
        Activity asignado=next(detalle,R.id.btn_aceptar_servicio,ServicioAsignadoActivity.class);
        waitFor(()->text(asignado,R.id.tv_pasajero_nombre).equals("Carlos Mendoza"));
        Activity hotel=next(asignado,R.id.btn_iniciar_recorrido,DireccionHotelActivity.class);
        waitFor(()->text(hotel,R.id.tv_direccion).contains("Palacio"));
        Activity aeropuerto=next(hotel,R.id.btn_llegue_al_hotel,DireccionAeropuertoActivity.class);
        waitFor(()->text(aeropuerto,R.id.tv_pasajero_nombre).equals("Carlos Mendoza"));
        waitFor(()->aeropuerto.findViewById(R.id.bottom_nav)!=null&&aeropuerto.findViewById(R.id.bottom_nav).isShown());
        for(int id=1;id<=6;id++)nm.cancel("taxista:"+cuenta+":solicitud:"+id,0);
        inst.runOnMainSync(()->{
            View nav=aeropuerto.findViewById(R.id.bottom_nav);android.graphics.Rect bounds=new android.graphics.Rect();
            assertTrue(nav.getGlobalVisibleRect(bounds));
            android.util.Log.i("TaxistaUiBounds","nav="+bounds+" content="+aeropuerto.findViewById(android.R.id.content).getHeight());
            assertTrue("Barra dentro de la pantalla",bounds.bottom<=aeropuerto.getResources().getDisplayMetrics().heightPixels);
        });
        capture("taxista-servicio-preview.png");
        Instrumentation.ActivityMonitor homeMonitor=inst.addMonitor(TaxistaMainActivity.class.getName(),null,false);
        inst.runOnMainSync(()->((BottomNavigationView)aeropuerto.findViewById(R.id.bottom_nav)).setSelectedItemId(R.id.nav_taxi_inicio));
        inst.waitForIdleSync();inst.removeMonitor(homeMonitor);
        TaxistaMainActivity reopened=a;
        waitFor(()->reopened.findViewById(R.id.taxista_card_activo).isShown());
        assertEquals(View.GONE,reopened.findViewById(R.id.banner_nueva_solicitud).getVisibility());
        capture("taxista-inicio-activo-preview.png");
        waitFor(()->reopened.findViewById(R.id.taxista_continuar).isShown());
        Activity resumed=next(reopened,R.id.taxista_continuar,DireccionAeropuertoActivity.class);
        waitFor(()->text(resumed,R.id.tv_pasajero_nombre).equals("Carlos Mendoza"));
        Activity validar=next(resumed,R.id.btn_llegue_al_destino,FinalizarServicioActivity.class);
        waitFor(()->validar.hasWindowFocus()&&validar.findViewById(R.id.btn_validar_codigo).hasOnClickListeners());
        inst.runOnMainSync(()->((EditText)validar.findViewById(R.id.et_codigo_manual)).setText("TAXI-2"));
        inst.runOnMainSync(()->validar.findViewById(R.id.btn_validar_codigo).performClick());
        inst.waitForIdleSync();
        // Espresso encuentra el botón del diálogo de confirmación.
        Instrumentation.ActivityMonitor finMonitor=inst.addMonitor(ServicioFinalizadoActivity.class.getName(),null,false);
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.taxista_local_confirmar))
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(androidx.test.espresso.action.ViewActions.click());
        Activity fin=finMonitor.waitForActivityWithTimeout(15000);inst.removeMonitor(finMonitor);assertNotNull(fin);screens.add(fin);
        waitFor(()->text(fin,R.id.tv_pasajero_nombre).equals("Carlos Mendoza"));
        assertEquals("FINALIZADO",ConductorDatabase.get(context).dao().buscar(cuenta,2).estado);
    }

    private void capture(String file) throws Exception {
        SystemClock.sleep(900);
        Bitmap image=inst.getUiAutomation().takeScreenshot();
        try(FileOutputStream out=context.openFileOutput(file,Context.MODE_PRIVATE)){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();
    }
    @Test public void fotoImportadaYBandejaConservanElDiseno() throws Exception {
        Bitmap bitmap=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xff007f80);
        File file=new File(context.getCacheDir(),"taxista-ui-test.png");
        try(FileOutputStream out=new FileOutputStream(file)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
        ConductorFoto.guardar(context,cuenta,android.net.Uri.fromFile(file));file.delete();
        TaxistaMainActivity a=main();tab(a,R.id.nav_taxi_perfil);
        waitFor(()->a.findViewById(android.R.id.content).findViewWithTag("taxista_foto")!=null);
        tab(a,R.id.nav_taxi_inicio);
        waitFor(()->a.findViewById(R.id.taxista_abrir_avisos).isShown());
        inst.runOnMainSync(()->a.findViewById(R.id.taxista_abrir_avisos).performClick());
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.taxista_configurar_avisos))
                .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
        SystemClock.sleep(900); // Espera el fin de la animacion del BottomSheet para la captura.
        Bitmap screenshot=inst.getUiAutomation().takeScreenshot();
        try(FileOutputStream out=context.openFileOutput("taxista-avisos-preview.png",Context.MODE_PRIVATE)){screenshot.compress(Bitmap.CompressFormat.PNG,100,out);}screenshot.recycle();
    }
}
