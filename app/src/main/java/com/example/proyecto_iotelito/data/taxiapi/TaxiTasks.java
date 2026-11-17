package com.example.proyecto_iotelito.data.taxiapi;

import android.os.*;

import androidx.lifecycle.*;

import java.util.concurrent.*;
import java.util.function.Consumer;

/** Operaciones cortas y consultas ligadas a RESUMED; nunca bloquea el hilo UI. */
public final class TaxiTasks {
    private static final ExecutorService IO = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private TaxiTasks() {}

    public static String message(Throwable e) {
        return e instanceof TaxiApiException
                ? e.getMessage()
                : "El servicio de taxis no está disponible";
    }

    public static <T> void run(
            LifecycleOwner owner, Callable<T> task, Consumer<T> ok, Consumer<String> fail) {
        IO.execute(
                () -> {
                    try {
                        T value = task.call();
                        MAIN.post(
                                () -> {
                                    if (owner.getLifecycle().getCurrentState()
                                            != Lifecycle.State.DESTROYED) ok.accept(value);
                                });
                    } catch (Exception e) {
                        MAIN.post(
                                () -> {
                                    if (owner.getLifecycle().getCurrentState()
                                            != Lifecycle.State.DESTROYED) fail.accept(message(e));
                                });
                    }
                });
    }

    public static <T> void poll(
            LifecycleOwner owner,
            long ms,
            Callable<T> task,
            Consumer<T> ok,
            Consumer<String> fail) {
        owner.getLifecycle()
                .addObserver(
                        new DefaultLifecycleObserver() {
                            boolean visible, busy;
                            int generation;
                            final Runnable tick =
                                    new Runnable() {
                                        public void run() {
                                            if (!visible || busy) return;
                                            busy = true;
                                            int started = generation;
                                            IO.execute(
                                                    () -> {
                                                        T result = null;
                                                        String error = null;
                                                        try {
                                                            result = task.call();
                                                        } catch (Exception e) {
                                                            error = message(e);
                                                        }
                                                        T value = result;
                                                        String failure = error;
                                                        MAIN.post(
                                                                () -> {
                                                                    busy = false;
                                                                    if (!visible
                                                                            || started
                                                                                    != generation) {
                                                                        if (visible)
                                                                            MAIN.post(tick);
                                                                        return;
                                                                    }
                                                                    if (failure == null)
                                                                        ok.accept(value);
                                                                    else fail.accept(failure);
                                                                    MAIN.postDelayed(tick, ms);
                                                                });
                                                    });
                                        }
                                    };

                            public void onResume(LifecycleOwner o) {
                                visible = true;
                                generation++;
                                MAIN.post(tick);
                            }

                            public void onPause(LifecycleOwner o) {
                                visible = false;
                                generation++;
                                MAIN.removeCallbacks(tick);
                            }

                            public void onDestroy(LifecycleOwner o) {
                                visible = false;
                                generation++;
                                MAIN.removeCallbacks(tick);
                                o.getLifecycle().removeObserver(this);
                            }
                        });
    }
}
