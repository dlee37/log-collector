package com.example.logcollector.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TimeoutExecutor {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public <T> T runWithTimeout(Callable<T> task, long timeout, TimeUnit unit) {
        Future<T> future = executor.submit(task);
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT, "Log retrieval timed out");
        } catch (ExecutionException e) {
            future.cancel(true);
            if (e.getCause() instanceof ResponseStatusException rse) {
                throw rse;
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error occurred");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error occurred");
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
