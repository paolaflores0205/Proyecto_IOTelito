package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.UserProfile;
import org.junit.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.Assert.*;

public class ClientProfileCodecTest {
    @Test public void registroYLecturaConservanIdentidadParaLosTresDocumentos() {
        String[][] documentos={{"DNI","12345678"},{"Pasaporte","ab123456"},{"Carnet de Extranjería","001234567"}};
        for(String[] doc:documentos){
            Map<String,Object> fields=ClientProfileCodec.registro(" Ana "," Torres ",doc[0],doc[1],
                    "15/03/1990","999888777","Av. Principal 123",LocalDate.of(2026,11,16));
            assertEquals(doc[0],fields.get("documentType"));
            assertEquals(doc[1].toUpperCase(Locale.ROOT),fields.get("documentNumber"));
            assertEquals("1990-03-15",fields.get("birthDate"));
            UserProfile decoded=ClientProfileCodec.decode(fields);
            assertEquals(doc[0],decoded.tipoDocumento);
            assertEquals(doc[1].toUpperCase(Locale.ROOT),decoded.numeroDocumento);
            assertEquals("15/03/1990",decoded.fechaNacimiento);
        }
    }
    @Test public void registroNoPermiteIdentidadVacia() {
        LocalDate hoy=LocalDate.of(2026,11,16);
        assertThrows(IllegalArgumentException.class,()->ClientProfileCodec.registro("Ana","Torres","DNI","", "15/03/1990","999888777","Av. Principal 123",hoy));
        assertThrows(IllegalArgumentException.class,()->ClientProfileCodec.registro("Ana","Torres","DNI","12345678", "","999888777","Av. Principal 123",hoy));
    }
    @Test public void datosAntiguosNoInventanIdentidadNiApellidos() {
        Map<String,Object> data=new HashMap<>();data.put("name","Ana Maria Torres");data.put("email","ana@example.test");
        UserProfile p=ClientProfileCodec.decode(data);
        assertEquals("Ana Maria Torres",p.firstNames);assertEquals("",p.lastNames);
        assertEquals("",p.numeroDocumento);assertEquals("",p.fechaNacimiento);assertEquals("",p.photoUrl);
    }
    @Test public void convierteNacimientoYConservaCamposRemotos() {
        Map<String,Object> data=new HashMap<>();data.put("name","Ana Torres");data.put("firstNames","Ana");data.put("lastNames","Torres");
        data.put("birthDate","1990-03-15");data.put("documentType","Pasaporte");data.put("documentNumber","AB123456");data.put("photoUrl","gs://bucket/profilePhotos/uid/foto.jpg");
        UserProfile p=ClientProfileCodec.decode(data);assertEquals("15/03/1990",p.fechaNacimiento);
        assertEquals("Torres",p.lastNames);assertEquals("AB123456",p.numeroDocumento);assertTrue(p.photoUrl.startsWith("gs://"));
    }
    @Test public void actualizacionSoloContieneCamposEditables() {
        Map<String,Object> data=ClientProfileCodec.editables(" Ana "," Torres ","999 888 777"," Av. Principal 123 ");
        assertEquals(new HashSet<>(Arrays.asList("name","firstNames","lastNames","phone","address")),data.keySet());
        assertEquals("Ana Torres",data.get("name"));assertEquals("999888777",data.get("phone"));
    }
    @Test public void validaLosTresDocumentosSinAplicarReglasDeDniAlPasaporte() {
        ClientProfileCodec.validarDocumento("DNI","12345678");ClientProfileCodec.validarDocumento("Pasaporte","AB123456");
        ClientProfileCodec.validarDocumento("Carnet de Extranjería","001234567");
        assertThrows(IllegalArgumentException.class,()->ClientProfileCodec.validarDocumento("DNI","AB123456"));
        assertThrows(IllegalArgumentException.class,()->ClientProfileCodec.validarDocumento("Pasaporte","123"));
        assertThrows(IllegalArgumentException.class,()->ClientProfileCodec.validarDocumento("Otro","12345678"));
    }
    @Test public void nacimientoValidaFechaRealEdadYFormato() {
        LocalDate hoy=LocalDate.of(2026,11,16);
        assertEquals("2008-11-16",ClientProfileCodec.nacimientoIso("16/11/2008",hoy));
        for(String date:Arrays.asList("17/11/2008","31/02/1990","16/11/2030","2020-01-01","15/03/1800"))
            assertThrows(IllegalArgumentException.class,()->ClientProfileCodec.nacimientoIso(date,hoy));
    }
    @Test public void editarRequiereNombresApellidosTelefonoYDomicilioValidos() {
        assertThrows(IllegalArgumentException.class,()->ClientProfileCodec.editables("Ana","","999888777","Av. Principal 123"));
        assertThrows(IllegalArgumentException.class,()->ClientProfileCodec.editables("Ana","Torres","abc","Av. Principal 123"));
        assertThrows(IllegalArgumentException.class,()->ClientProfileCodec.editables("Ana","Torres","999888777","Calle"));
    }
    @Test public void erroresPorCampoYNombresSoloConLetras() {
        assertTrue(ClientProfileCodec.erroresEditables("José María","Ñahui O'Neil","012345678","Av. Lima 123").isEmpty());
        java.util.Map<String,String> e=ClientProfileCodec.erroresEditables("Juan2","X","98765432","Calle");
        assertEquals(java.util.Arrays.asList("nombres","apellidos","telefono","domicilio"),new java.util.ArrayList<>(e.keySet()));
    }
    @Test public void contrasenaDeOchoConLetrasYNumeros() {
        assertNull(ClientProfileCodec.errorContrasena("hotel2026"));
        for(String mala:new String[]{"abc123","solamenteletras","12345678",null})
            assertNotNull(ClientProfileCodec.errorContrasena(mala));
    }
}
