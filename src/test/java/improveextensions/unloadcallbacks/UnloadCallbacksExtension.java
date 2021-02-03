package improveextensions.unloadcallbacks;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventCallback;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.extensions.Extension;
import net.minestom.server.extras.selfmodification.MinestomRootClassLoader;
import net.minestom.server.utils.time.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import java.util.concurrent.atomic.AtomicBoolean;

public class UnloadCallbacksExtension extends Extension {

    private boolean ticked1 = false;
    private boolean ticked2 = false;
    private boolean tickedScheduledNonTransient = false;
    private boolean tickedScheduledTransient = false;
    private final EventCallback<InstanceTickEvent> callback = this::onTick;

    private void onTick(InstanceTickEvent e) {
        ticked1 = true;
    }

    @Override
    public void initialize() {
        GlobalEventHandler globalEvents = MinecraftServer.getGlobalEventHandler();
        // this callback will be automatically removed when unloading the extension
        globalEvents.addEventCallback(InstanceTickEvent.class, callback);
        // this one too
        globalEvents.addEventCallback(InstanceTickEvent.class, e -> ticked2 = true);

        // this callback will be cancelled
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            tickedScheduledNonTransient = true;
        }).repeat(100L, TimeUnit.MILLISECOND).schedule();

        // this callback will NOT be cancelled
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            tickedScheduledTransient = true;
        }).repeat(100L, TimeUnit.MILLISECOND).makeTransient().schedule();

        try {
            Assertions.assertTrue(MinestomRootClassLoader.findExtensionObjectOwner(callback).isPresent());
            Assertions.assertEquals("UnloadCallbacksExtension", MinestomRootClassLoader.findExtensionObjectOwner(callback).get());
        } catch (AssertionFailedError e) {
            e.printStackTrace();
            System.exit(-1);
        }

        MinecraftServer.getSchedulerManager().buildTask(() -> {
            // unload self
            MinecraftServer.getExtensionManager().unloadExtension(getDescription().getName());
        }).delay(1L, TimeUnit.SECOND).schedule();
    }

    @Override
    public void terminate() {
        ticked1 = false;
        ticked2 = false;
        tickedScheduledNonTransient = false;
        tickedScheduledTransient = false;

        AtomicBoolean executedDelayTaskAfterTerminate = new AtomicBoolean(false);
        // because terminate is called just before unscheduling and removing event callbacks,
        //  the following task will never be executed, because it is not transient
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            executedDelayTaskAfterTerminate.set(true);
        }).delay(100L, TimeUnit.MILLISECOND).schedule();

        // this shutdown tasks will not be executed because it is not transient
        MinecraftServer.getSchedulerManager().buildShutdownTask(() -> Assertions.fail("This shutdown task should be unloaded when the extension is")).schedule();

        MinecraftServer.getSchedulerManager().buildTask(() -> {
            // Make sure callbacks are disabled
            try {
                Assertions.assertFalse(ticked1, "ticked1 should be false because the callback has been unloaded");
                Assertions.assertFalse(ticked2, "ticked2 should be false because the callback has been unloaded");
                Assertions.assertFalse(tickedScheduledNonTransient, "tickedScheduledNonTransient should be false because the callback has been unloaded");
                Assertions.assertTrue(tickedScheduledTransient, "tickedScheduledNonTransient should be true because the callback has NOT been unloaded");
                Assertions.assertFalse(executedDelayTaskAfterTerminate.get(), "executedDelayTaskAfterTerminate should be false because the callback has been unloaded before executing");
                System.out.println("All tests passed.");
            } catch (AssertionFailedError e) {
                e.printStackTrace();
            }
            MinecraftServer.stopCleanly(); // TODO: fix deadlock which happens because stopCleanly waits on completion of scheduler tasks
        }).delay(1L, TimeUnit.SECOND).makeTransient().schedule();
    }
}
