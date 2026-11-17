package com.example.proyecto_iotelito.data.local.cliente;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import org.json.JSONException;
import org.json.JSONObject;

@Entity(tableName = "servicios_taxi", indices = {@Index(value = {"clienteId", "reservaId"}, unique = true)})
public class TaxiEntity {
    @PrimaryKey
    public int id;
    public int reservaId;
    public String clienteId;
    public String nombreCliente;
    public String hotel;
    public String origen;
    public String destino;
    public Integer hotelId;
    public String hotelCity, airportCode, airportName, airportCity;
    public String hora;
    public int pasajeros;
    public int maletas;
    public String estado;
    public long creado;
    public String taxistaId;
    public String conductor;

    public JSONObject comoJson() {
        try {
            return new JSONObject().put("id", id).put("reserva", reservaId).put("cliente", clienteId)
                    .put("nombre", nombreCliente).put("hotel", hotel).put("origen", origen)
                    .put("destino", destino).put("hora", hora).put("pasajeros", pasajeros)
                    .put("maletas", maletas).put("estado", estado).put("creado", creado)
                    .put("taxista", taxistaId).put("conductor", conductor)
                    .put("hotelId",hotelId).put("hotelCity",hotelCity).put("airportCode",airportCode)
                    .put("airportName",airportName).put("airportCity",airportCity);
        } catch (JSONException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static TaxiEntity desdeJson(JSONObject pedido) throws JSONException {
        TaxiEntity entity = new TaxiEntity();
        entity.id = pedido.getInt("id");
        entity.reservaId = pedido.getInt("reserva");
        entity.clienteId = pedido.getString("cliente");
        entity.nombreCliente = pedido.getString("nombre");
        entity.hotel = pedido.getString("hotel");
        entity.origen = pedido.optString("origen", entity.hotel);
        entity.destino = pedido.optString("destino", "Destino no registrado");
        entity.hotelId=pedido.has("hotelId")&&!pedido.isNull("hotelId")?pedido.getInt("hotelId"):null;
        entity.hotelCity=pedido.optString("hotelCity",null);entity.airportCode=pedido.optString("airportCode",null);
        entity.airportName=pedido.optString("airportName",null);entity.airportCity=pedido.optString("airportCity",null);
        entity.hora = pedido.getString("hora");
        entity.pasajeros = pedido.getInt("pasajeros");
        entity.maletas = pedido.getInt("maletas");
        entity.estado = pedido.getString("estado");
        entity.creado = pedido.getLong("creado");
        entity.taxistaId = pedido.optString("taxista", "");
        entity.conductor = pedido.optString("conductor", "");
        return entity;
    }
}
