package com.example.proyecto_iotelito;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/** Aplica a todas las pantallas el espacio seguro de las barras del sistema. */
public class IoTelitoApplication extends Application implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        View content = activity.findViewById(android.R.id.content);
        if (content == null) return;

        int initialLeft = content.getPaddingLeft();
        int initialTop = content.getPaddingTop();
        int initialRight = content.getPaddingRight();
        int initialBottom = content.getPaddingBottom();
        View bottomNavigation = activity.findViewById(R.id.bottom_nav);
        if (bottomNavigation == null) {
            bottomNavigation = activity.findViewById(R.id.superadmin_bottom_nav);
        }
        boolean hasBottomNavigation = bottomNavigation != null;
        int maximumBottomNavigationInset = getResources()
                .getDimensionPixelSize(R.dimen.bottom_navigation_safe_space);

        if (bottomNavigation != null) {
            View navigationView = bottomNavigation;
            int navigationLeft = navigationView.getPaddingLeft();
            int navigationTop = navigationView.getPaddingTop();
            int navigationRight = navigationView.getPaddingRight();
            int navigationBottom = navigationView.getPaddingBottom();

            // Material agrega automáticamente el inset inferior al BottomNavigationView.
            // En Samsung esto duplicaba el espacio porque la barra del sistema ya ocupa
            // su propia franja. Sustituimos ese listener para conservar solo el padding
            // original del componente.
            ViewCompat.setOnApplyWindowInsetsListener(navigationView, (view, windowInsets) -> {
                view.setPadding(
                        navigationLeft,
                        navigationTop,
                        navigationRight,
                        navigationBottom
                );
                return windowInsets;
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            int bottomInset = hasBottomNavigation
                    ? Math.min(safeInsets.bottom, maximumBottomNavigationInset)
                    : safeInsets.bottom;
            view.setPadding(
                    initialLeft + safeInsets.left,
                    initialTop + safeInsets.top,
                    initialRight + safeInsets.right,
                    initialBottom + bottomInset
            );
            return windowInsets;
        });

        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView())
                .setAppearanceLightNavigationBars(true);
        ViewCompat.requestApplyInsets(content);
    }

    @Override public void onActivityStarted(@NonNull Activity activity) { }
    @Override public void onActivityResumed(@NonNull Activity activity) { }
    @Override public void onActivityPaused(@NonNull Activity activity) { }
    @Override public void onActivityStopped(@NonNull Activity activity) { }
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }
    @Override public void onActivityDestroyed(@NonNull Activity activity) { }
}
