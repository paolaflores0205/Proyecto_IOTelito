package com.example.proyecto_iotelito.data;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Cola local del cliente: las operaciones de disco nunca bloquean la pantalla. */
public final class ClientLocalTasks {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private ClientLocalTasks() { }
    public static void execute(Runnable task) { IO.execute(task); }
    public static <T> void run(Context context, LifecycleOwner owner, Callable<T> task, Consumer<T> success) {
        run(context, owner, task, success, () -> { });
    }
    public static <T> void run(Context context, LifecycleOwner owner, Callable<T> task,
                               Consumer<T> success, Runnable failed) {
        Context app = context.getApplicationContext();
        MutableLiveData<Result<T>> result = new MutableLiveData<>();
        result.observe(owner, value -> {
            result.removeObservers(owner);
            if (value.error == null) success.accept(value.data);
            else {
                Log.e("ClientLocalStorage", "No se completó la operación local", value.error);
                Toast.makeText(app, value.error instanceof IllegalArgumentException
                        ? value.error.getMessage() : "No se pudo cargar o guardar. Intenta nuevamente.", Toast.LENGTH_LONG).show();
                failed.run();
            }
        });
        IO.execute(() -> {
            try { result.postValue(new Result<>(task.call(), null)); }
            catch (Exception error) { result.postValue(new Result<>(null, error)); }
        });
    }
    private static class Result<T> {
        final T data;
        final Exception error;
        Result(T data, Exception error) { this.data = data; this.error = error; }
    }
}
