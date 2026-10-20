package com.example.proyecto_iotelito.ui.taxista;
import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
/** Archivo interno, acotado a 512 px; no depende del permiso del URI después de importar. */
final class ConductorFoto {
    static File archivo(Context c,String cuenta) throws Exception {
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(cuenta.getBytes(StandardCharsets.UTF_8));
        StringBuilder name=new StringBuilder("taxista_foto_");
        for(byte b:hash)name.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        return new File(c.getFilesDir(),name+".jpg");
    }
    static boolean guardar(Context c,String cuenta,Uri uri) throws Exception {
        ImageDecoder.Source source=ImageDecoder.createSource(c.getContentResolver(),uri);
        Bitmap image=ImageDecoder.decodeBitmap(source,(decoder,info,src)->{
            int w=info.getSize().getWidth(),h=info.getSize().getHeight();
            float scale=Math.min(1f,512f/Math.max(w,h));
            decoder.setTargetSize(Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)));
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
        File finalFile=archivo(c,cuenta),temp=new File(finalFile.getPath()+".tmp");
        try {
            try(FileOutputStream out=new FileOutputStream(temp)){if(!image.compress(Bitmap.CompressFormat.JPEG,90,out))throw new IOException("Imagen no válida");}
            if(!temp.renameTo(finalFile))throw new IOException("No se pudo guardar la foto");
            return true;
        } finally {image.recycle();if(temp.exists())temp.delete();}
    }
}
