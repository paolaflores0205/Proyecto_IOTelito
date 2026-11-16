package com.example.proyecto_iotelito.data;

import android.content.Context;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;
import android.widget.ImageView;
import com.google.android.gms.tasks.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONObject;
import java.io.*;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Prepara JPEG de hasta 1024 px y transfiere fotos privadas mediante la API AWS. */
public final class ClientProfilePhotos {
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    private ClientProfilePhotos() { }
    public static String preparar(Context context, Uri uri) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();options.inJustDecodeBounds=true;
        try(InputStream in=context.getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,options);}
        if(options.outWidth<=0||options.outHeight<=0)throw new IOException("Selecciona una imagen válida.");
        options.inJustDecodeBounds=false;options.inSampleSize=1;
        while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1024)options.inSampleSize*=2;
        Bitmap bitmap;
        try(InputStream in=context.getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in,null,options);}
        if(bitmap==null)throw new IOException("No se pudo abrir la imagen.");
        try(InputStream in=context.getContentResolver().openInputStream(uri)){
            int orientation=new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix=new Matrix();
            if(orientation==ExifInterface.ORIENTATION_ROTATE_90)matrix.postRotate(90);
            if(orientation==ExifInterface.ORIENTATION_ROTATE_180)matrix.postRotate(180);
            if(orientation==ExifInterface.ORIENTATION_ROTATE_270)matrix.postRotate(270);
            if(orientation==ExifInterface.ORIENTATION_FLIP_HORIZONTAL)matrix.postScale(-1,1);
            if(orientation==ExifInterface.ORIENTATION_FLIP_VERTICAL)matrix.postScale(1,-1);
            if(orientation==ExifInterface.ORIENTATION_TRANSPOSE){matrix.postRotate(90);matrix.postScale(-1,1);}
            if(orientation==ExifInterface.ORIENTATION_TRANSVERSE){matrix.postRotate(270);matrix.postScale(-1,1);}
            if(!matrix.isIdentity()){Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);if(rotated!=bitmap)bitmap.recycle();bitmap=rotated;}
        }catch(IOException ignored){ }
        File folder=new File(context.getCacheDir(),"profile-drafts");if(!folder.exists()&&!folder.mkdirs())throw new IOException("No se pudo preparar la foto.");
        File file=new File(folder,UUID.randomUUID()+".jpg");
        try(FileOutputStream out=new FileOutputStream(file)){if(!bitmap.compress(Bitmap.CompressFormat.JPEG,85,out))throw new IOException("No se pudo convertir la foto.");}
        finally{bitmap.recycle();}
        if(file.length()>=2*1024*1024){file.delete();throw new IOException("La imagen es demasiado grande.");}
        return file.getAbsolutePath();
    }
    public static Task<String> subir(String uid,String path){
        if(path==null)return Tasks.forResult(null);
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        if(user==null||!user.getUid().equals(uid))return Tasks.forException(new IllegalStateException("La sesión cambió. Vuelve a ingresar."));
        return user.getIdToken(false).onSuccessTask(result->Tasks.call(IO,()->{
            comprobarSesion(uid);
            try {
                File file=new File(path);
                if(!file.isFile()||file.length()<1||file.length()>ProfilePhotoPolicy.MAX_BYTES)
                    throw new AwsProfilePhotoApi.PhotoException("Selecciona nuevamente una foto de menos de 2 MiB.");
                JSONObject signed=AwsProfilePhotoApi.solicitar("/profile-photos/upload-url",result.getToken(),
                        new JSONObject().put("contentType","image/jpeg").put("sizeBytes",file.length()));
                String reference=signed.getString("reference");
                if(!ProfilePhotoPolicy.propia(reference,uid))throw new AwsProfilePhotoApi.PhotoException("No se pudo preparar tu foto.");
                comprobarSesion(uid); AwsProfilePhotoApi.subir(file,signed); comprobarSesion(uid);
                return reference;
            } catch(UnknownHostException|SocketTimeoutException error) {
                throw new AwsProfilePhotoApi.PhotoException("Se perdió la conexión al subir la foto. Tus cambios siguen en el formulario; reintenta.");
            }
        }));
    }
    public static void mostrar(ImageView view,String remote){
        view.setImageDrawable(null); HotelImageLoader.show(view,null);
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        if(user==null||!ProfilePhotoPolicy.propia(remote,user.getUid()))return;
        String uid=user.getUid(); Object request=new Object();
        view.setTag(com.example.proyecto_iotelito.R.id.hotel_image_request,request);
        user.getIdToken(false).onSuccessTask(result->Tasks.call(IO,()->{
            comprobarSesion(uid);
            JSONObject signed=AwsProfilePhotoApi.solicitar("/profile-photos/read-url",result.getToken(),
                    new JSONObject().put("reference",remote));
            comprobarSesion(uid);
            byte[] bytes=AwsProfilePhotoApi.descargar(signed.getString("url"));
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
            if(options.outWidth<=0||options.outHeight<=0)throw new IOException("Foto inválida.");
            options.inJustDecodeBounds=false;options.inSampleSize=1;
            while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1024)options.inSampleSize*=2;
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        })).addOnSuccessListener(bitmap->{
            FirebaseUser current=FirebaseAuth.getInstance().getCurrentUser();
            if(bitmap==null)return;
            if(current!=null&&uid.equals(current.getUid())&&view.getTag(com.example.proyecto_iotelito.R.id.hotel_image_request)==request)
                view.setImageBitmap(bitmap);
            else bitmap.recycle();
        });
    }
    private static void comprobarSesion(String uid){
        FirebaseUser current=FirebaseAuth.getInstance().getCurrentUser();
        if(current==null||!uid.equals(current.getUid()))throw new IllegalStateException("La sesión cambió. Vuelve a ingresar.");
    }
    public static String mensajeError(Exception error){
        Throwable cause=error;
        while(cause!=null){
            if(cause instanceof AwsProfilePhotoApi.PhotoException)return cause.getMessage();
            cause=cause.getCause();
        }
        return "No se guardaron los cambios. Vuelve a intentarlo; tus datos siguen en el formulario.";
    }
}
