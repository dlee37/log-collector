package com.example.logcollector.util;

import java.util.concurrent.*;

public class TimeoutExecutor {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public <T> T runWithTimeout(Callable<T> task, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        Future<T> future = executor.submit(task);
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            future.cancel(true); // Interrupt the task
            throw e;
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
