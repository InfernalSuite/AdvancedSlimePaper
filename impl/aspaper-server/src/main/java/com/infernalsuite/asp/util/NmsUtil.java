package com.infernalsuite.asp.util;

import com.infernalsuite.asp.InternalPlugin;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.scheduler.CraftScheduler;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CountDownLatch;

public class NmsUtil {

    public static long asLong(int chunkX, int chunkZ) {
        return (((long) chunkZ) * Integer.MAX_VALUE + ((long) chunkX));
        //return (long)chunkX & 4294967295L | ((long)chunkZ & 4294967295L) << 32;
    }

    public static void runSyncAndWait(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        RuntimeException[] runtimeException = new RuntimeException[1];

        Bukkit.getScheduler().runTask(new InternalPlugin(), () -> {
            try {
                runnable.run();
            } catch (RuntimeException e) {
                runtimeException[0] = e;
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e); // Rather propagate the interrupt (and thus prevent further execution) than continue
        }

        if (runtimeException[0] != null) {
            throw runtimeException[0];
        }
    }
}
