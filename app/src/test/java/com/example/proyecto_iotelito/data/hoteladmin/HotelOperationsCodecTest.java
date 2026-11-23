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
        assertEquals(ReservaAdmin.EstadoCheckout.HOSPEDADO,HotelOperationsCodec.status("CONFIRMADA",start,end,end));
        assertEquals(ReservaAdmin.EstadoCheckout.CHECKOUT_PENDIENTE,HotelOperationsCodec.status("EN_CHECKOUT",start,end,end));
        assertEquals(ReservaAdmin.EstadoCheckout.FINALIZADA,HotelOperationsCodec.status("FINALIZADA",start,end,end));
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
    private static HotelOperationsCodec.ServiceLine line(int id,String name,double price,int qty){
        return new HotelOperationsCodec.ServiceLine(id,name,price,qty);
    }
    @Test public void ingresosPorServicioDeMenorAMayorYSoloDelPeriodo(){
        List<HotelOperationsCodec.Receipt> receipts=Arrays.asList(
                new HotelOperationsCodec.Receipt(LocalDateTime.of(2026,11,5,10,0),300,Arrays.asList(line(1,"Spa",80,1),line(2,"Desayuno",42.5,2))),
                new HotelOperationsCodec.Receipt(LocalDateTime.of(2026,11,20,9,0),150,Arrays.asList(line(2,"Desayuno",42.5,1),line(3,"Lavandería",15,1))),
                new HotelOperationsCodec.Receipt(LocalDateTime.of(2026,12,2,9,0),999,Collections.singletonList(line(1,"Spa",500,1))));
        HotelOperationsCodec.Report r=HotelOperationsCodec.report(Collections.emptyList(),receipts,0,today,"MONTH");
        assertEquals(3,r.serviceIncome.size());
        assertEquals("Lavandería",r.serviceIncome.get(0).concepto);assertEquals(15,r.serviceIncome.get(0).monto,0.001);
        assertEquals("Spa",r.serviceIncome.get(1).concepto);assertEquals(80,r.serviceIncome.get(1).monto,0.001);
        assertEquals("Desayuno",r.serviceIncome.get(2).concepto);assertEquals(127.5,r.serviceIncome.get(2).monto,0.001);
        assertEquals(5,r.servicesSold);
    }
    @Test public void lineasDeServicioValidanYSeLeenDeFirestore(){
        HotelOperationsCodec.ServiceLine l=line(7,"Desayuno",19.9,3);
        assertEquals(19.9*3,l.amount(),0);
        HotelOperationsCodec.ServiceLine back=HotelOperationsCodec.ServiceLine.from(l.toMap());
        assertNotNull(back);assertEquals(3,back.quantity);assertEquals("Desayuno",back.name);
        assertNull(HotelOperationsCodec.ServiceLine.from("texto"));
        assertEquals(1,HotelOperationsCodec.ServiceLine.list(Arrays.asList(l.toMap(),Collections.singletonMap("name","x"))).size());
        for(int qty:new int[]{0,100})
            assertThrows(IllegalArgumentException.class,()->line(1,"Spa",10,qty));
        assertThrows(IllegalArgumentException.class,()->line(1," ",10,1));
        assertThrows(IllegalArgumentException.class,()->line(1,"Spa",-1,1));
        assertEquals(19.9*3+80,HotelOperationsCodec.servicesTotal(Arrays.asList(l,line(1,"Spa",80,1))),0);
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
