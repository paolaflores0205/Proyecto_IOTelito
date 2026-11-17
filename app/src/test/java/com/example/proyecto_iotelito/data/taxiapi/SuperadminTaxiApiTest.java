package com.example.proyecto_iotelito.data.taxiapi;

import org.json.*;
import org.junit.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class SuperadminTaxiApiTest {
    @Test public void respetaRutasFiltrosYCamposDelContrato() {
        AtomicReference<String> request=new AtomicReference<>();AtomicReference<JSONObject> body=new AtomicReference<>();
        JsonTaxiApi api=new JsonTaxiApi(){protected JSONObject request(String method,String route,JSONObject value,boolean auth){
            assertTrue(auth);request.set(method+" "+route);body.set(value);return new JSONObject();
        }};
        api.listarTaxistas(null);assertEquals("GET /taxistas",request.get());
        api.listarTaxistas("APROBADO");assertEquals("GET /taxistas?estadoSolicitud=APROBADO",request.get());
        api.cambiarActivoTaxista(12345678901L,false,"  Solicitud del usuario  ");
        assertEquals("PATCH /taxistas/12345678901/activo",request.get());
        assertFalse(body.get().optBoolean("activo"));assertEquals("Solicitud del usuario",body.get().optString("motivo"));
        assertThrows(IllegalArgumentException.class,()->api.listarTaxistas("otro&rol=admin"));
        assertThrows(IllegalArgumentException.class,()->api.cambiarActivoTaxista(0,true,null));
        assertThrows(IllegalArgumentException.class,()->api.cambiarActivoTaxista(1,false,"x".repeat(501)));
    }
    @Test public void soloSuperadminAdministraConductoresEnLaApiDePrueba() {
        AtomicReference<FakeTaxiApi.Caller> caller=new AtomicReference<>(new FakeTaxiApi.Caller("driver","TAXISTA","Ana","ana@test.pe",null));
        FakeTaxiApi api=new FakeTaxiApi(new FakeTaxiApi.Store(),caller::get,(id,c)->null);
        long id=api.miPerfilTaxista().optLong("id");
        assertEquals(403,assertThrows(TaxiApiException.class,()->api.listarTaxistas(null)).status);
        caller.set(new FakeTaxiApi.Caller("sa","SUPERADMIN","SA","sa@test.pe",null));
        assertEquals(1,api.listarTaxistas("APROBADO").optJSONArray("taxistas").length());
        assertEquals(0,api.listarTaxistas("PENDIENTE").optJSONArray("taxistas").length());
        assertFalse(api.cambiarActivoTaxista(id,false,"Solicitud del usuario").optBoolean("activo"));
        assertEquals(404,assertThrows(TaxiApiException.class,()->api.cambiarActivoTaxista(99999,true,null)).status);
        caller.set(new FakeTaxiApi.Caller("driver","TAXISTA","Ana","ana@test.pe",null));
        assertEquals(403,assertThrows(TaxiApiException.class,api::miPerfilTaxista).status);
        caller.set(new FakeTaxiApi.Caller("client","CLIENT","Cliente","client@test.pe",null));
        assertEquals(403,assertThrows(TaxiApiException.class,()->api.cambiarActivoTaxista(id,true,null)).status);
    }
}
