package com.example.proyecto_iotelito.data.hoteladmin;

import com.example.proyecto_iotelito.model.hoteladmin.ReservaAdmin;
import org.junit.Test;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

public class HotelOperationsCodecTest {
    private final LocalDate today=LocalDate.of(2026,11,3);
    private Map<String,Object> booking(){
        Map<String,Object> data=new HashMap<>();
        data.put("id",2001L);data.put("hotelId",1L);data.put("clientUid","client");
        data.put("roomIds",Arrays.asList(101,102));data.put("roomName","Doble");data.put("roomNumber","101 y 102");
        data.put("fechaEntrada","2026-11-02");data.put("fechaSalida","2026-11-04");
        data.put("precioTotal",400.50);data.put("estado","CONFIRMADA");data.put("codigo","QA-2001");return data;
    }
    private ReservaAdmin decode(Map<String,Object> data){return HotelOperationsCodec.decode("2001",data,1,today);}
    @Test public void reservaAnteriorSinNombreConservaIdentidad(){
        ReservaAdmin row=decode(booking());assertEquals("Huésped · QA-2001",row.huespedNombre);
        assertEquals(2,row.cantidadHabitaciones);assertEquals(2,row.noches);assertEquals(400.50,row.total(),0.001);
        assertEquals(ReservaAdmin.EstadoCheckout.HOSPEDADO,row.estado);
    }
    @Test public void estadosEnLimitesDeEstadia(){
        LocalDate start=today,end=today.plusDays(2);
        assertEquals(ReservaAdmin.EstadoCheckout.PROXIMA,HotelOperationsCodec.status("CONFIRMADA",start,end,today.minusDays(1)));
        assertEquals(ReservaAdmin.EstadoCheckout.HOSPEDADO,HotelOperationsCodec.status("CONFIRMADA",start,end,today));
        assertEquals(ReservaAdmin.EstadoCheckout.CHECKOUT_PENDIENTE,HotelOperationsCodec.status("CONFIRMADA",start,end,end));
        assertEquals(ReservaAdmin.EstadoCheckout.FINALIZADA,HotelOperationsCodec.status("COMPLETADA",start,end,today));
    }
    @Test public void noPresentaCanceladasNiPendientesComoConfirmadas(){
        Map<String,Object> data=booking();data.put("estado","CANCELADA");assertNull(decode(data));
        data.put("estado","PENDIENTE_PAGO");assertNull(decode(data));
    }
    @Test public void rechazaHotelAjenoEIdentidadInconsistente(){
        Map<String,Object> data=booking();data.put("hotelId",2L);assertThrows(IllegalArgumentException.class,()->decode(data));
        data.put("hotelId",1L);data.put("id",2002L);assertThrows(IllegalArgumentException.class,()->decode(data));
    }
    @Test public void rechazaFechasYPreciosInvalidos(){
        Map<String,Object> data=booking();data.put("fechaSalida","2026-11-01");assertThrows(IllegalArgumentException.class,()->decode(data));
        data.put("fechaSalida","2026-11-04");data.put("precioTotal",Double.NaN);assertThrows(IllegalArgumentException.class,()->decode(data));
    }
    @Test public void importesExigenPrecisionYLimites(){
        assertEquals(413,HotelOperationsCodec.total(400.50,12.50),0.001);
        for(double invalid:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY,1.001,1000001})
            assertThrows(IllegalArgumentException.class,()->HotelOperationsCodec.total(100,invalid));
    }
    @Test public void sinComprobantesNoInventaIngresos(){
        HotelOperationsCodec.Report r=HotelOperationsCodec.report(Collections.singletonList(decode(booking())),Collections.emptyList(),4,today,"MONTH");
        assertEquals(1,r.bookings);assertEquals(0,r.income,0.001);assertEquals(100.0*4/120,r.occupancy,0.001);
        assertArrayEquals(new double[4],r.buckets,0.001);
    }
    @Test public void ocupaSoloNochesDentroDelMes(){
        Map<String,Object> data=booking();data.put("fechaEntrada","2026-10-30");data.put("fechaSalida","2026-11-02");
        HotelOperationsCodec.Report r=HotelOperationsCodec.report(Collections.singletonList(decode(data)),Collections.emptyList(),2,today,"MONTH");
        assertEquals(0,r.bookings);assertEquals(100.0*2/60,r.occupancy,0.001);
    }
    @Test public void ingresosSeAgrupanPorFechaDeRegistroNoDeReserva(){
        List<HotelOperationsCodec.Receipt> receipts=Arrays.asList(
                new HotelOperationsCodec.Receipt(LocalDateTime.of(2026,11,1,0,0),100.25),
                new HotelOperationsCodec.Receipt(LocalDateTime.of(2026,11,30,23,59),20),
                new HotelOperationsCodec.Receipt(LocalDateTime.of(2026,12,1,0,0),999));
        HotelOperationsCodec.Report r=HotelOperationsCodec.report(Collections.emptyList(),receipts,0,today,"MONTH");
        assertEquals(120.25,r.income,0.001);assertArrayEquals(new double[]{100.25,0,0,20},r.buckets,0.001);assertNull(r.occupancy);
    }
    @Test public void periodosDiarioYAnualRespetanLimites(){
        List<HotelOperationsCodec.Receipt> receipts=Arrays.asList(
                new HotelOperationsCodec.Receipt(today.atTime(6,0),15),
                new HotelOperationsCodec.Receipt(today.atTime(23,59),20),
                new HotelOperationsCodec.Receipt(today.plusDays(1).atStartOfDay(),30));
        HotelOperationsCodec.Report day=HotelOperationsCodec.report(Collections.emptyList(),receipts,1,today,"DAY");
        assertEquals(35,day.income,0.001);assertArrayEquals(new double[]{0,15,0,20},day.buckets,0.001);
        assertEquals(65,HotelOperationsCodec.report(Collections.emptyList(),receipts,1,today,"YEAR").income,0.001);
    }
}
