package br.com.prospectaai.ms_async_task.domain.util;

import java.util.concurrent.Executors;

public class VirtualThread {
    public static void callAsync(Runnable runnable) {
        try(var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(runnable);
        }
    }
}
