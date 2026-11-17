package com.example.proyecto_iotelito.ui.taxi;

import android.app.Activity;
import android.view.*;
import android.widget.*;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.taxiapi.TaxiApis;

/** Avisos exclusivos del flujo taxi; no altera las pantallas de reservas. */
public final class TaxiUi {
    private TaxiUi() {}

    public static void status(Activity a, String error) {
        ViewGroup content = a.findViewById(android.R.id.content);
        TextView label = content.findViewWithTag("taxi_api_status");
        if (label == null) {
            if (content.getChildCount() == 0) return;
            View root = content.getChildAt(0);
            content.removeView(root);
            LinearLayout shell = new LinearLayout(a);
            shell.setOrientation(LinearLayout.VERTICAL);
            label = new TextView(a);
            label.setTag("taxi_api_status");
            label.setTextSize(13);
            label.setPadding(24, 12, 24, 12);
            label.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            shell.addView(label, new LinearLayout.LayoutParams(-1, -2));
            shell.addView(root, new LinearLayout.LayoutParams(-1, 0, 1));
            content.addView(shell, new ViewGroup.LayoutParams(-1, -1));
        }
        label.setText(
                error != null
                        ? error
                        : TaxiApis.fake()
                                ? "Modo de prueba · API de taxis simulada en memoria"
                                : "");
        label.setVisibility(error != null || TaxiApis.fake() ? View.VISIBLE : View.GONE);
        label.setBackgroundColor(
                a.getColor(error == null ? R.color.io_navy : R.color.io_danger_bg));
        label.setTextColor(a.getColor(error == null ? R.color.white : R.color.io_danger_text));
    }
}
