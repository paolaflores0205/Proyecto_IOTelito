package com.example.proyecto_iotelito.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProfilePhotoPolicyTest {
    private final String ref="s3://iot-telito-imagenes/profilePhotos/user1/12345678-1234-1234-1234-123456789abc.jpg";
    @Test public void soloAdmiteReferenciaEstablePropia(){
        assertTrue(ProfilePhotoPolicy.propia(ref,"user1"));
        assertFalse(ProfilePhotoPolicy.propia(ref,"other"));
        assertFalse(ProfilePhotoPolicy.propia(ref.replace("iot-telito-imagenes","foreign"),"user1"));
        assertFalse(ProfilePhotoPolicy.propia(ref.replace("12345678-1234-1234-1234-123456789abc","../user2/foto"),"user1"));
        assertFalse(ProfilePhotoPolicy.propia(ref+"?token=x","user1"));
        assertFalse(ProfilePhotoPolicy.propia(null,"user1"));
        assertFalse(ProfilePhotoPolicy.propia(ref,null));
        assertFalse(ProfilePhotoPolicy.propia("https://example.org/foto.jpg","user1"));
    }
    @Test public void destinosFirmadosSoloPermitenS3DelBucket(){
        assertTrue(ProfilePhotoPolicy.destinoS3("https://iot-telito-imagenes.s3.us-east-1.amazonaws.com/?X-Amz-Signature=abc"));
        assertTrue(ProfilePhotoPolicy.destinoS3("https://iot-telito-imagenes.s3.amazonaws.com/profilePhotos/x.jpg"));
        for(String bad:new String[]{"http://iot-telito-imagenes.s3.amazonaws.com/",
                "https://iot-telito-imagenes.s3.amazonaws.com.evil.org/",
                "https://evil.org/iot-telito-imagenes.s3.amazonaws.com/",
                "https://other.s3.amazonaws.com/","https://user@iot-telito-imagenes.s3.amazonaws.com/",
                "https://iot-telito-imagenes.s3.amazonaws.com:8443/","file:///tmp/foto",null})
            assertFalse(String.valueOf(bad),ProfilePhotoPolicy.destinoS3(bad));
    }
}
