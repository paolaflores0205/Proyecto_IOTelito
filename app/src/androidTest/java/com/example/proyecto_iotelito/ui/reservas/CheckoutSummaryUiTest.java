package com.example.proyecto_iotelito.ui.reservas;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.data.ComprobanteCheckout;
import java.time.LocalDate;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Inflación y presentación local; no inicia sesión ni consulta Firebase. */
@RunWith(AndroidJUnit4.class)
public class CheckoutSummaryUiTest {
    @Test public void tarjetaDelListadoMuestraTotalCobradoYNoLaBase(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context context=new ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext(),R.style.Theme_Proyecto_IOTelito);
            Reserva row=new Reserva(42,1,"Suite","101",LocalDate.of(2026,11,10),LocalDate.of(2026,11,13),"2 adultos",1200.50,Reserva.Estado.FINALIZADA,"QA42");
            Map<String,Object> data=new HashMap<>();data.put("reservationId","42");data.put("hotelId",1);data.put("clientUid","client");
            data.put("base",1200.50);data.put("extra",50.25);data.put("total",1250.75);
            row.comprobante=ComprobanteCheckout.deReserva(row,data,123,"client");
            ReservaAdapter adapter=new ReservaAdapter(reserva->{});
            adapter.submitList(Collections.singletonList(ReservaAdapter.Item.reserva(row)));
            RecyclerView.ViewHolder holder=adapter.onCreateViewHolder(new FrameLayout(context),adapter.getItemViewType(0));
            adapter.onBindViewHolder(holder,0);
            TextView amount=holder.itemView.findViewById(R.id.tv_precio_total);
            assertEquals("S/ 1,250.75",amount.getText().toString());
            row.comprobante=null;adapter.onBindViewHolder(holder,0);
            assertEquals("Total por consultar",amount.getText().toString());
            row.estado=Reserva.Estado.EN_CHECKOUT;adapter.onBindViewHolder(holder,0);
            assertEquals("S/ 1,200.50",amount.getText().toString());
        });
    }
}
