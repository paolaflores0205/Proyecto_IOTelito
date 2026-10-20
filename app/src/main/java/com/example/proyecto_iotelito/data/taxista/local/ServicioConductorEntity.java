package com.example.proyecto_iotelito.data.taxista.local;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import com.example.proyecto_iotelito.model.taxista.SolicitudServicio;
import com.example.proyecto_iotelito.model.taxista.ServicioHistorial;
import com.example.proyecto_iotelito.ui.taxista.EstadoServicio;
import java.text.DateFormat;
import java.util.Date;

/** Copia propia del conductor; nunca cambia el esquema del cliente. */
@Entity(tableName="conductor_servicios", primaryKeys={"cuenta","id"})
public class ServicioConductorEntity {
    @NonNull public String cuenta="";
    public int id;
    public boolean cliente;
    public String estado="SOLICITADO", hotel="", origen="", destino="", nombre="", iniciales="";
    public String fecha="", detalle="", distancia="", duracion="", motivo;
    public int minutos;
    public double tarifa;
    public long finalizado;
    public static ServicioConductorEntity from(String cuenta, SolicitudServicio s, boolean cliente) {
        ServicioConductorEntity e=new ServicioConductorEntity();
        e.cuenta=cuenta; e.id=s.id; e.cliente=cliente; e.hotel=s.hotel; e.origen=s.hotel;
        e.destino=s.destino; e.nombre=s.pasajeroNombre; e.iniciales=s.pasajeroIniciales;
        e.fecha=s.fechaHora; e.detalle=s.distanciaTiempo; e.minutos=s.minutosEspera; e.tarifa=s.tarifa;
        if(!cliente&&s.id>=1&&s.id<=6){
            String[] distancias={"18.2 km","22.5 km","17.8 km","24 km","16.5 km","23 km"};
            String[] duraciones={"25 min","34 min","24 min","38 min","23 min","36 min"};
            e.distancia=distancias[s.id-1];e.duracion=duraciones[s.id-1];
        }
        return e;
    }
    public SolicitudServicio solicitud() {
        return new SolicitudServicio(id,minutos,hotel,destino,fecha,nombre,iniciales,detalle,tarifa);
    }
    public ServicioHistorial historial() {
        String hora=finalizado>0 ? DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(finalizado)) : fecha;
        return new ServicioHistorial(id,hora,EstadoServicio.valueOf(estado),hotel,destino,nombre,
                "Toyota Corolla",distancia,duracion,motivo,tarifa);
    }
}
