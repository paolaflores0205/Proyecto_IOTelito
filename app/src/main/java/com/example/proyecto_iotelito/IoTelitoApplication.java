package com.example.proyecto_iotelito;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Aplica a todas las pantallas el espacio seguro de las barras del sistema. */
public class IoTelitoApplication extends Application implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityPostCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        // Esperar a que termine onCreate: antes de setContentView el menú todavía
        // no existe y se trataría la pantalla como si no tuviera navegación.
        View content = activity.findViewById(android.R.id.content);
        if (content == null) return;

        int initialLeft = content.getPaddingLeft();
        int initialTop = content.getPaddingTop();
        int initialRight = content.getPaddingRight();
        int initialBottom = content.getPaddingBottom();
        View clientBottomNavigation = activity.findViewById(R.id.bottom_nav);
        View superadminBottomNavigation = activity.findViewById(R.id.superadmin_bottom_nav);
        View bottomNavigation = clientBottomNavigation;
        if (bottomNavigation == null) {
            bottomNavigation = superadminBottomNavigation;
        }
        boolean hasBottomNavigation = bottomNavigation != null;
        View navigationView = bottomNavigation;

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            // Las pantallas con menú inferior gestionan su inset como margen del
            // propio menú. Aplicarlo también aquí generaba la franja vacía del A55.
            int bottomInset = hasBottomNavigation ? 0 : safeInsets.bottom;
            view.setPadding(
                    initialLeft + safeInsets.left,
                    initialTop + safeInsets.top,
                    initialRight + safeInsets.right,
                    initialBottom + bottomInset
            );
            if (hasBottomNavigation) {
                // El contenedor aplica los insets una sola vez. Material puede
                // reinstalar su listener al adjuntarse; por eso no lo sustituimos.
                // El espacio inferior queda como margen externo, no como padding.
                if (navigationView instanceof BottomNavigationView) {
                    navigationView.setPadding(navigationView.getPaddingLeft(), 0,
                            navigationView.getPaddingRight(), 0);
                }
                ViewGroup.LayoutParams rawParams = navigationView.getLayoutParams();
                if (rawParams instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) rawParams;
                    if (params.bottomMargin != safeInsets.bottom) {
                        params.bottomMargin = safeInsets.bottom;
                        navigationView.setLayoutParams(params);
                    }
                }
                // Los hijos ya están dentro del área segura. No deben sumar de
                // nuevo las barras, pero conservan los insets del teclado (IME).
                return new WindowInsetsCompat.Builder(windowInsets)
                        .setInsets(WindowInsetsCompat.Type.systemBars()
                                | WindowInsetsCompat.Type.displayCutout(), Insets.NONE)
                        .build();
            }
            return windowInsets;
        });

        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);

        // Todos los roles usan un menú blanco y una barra del sistema clara.
        activity.getWindow().setNavigationBarColor(activity.getColor(R.color.white));
        activity.getWindow().setNavigationBarContrastEnforced(false);
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView())
                .setAppearanceLightNavigationBars(true);
        ViewCompat.requestApplyInsets(content);
    }

    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) { }
    @Override public void onActivityStarted(@NonNull Activity activity) { }
    @Override public void onActivityResumed(@NonNull Activity activity) { }
    @Override public void onActivityPaused(@NonNull Activity activity) { }
    @Override public void onActivityStopped(@NonNull Activity activity) { }
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }
    @Override public void onActivityDestroyed(@NonNull Activity activity) { }
}
