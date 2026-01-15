package br.com.prospectaai.ms_async_task.domain.util;

public class VirtualThread {
    public static void callAsync(Runnable runnable) {
        Thread.ofVirtual().start(runnable);
    }
}
