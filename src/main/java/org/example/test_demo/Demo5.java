package org.example.test_demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Demo5 {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                try {
//                    System.out.println(Thread.currentThread().getName());
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return finalI;
            }, executorService);
            futures.add(future);
        }
        for (CompletableFuture<Integer> future : futures) {
            System.out.println(future.get());
        }
        executorService.shutdown();
    }
}
