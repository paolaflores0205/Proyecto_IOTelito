package com.example.proyecto_iotelito.data;

import android.app.*;
import android.content.*;
import android.os.SystemClock;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.hoteladmin.*;
import com.example.proyecto_iotelito.model.auth.*;
import com.example.proyecto_iotelito.ui.hoteladmin.HotelAdminMainActivity;
import com.example.proyecto_iotelito.ui.hoteladmin.mensajes.AdminChatActivity;
import com.example.proyecto_iotelito.ui.hoteladmin.reservas.CobroCheckoutActivity;
import com.example.proyecto_iotelito.ui.reservas.ChatHotelActivity;
import com.google.android.gms.tasks.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Optativa y exclusivamente local. Ejecutar en un AVD desechable con hotelEmulator=true.
 * Reemplaza FirebaseApp por demo-iotelito-auth y no contiene credenciales reales.
 */
@RunWith(AndroidJUnit4.class)
public class HotelOperationsEmulatorTest {
    private final Instrumentation ins=InstrumentationRegistry.getInstrumentation();
    private final Context context=ins.getTargetContext();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseOptions original;
    private Activity activity;
    private String clientUid,adminUid;
    private final String password="emulator-test-only";
    private JSONObject fields(Object... values)throws Exception{
        JSONObject result=new JSONObject();
        for(int i=0;i<values.length;i+=2){Object v=values[i+1];JSONObject field=new JSONObject();
            if(v==null)field.put("nullValue",JSONObject.NULL);
            else if(v instanceof Boolean)field.put("booleanValue",v);
            else if(v instanceof Number)field.put(v instanceof Double?"doubleValue":"integerValue",v instanceof Double?v:String.valueOf(v));
            else if(v instanceof int[]){JSONArray array=new JSONArray();for(int number:(int[])v)array.put(new JSONObject().put("integerValue",String.valueOf(number)));field.put("arrayValue",new JSONObject().put("values",array));}
            else field.put("stringValue",v);
            result.put((String)values[i],field);
        }return result;
    }
    private void seed(String path,JSONObject data)throws Exception{
        // Esta dirección es literal y solo existe dentro del AVD local.
        HttpURLConnection connection=(HttpURLConnection)new URL("http://127.0.0.1:8180/v1/projects/demo-iotelito-auth/databases/(default)/documents:commit").openConnection();
        connection.setRequestMethod("POST");connection.setRequestProperty("Authorization","Bearer owner");connection.setRequestProperty("Content-Type","application/json");
        connection.setConnectTimeout(10000);connection.setReadTimeout(10000);connection.setDoOutput(true);
        JSONObject update=new JSONObject().put("name","projects/demo-iotelito-auth/databases/(default)/documents/"+path).put("fields",data);
        byte[] body=new JSONObject().put("writes",new JSONArray().put(new JSONObject().put("update",update))).toString().getBytes(StandardCharsets.UTF_8);
        try(java.io.OutputStream out=connection.getOutputStream()){out.write(body);}
        assertEquals("Seed local "+path,200,connection.getResponseCode());connection.disconnect();
    }
    private void waitFor(BooleanSupplier ready){
        long deadline=SystemClock.uptimeMillis()+25000;AtomicBoolean ok=new AtomicBoolean();
        while(!ok.get()&&SystemClock.uptimeMillis()<deadline){ins.runOnMainSync(()->ok.set(ready.getAsBoolean()));if(!ok.get())SystemClock.sleep(100);}
        assertTrue("La operación/pantalla no terminó",ok.get());
    }
    private void login(String email,UserRole role,String uid,Integer hotel)throws Exception{
        closeScreen();ins.runOnMainSync(()->SessionRepository.cerrar(context));
        Tasks.await(auth.signInWithEmailAndPassword(email,password),20,TimeUnit.SECONDS);
        ins.runOnMainSync(()->SessionRepository.guardar(context,new AuthenticatedUser(email,"Cuenta local",role,uid,hotel)));
    }
    private <T extends Activity>T open(Class<T> type,Intent intent){
        closeScreen();activity=ins.startActivitySync((intent==null?new Intent(context,type):intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return type.cast(activity);
    }
    private void closeScreen(){if(activity!=null){Activity current=activity;ins.runOnMainSync(current::finish);ins.waitForIdleSync();activity=null;}}

    private boolean hasNotification(String tag, int id) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        for (android.service.notification.StatusBarNotification notification : manager.getActiveNotifications()) {
            if (tag.equals(notification.getTag()) && notification.getId() == id) return true;
        }
        return false;
    }

    private void verifyPhotoUploads(FirebaseStorage storage) throws Exception {
        List<String> draft = new ArrayList<>();
        List<File> temporary = new ArrayList<>();
        android.graphics.Bitmap image = android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888);
        image.eraseColor(android.graphics.Color.BLUE);
        try {
            for (int i = 0; i < 4; i++) {
                File file = File.createTempFile("qa-photo-", ".jpg", context.getCacheDir());
                temporary.add(file);
                try (FileOutputStream output = new FileOutputStream(file)) {
                    assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, output));
                }
                draft.add(android.net.Uri.fromFile(file).toString());
            }
            FirebaseHotelAdminRepository repository = new FirebaseHotelAdminRepository(context);
            AtomicReference<String> result = new AtomicReference<>("PENDIENTE");
            ins.runOnMainSync(() -> repository.savePhotos(draft, java.util.Collections.emptyList(), result::set));
            waitFor(() -> !"PENDIENTE".equals(result.get()));
            assertNull(result.get(), result.get());
            @SuppressWarnings("unchecked")
            List<String> gallery = (List<String>) Tasks.await(db.document("hotels/1").get(Source.SERVER),
                    20, TimeUnit.SECONDS).get("photos");
            assertEquals(4, gallery.size());
            byte[] jpeg = Tasks.await(storage.getReferenceFromUrl(gallery.get(0)).getBytes(100_000),
                    20, TimeUnit.SECONDS);
            assertTrue(jpeg.length > 0);
            try {
                Tasks.await(storage.getReference().child("hotelAdminPhotos/2/" + adminUid + "/alien.jpg")
                        .putBytes(jpeg, new StorageMetadata.Builder().setContentType("image/jpeg").build()),
                        20, TimeUnit.SECONDS);
                fail("No debe subir archivos de otro hotel");
            } catch (ExecutionException denied) {
                assertTrue(denied.getCause() instanceof com.google.firebase.storage.StorageException);
                assertEquals(com.google.firebase.storage.StorageException.ERROR_NOT_AUTHORIZED,
                        ((com.google.firebase.storage.StorageException) denied.getCause()).getErrorCode());
            }
            result.set("PENDIENTE");
            com.example.proyecto_iotelito.model.hoteladmin.Servicio service =
                    new com.example.proyecto_iotelito.model.hoteladmin.Servicio(801, 1, "Desayuno QA", "", 25,
                            "servicio", R.drawable.superadmin_hotel_sol, true);
            ins.runOnMainSync(() -> repository.saveServiceWithPhoto(service, true, draft.get(0), result::set));
            waitFor(() -> !"PENDIENTE".equals(result.get()));
            assertNull(result.get(), result.get());
            DocumentSnapshot savedService = Tasks.await(db.document("hotels/1/services/801").get(Source.SERVER),
                    20, TimeUnit.SECONDS);
            assertNotNull(savedService.getString("fotoUrl"));
        } finally {
            image.recycle();
            for (File file : temporary) file.delete();
        }
    }
    @After public void cleanup()throws Exception{
        if(original==null)return;
        closeScreen();ins.runOnMainSync(()->SessionRepository.cerrar(context));
        Tasks.await(db.terminate(),10,TimeUnit.SECONDS);
        FirebaseApp.getInstance().delete();FirebaseApp.initializeApp(context,original);
    }
    @Test public void sharedChatCheckoutAndAdminTabsUseEmulatedFirebase()throws Exception{
        assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("hotelEmulator")));
        original=FirebaseApp.getInstance().getOptions();FirebaseApp.getInstance().delete();
        FirebaseApp.initializeApp(context,new FirebaseOptions.Builder().setApiKey("fake-api-key").setApplicationId("1:123456:android:abcdef")
                .setProjectId("demo-iotelito-auth").setStorageBucket("demo-iotelito-auth.appspot.com").build());
        auth=FirebaseAuth.getInstance();auth.useEmulator("127.0.0.1",9099);
        db=FirebaseFirestore.getInstance();db.useEmulator("127.0.0.1",8180);
        boolean useStorage = "true".equals(InstrumentationRegistry.getArguments().getString("hotelStorage"));
        boolean checkNotifications = "true".equals(InstrumentationRegistry.getArguments().getString("hotelNotifications"));
        FirebaseStorage storage = useStorage ? FirebaseStorage.getInstance() : null;
        if (storage != null) storage.useEmulator("127.0.0.1", 9199);
        db.setFirestoreSettings(new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build());
        clientUid=Tasks.await(auth.createUserWithEmailAndPassword("client-hotel@example.test",password),20,TimeUnit.SECONDS).getUser().getUid();
        auth.signOut();adminUid=Tasks.await(auth.createUserWithEmailAndPassword("admin-hotel@example.test",password),20,TimeUnit.SECONDS).getUser().getUid();auth.signOut();
        seed("users/"+clientUid,fields("uid",clientUid,"name","Cliente QA","email","client-hotel@example.test","role","CLIENT","active",true,"hotelId",null));
        seed("users/"+adminUid,fields("uid",adminUid,"name","Admin QA","email","admin-hotel@example.test","role","HOTEL_ADMIN","active",true,"hotelId",1));
        seed("hotels/1",fields("id",1,"name","Hotel QA","address","Direccion QA","city","Lima","district","Prueba","active",true,"administratorUid",adminUid,"rooms",1,"hasInventory",true,"pricePerNight",100.25));
        seed("hotels/2",fields("id",2,"name","Hotel ajeno","address","Otra","city","Lima","active",true,"administratorUid","other-admin"));
        seed("hotels/1/rooms/101",fields("id",101,"hotelId",1,"tipo","Doble","nombreCodigo","101","areaM2",20,"adultos",2,"ninos",0,"precioNoche",100.25,"descripcion","QA","disponible",true,"archived",false));
        LocalDate today=LocalDate.now(FirebaseHotelOperations.ZONE);
        seed("reservations/2001",fields("id",2001,"hotelId",1,"clientUid",clientUid,"clientName","Cliente QA","hotelName","Hotel QA","roomIds",new int[]{101},"roomName","Doble","roomNumber","101","fechaEntrada",today.minusDays(1).toString(),"fechaSalida",today.toString(),"precioTotal",100.25,"estado","CONFIRMADA","codigo","LOCAL-2001"));
        seed("reservations/3001",fields("id",3001,"hotelId",2,"clientUid","other-client","roomIds",new int[]{201},"roomName","Simple","roomNumber","201","fechaEntrada",today.toString(),"fechaSalida",today.plusDays(1).toString(),"precioTotal",999.0,"estado","CONFIRMADA","codigo","AJENO"));
        login("client-hotel@example.test",UserRole.CLIENT,clientUid,null);
        Tasks.await(new FirebaseHotelChat(context).send(2001,"Pregunta del huésped"),20,TimeUnit.SECONDS);
        com.example.proyecto_iotelito.data.ReservaRepository.getInstance(context).solicitarCheckout(2001,5,"Estadía de prueba satisfactoria",
                TarjetaSimulada.validar("4242424242424242","12/39","123",java.time.YearMonth.now()));
        login("admin-hotel@example.test",UserRole.HOTEL_ADMIN,adminUid,1);
        AdminChatActivity chat=open(AdminChatActivity.class,new Intent(context,AdminChatActivity.class).putExtra(AdminChatActivity.EXTRA_CONVERSACION_ID,2001));
        waitFor(()->chat.findViewById(R.id.btn_enviar).isEnabled()&&((LinearLayout)chat.findViewById(R.id.ll_mensajes)).getChildCount()==1);
        ins.runOnMainSync(()->{((EditText)chat.findViewById(R.id.et_mensaje)).setText("Respuesta del administrador");chat.findViewById(R.id.btn_enviar).performClick();});
        waitFor(()->((LinearLayout)chat.findViewById(R.id.ll_mensajes)).getChildCount()==2);
        HotelAdminMainActivity main=open(HotelAdminMainActivity.class,null);
        waitFor(() -> main.findViewById(R.id.card_alerta_checkout) != null
                && main.findViewById(R.id.card_alerta_checkout).getVisibility() == android.view.View.VISIBLE);
        if (checkNotifications) waitFor(() -> hasNotification("hotel-checkout:" + adminUid + ":1", 2001));
        if (storage != null) verifyPhotoUploads(storage);
        ins.runOnMainSync(()->main.openTab(R.id.nav_hoteladmin_reservas));
        waitFor(()->main.findViewById(R.id.rv_reservas)!=null);
        ins.runOnMainSync(()->main.findViewById(R.id.chip_checkout).performClick());
        waitFor(()->((androidx.recyclerview.widget.RecyclerView)main.findViewById(R.id.rv_reservas)).getAdapter().getItemCount()==1);
        ins.runOnMainSync(()->main.openTab(R.id.nav_hoteladmin_mensajes));
        waitFor(()->main.findViewById(R.id.rv_conversaciones)!=null&&((androidx.recyclerview.widget.RecyclerView)main.findViewById(R.id.rv_conversaciones)).getAdapter().getItemCount()==1);
        CobroCheckoutActivity checkout=open(CobroCheckoutActivity.class,new Intent(context,CobroCheckoutActivity.class).putExtra(CobroCheckoutActivity.EXTRA_RESERVA_ID,2001));
        waitFor(()->checkout.findViewById(R.id.btn_confirmar).isEnabled());
        Tasks.await(new FirebaseHotelOperations(context).checkout(2001,12.50,"Daño de prueba","Observación QA"),20,TimeUnit.SECONDS);
        // Reintentar no cambia el comprobante ni duplica importes.
        Tasks.await(new FirebaseHotelOperations(context).checkout(2001,0,"",""),20,TimeUnit.SECONDS);
        assertEquals("FINALIZADA",Tasks.await(db.document("reservations/2001").get(Source.SERVER),20,TimeUnit.SECONDS).getString("estado"));
        assertEquals(112.75,Tasks.await(db.document("hotelCheckouts/2001").get(Source.SERVER),20,TimeUnit.SECONDS).getDouble("total"),.001);
        AtomicReference<com.example.proyecto_iotelito.model.hoteladmin.ReservaAdmin> loaded = new AtomicReference<>();
        AtomicReference<String> loadError = new AtomicReference<>();
        ins.runOnMainSync(() -> new FirebaseHotelOperations(context).load(2001, loaded::set, loadError::set));
        waitFor(() -> loaded.get() != null || loadError.get() != null);
        assertNull(loadError.get()); assertTrue(loaded.get().tieneComprobante);
        assertEquals(112.75, loaded.get().total(), .001);
        assertEquals("Daño de prueba", loaded.get().consumos.get(0).motivo);
        final HotelAdminMainActivity reportMain=open(HotelAdminMainActivity.class,null);
        ins.runOnMainSync(()->reportMain.openTab(R.id.nav_hoteladmin_reportes));
        waitFor(()->reportMain.findViewById(R.id.tv_ingresos)!=null&&((TextView)reportMain.findViewById(R.id.tv_ingresos)).getText().toString().contains("112.75"));
        if (checkNotifications) waitFor(() -> !hasNotification("hotel-checkout:" + adminUid + ":1", 2001));
        try{Tasks.await(db.document("reservations/3001").get(Source.SERVER),20,TimeUnit.SECONDS);fail("No debe leer otro hotel");}
        catch(ExecutionException denied){assertTrue(denied.getCause() instanceof FirebaseFirestoreException);assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED,((FirebaseFirestoreException)denied.getCause()).getCode());}
        login("client-hotel@example.test",UserRole.CLIENT,clientUid,null);
        com.example.proyecto_iotelito.ui.perfil.NotificacionesActivity notices =
                open(com.example.proyecto_iotelito.ui.perfil.NotificacionesActivity.class, null);
        waitFor(() -> ((androidx.recyclerview.widget.RecyclerView) notices.findViewById(R.id.rv_notificaciones))
                .getAdapter().getItemCount() == 1);
        if (checkNotifications) waitFor(() -> hasNotification("client-checkout:" + clientUid, 2001));
        ChatHotelActivity client=open(ChatHotelActivity.class,new Intent(context,ChatHotelActivity.class).putExtra(ChatHotelActivity.EXTRA_HOTEL_ID,1).putExtra(ChatHotelActivity.EXTRA_RESERVA_ID,2001));
        waitFor(()->client.findViewById(R.id.mensajes_container)!=null&&((LinearLayout)client.findViewById(R.id.mensajes_container)).getChildCount()==2);
        System.out.println("HOTEL_EMULATOR: chat bidireccional persistido, tabs reales, filtro hotel, checkout idempotente y reporte correctos.");
    }
}
