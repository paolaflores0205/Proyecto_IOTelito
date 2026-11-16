package com.example.proyecto_iotelito.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;

/** Informa la conexión mientras la pantalla está visible, sin mantener callbacks en segundo plano. */
public final class ConexionCliente extends LiveData<Boolean> {
    private final ConnectivityManager manager;
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean registered;
    public ConexionCliente(Context context) {
        manager = context.getApplicationContext().getSystemService(ConnectivityManager.class);
        setValue(disponible(context));
    }
    public static boolean disponible(Context context) {
        ConnectivityManager manager = context.getApplicationContext().getSystemService(ConnectivityManager.class);
        NetworkCapabilities caps = manager == null ? null : manager.getNetworkCapabilities(manager.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
    private void actualizar() {
        main.post(() -> {
            if (!hasActiveObservers()) return;
            NetworkCapabilities caps = manager.getNetworkCapabilities(manager.getActiveNetwork());
            boolean online = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            if (!Boolean.valueOf(online).equals(getValue())) setValue(online);
        });
    }
    private final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network network) { actualizar(); }
        @Override public void onLost(Network network) { actualizar(); }
        @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) { actualizar(); }
    };
    @Override protected void onActive() {
        if (manager == null) return;
        manager.registerDefaultNetworkCallback(callback);
        registered = true;
        actualizar();
    }
    @Override protected void onInactive() {
        if (registered) manager.unregisterNetworkCallback(callback);
        registered = false;
        main.removeCallbacksAndMessages(null);
    }
}
