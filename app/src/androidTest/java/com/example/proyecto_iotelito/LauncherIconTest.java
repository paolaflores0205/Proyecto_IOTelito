package com.example.proyecto_iotelito;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LauncherIconTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private Bitmap render(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, 108, 108);
        drawable.draw(new Canvas(bitmap));
        return bitmap;
    }
    @Test public void iconosNormalYRedondoUsanElEdificioTurquesaSobreAzul() {
        for (int id : new int[]{R.mipmap.ic_launcher, R.mipmap.ic_launcher_round}) {
            Drawable drawable = context.getDrawable(id);
            assertTrue(drawable instanceof AdaptiveIconDrawable);
            assertNotNull(((AdaptiveIconDrawable) drawable).getMonochrome());
            Bitmap bitmap = render(drawable);
            assertEquals(context.getColor(R.color.io_teal), bitmap.getPixel(54, 54));
            assertEquals(context.getColor(R.color.io_navy), bitmap.getPixel(54, 10));
            bitmap.recycle();
        }
        // Algunos launchers eligen roundIcon como ícono de la aplicación.
        int selectedIcon = context.getApplicationInfo().icon;
        assertTrue(selectedIcon == R.mipmap.ic_launcher || selectedIcon == R.mipmap.ic_launcher_round);
    }
    @Test public void versionTematicaConservaElEdificioSinFondoOpaco() {
        Bitmap bitmap = render(context.getDrawable(R.drawable.ic_launcher_monochrome));
        assertEquals(0xFFFFFFFF, bitmap.getPixel(54, 54));
        assertEquals(0, bitmap.getPixel(10, 10));
        bitmap.recycle();
    }
}
