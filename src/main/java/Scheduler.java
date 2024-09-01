import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final hj a; // Ваш класс для работы с сокетами

    public Scheduler(hj socketClient) {
        this.a = socketClient;
        startScheduledTasks();
    }

    private void startScheduledTasks() {
        long initialDelay = calculateInitialDelayToNextHour(); // Задержка до следующего целого часа
        scheduler.scheduleAtFixedRate(() -> {
            if (this.a != null) {
                System.out.println("Sending 'Top' command.");
                this.a.socket.emit("Top"); // Отправка сообщения
            } else {
                System.out.println("Socket client is not initialized.");
            }
        }, initialDelay, 1, TimeUnit.HOURS); // Выполнять каждую 1 час
    }

    private long calculateInitialDelayToNextHour() {
        long currentMillis = System.currentTimeMillis();
        long millisInAnHour = TimeUnit.HOURS.toMillis(1);
        long currentHourMillis = currentMillis % millisInAnHour;
        long nextHourMillis = currentHourMillis == 0 ? millisInAnHour : millisInAnHour - currentHourMillis;
        return nextHourMillis;
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}