import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

enum Status {
    MOVING,
    STOPPED,
    DOOR_OPEN
}

// КЛАСС ЛИФТА
public class Elevator implements Runnable, Constants {
    private final int id;

    // Для гарантии видимости изменений между потоками
    private volatile int currentFloor;
    private volatile int targetFloor;
    private volatile boolean movementDirection;
    private volatile Status status;
    private volatile boolean isProcessingCommand = false;

    // Общие настройки для всех лифтов
    private static volatile int speed = 5;
    private static volatile int openedDoorsPeriod = 5;

    // Потокобезопасная очередь команд
    private final BlockingDeque<Command> commands;

    public Elevator(int id) {
        this.id = id;
        this.currentFloor = 1;
        this.targetFloor = 1;
        this.movementDirection = UP;
        this.status = Status.STOPPED;
        this.commands = new LinkedBlockingDeque<>();
    }

    // ГЕТТЕРЫ
    public int getID() {
        return id;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public int getTargetFloor() {
        return targetFloor;
    }

    public boolean getDirection() {
        return movementDirection;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isProcessing() {
        return isProcessingCommand;
    }

    public int getTaskCount() {
        return commands.size();
    }

    // СТАТИЧЕСКИЕ ГЕТТЕРЫ И СЕТТЕРЫ
    public static int getSpeed() {
        return speed;
    }

    public static void setSpeed(int newSpeed) {
        if (newSpeed >= MIN_SPEED && newSpeed <= MAX_ELEVATOR_SPEED) {
            speed = newSpeed;
            Logger.info(CONFIG, "Elevator speed set to " + newSpeed + " s/floor");
        } else {
            Logger.error(CONFIG, "Invalid speed: " + newSpeed);
        }
    }

    public static int getOpenedDoorsPeriod() {
        return openedDoorsPeriod;
    }

    public static void setOpenedDoorsPeriod(int period) {
        if (period >= MIN_SPEED && period <= MAX_DOOR_SPEED) {
            openedDoorsPeriod = period;
            Logger.info(CONFIG, "Door period set to " + period + "s");
        } else {
            Logger.error(CONFIG, "Invalid door period: " + period);
        }
    }

    // ВЫЧИСЛЕНИЯ
    // Расчёт расстояния до целевого этажа
    public int calculateDistance(int floor) {
        return Math.abs(this.currentFloor - floor);
    }

    // Проверка, нахождения этажа по ходу движения лифта
    public boolean isOnTheWay(int floor, boolean requestDirection) {
        if (status != Status.MOVING) {
            return false;
        }

        if (movementDirection != requestDirection) {
            return false;
        }

        if (movementDirection == UP) {
            return floor >= currentFloor && floor <= targetFloor;
        } else {
            return floor <= currentFloor && floor >= targetFloor;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Elevator №").append(id)
                .append(" | Floor: ").append(currentFloor);

        Status currentStatus = status; // локальная копия для консистентности
        if (currentStatus == Status.MOVING) {
            sb.append(" | Moving ").append(movementDirection == UP ? "↑" : "↓")
                    .append(" to ").append(targetFloor);
        } else if (currentStatus == Status.DOOR_OPEN) {
            sb.append(" | Doors open");
        } else {
            sb.append(" | Idle");
        }

        int queueSize = commands.size();
        if (queueSize > 0) {
            sb.append(" | Queue: ").append(queueSize);
        }

        return sb.toString();
    }

    // УПРАВЛЕНИЕ КОМАНДАМИ

    // Добавление команды в очередь (из Dispatcher)
    public void addCommand(Command command) {
        if (command == null || !command.isValid()) {
            Logger.error("Elevator №" + id, "Received invalid command");
            return;
        }

        try {
            commands.put(command);
            Logger.elevator(id, "Command queued: " + command + " (queue size: " + commands.size() + ")");
        } catch (InterruptedException e) {
            Logger.error("Elevator №" + id, "Interrupted while adding command");
            Thread.currentThread().interrupt();
        }
    }

    // ДЕЙСТВИЯ ЛИФТА
    private void move(int floor) {
        // Валидация
        if (floor < MIN_FLOORS || floor > MAX_FLOORS) {
            Logger.error("Elevator №" + id, "Invalid floor: " + floor);
            return;
        }

        int distance = calculateDistance(floor);

        if (distance == 0) {
            Logger.elevator(id, "Already on floor " + currentFloor);
            return;
        }

        // Устанавливаем параметры движения
        this.targetFloor = floor;
        this.movementDirection = (floor > currentFloor) ? UP : DOWN;
        this.status = Status.MOVING;

        long travelTime = (long) distance * speed;
        String direction = movementDirection == UP ? UP_STR : DOWN_STR;

        Logger.elevator(id, "Moving " + direction + ": " + currentFloor + " -> " + floor +
                " (" + formatTime(travelTime) + ")");

        try {
            TimeUnit.SECONDS.sleep(travelTime);
            this.currentFloor = floor;
            Logger.elevator(id, "Arrived at floor " + currentFloor);
        } catch (InterruptedException e) {
            Logger.error("Elevator №" + id, "Movement interrupted");
            Thread.currentThread().interrupt();
        }
    }

    private void openDoors() {
        this.status = Status.DOOR_OPEN;
        Logger.elevator(id, "Doors OPENING on floor " + currentFloor);

        try {
            TimeUnit.SECONDS.sleep(openedDoorsPeriod);
            Logger.elevator(id, "Doors CLOSING on floor " + currentFloor);
        } catch (InterruptedException e) {
            Logger.error("Elevator №" + id, "Door operation interrupted");
            Thread.currentThread().interrupt();
        }
    }

    private void stop() {
        this.status = Status.STOPPED;
        Logger.elevator(id, "Stopped at floor " + currentFloor);
    }

    // Форматирование времени
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0)
            sb.append(hours).append("h ");
        if (minutes > 0)
            sb.append(minutes).append("m ");
        if (secs > 0)
            sb.append(secs).append("s");

        return sb.toString().trim();
    }

    // ОСНОВНОЙ ЦИКЛ ЛИФТА
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Блокирующее ожидание команды
                Command command = commands.take();
                isProcessingCommand = true;

                int pickupFloor = command.getFirstTarget();
                int destinationFloor = command.getSecondTarget();

                // Логирование в зависимости от режима
                if (command.getMode() == Command.Mode.EXTERNAL) {
                    Logger.elevator(id, "Processing EXTERNAL request: pickup floor = " + pickupFloor +
                            ", direction = " + (command.getDirection() ? "UP" : "DOWN") +
                            ", destination = " + destinationFloor);
                } else {
                    Logger.elevator(id, "Processing INTERNAL request: pickup = " + pickupFloor +
                            ", destination = " + destinationFloor);
                }

                // Движение к пассажиру
                if (currentFloor != pickupFloor) {
                    move(pickupFloor);
                }
                stop();
                openDoors();

                // Перемещение пассажира к цели
                if (currentFloor != destinationFloor) {
                    move(destinationFloor);
                }
                stop();
                openDoors();

                Logger.elevator(id, "Request COMPLETED");
                isProcessingCommand = false;

            } catch (InterruptedException e) {
                Logger.warning("Elevator №" + id, "Received shutdown signal");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Logger.error("Elevator №" + id, "Unexpected error: " + e.getMessage());
            }
        }

        Logger.elevator(id, "Stopped");
    }
}