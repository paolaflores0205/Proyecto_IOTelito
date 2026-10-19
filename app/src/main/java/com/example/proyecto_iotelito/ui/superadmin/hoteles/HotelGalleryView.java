package com.example.proyecto_iotelito.ui.superadmin.hoteles;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.*;
import com.example.proyecto_iotelito.R;
import com.google.android.material.button.MaterialButton;
import java.util.*;

/** Carrusel horizontal compartido por formulario y detalle. La primera foto es la portada. */
public class HotelGalleryView extends HorizontalScrollView {
    public HotelGalleryView(Context context, AttributeSet attrs) { super(context, attrs); setFillViewport(true); }
    public void show(List<String> photos, int fallback, Runnable changed) {
        removeAllViews();
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(photos.size() <= 1 ? android.view.Gravity.CENTER_HORIZONTAL : android.view.Gravity.START);
        addView(row, new LayoutParams(-2, -1));
        int count = Math.max(1, photos.size());
        for (int i = 0; i < count; i++) {
            final int index = i;
            LinearLayout card = new LinearLayout(getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(270), -1);
            params.setMarginEnd(count > 1 ? dp(10) : 0);
            row.addView(card, params);
            ImageView image = new ImageView(getContext());
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageResource(fallback);
            if (!photos.isEmpty()) image.setImageURI(Uri.parse(photos.get(i)));
            image.setContentDescription("Foto " + (i + 1) + " de " + count);
            card.addView(image, new LinearLayout.LayoutParams(-1, 0, 1));
            TextView caption = new TextView(getContext());
            caption.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            caption.setText(photos.isEmpty() ? "Sin fotos propias · imagen de ejemplo" : (i == 0 ? "Portada" : "Foto " + (i + 1)) + " · " + count + " fotos");
            if (!photos.isEmpty() && com.example.proyecto_iotelito.data.SampleHotelGalleries.isIllustrative(photos.get(i))) {
                caption.append(" · Ilustrativa");
            }
            card.addView(caption);
            if (changed != null && !photos.isEmpty()) {
                LinearLayout actions = new LinearLayout(getContext());
                MaterialButton cover = new MaterialButton(getContext());
                cover.setText(i == 0 ? "Portada" : "Usar portada");
                cover.setEnabled(i != 0);
                cover.setOnClickListener(v -> { String photo = photos.remove(index); photos.add(0, photo); changed.run(); });
                MaterialButton remove = new MaterialButton(getContext());
                remove.setText("Quitar");
                remove.setOnClickListener(v -> { photos.remove(index); changed.run(); });
                actions.addView(cover, new LinearLayout.LayoutParams(0, -2, 1));
                actions.addView(remove, new LinearLayout.LayoutParams(0, -2, 1));
                card.addView(actions);
            }
        }
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
