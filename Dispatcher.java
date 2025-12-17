import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;
import java.util.List;

/**
 * Диспетчер системы управления лифтами
 * Распределяет запросы между лифтами
 *
 * Потокобезопасность:
 * - ConcurrentLinkedQueue для хранения лифтов
 * - Чтение состояния лифтов через volatile поля
 */

public class Dispatcher implements Constants {

    private final ConcurrentLinkedQueue<Elevator> elevators;
    private final List<Thread> elevatorThreads;
    private volatile int maxFloors;
    private volatile int elevatorCount;
    private volatile Command.Mode commandMode;
    private volatile boolean isRunning;

    public Dispatcher() {
        this.elevators = new ConcurrentLinkedQueue<>();
        this.elevatorThreads = new ArrayList<>();
        this.maxFloors = DEFAULT_FLOORS;
        this.elevatorCount = 0;
        this.commandMode = Command.Mode.INTERNAL;
        this.isRunning = false;
    }

    // ГЕТТЕРЫ И СЕТТЕРЫ
    public ConcurrentLinkedQueue<Elevator> getElevators() {
        return elevators;
    }

    public int getMaxFloors() {
        return maxFloors;
    }

    public void setMaxFloors(int floors) {
        if (floors < MIN_FLOORS || floors > MAX_FLOORS) {
            Logger.error(DISPATCHER, "Invalid floor count: " + floors +
                        " (must be " + MIN_FLOORS + "-" + MAX_FLOORS + ")");
            return;
        }
        this.maxFloors = floors;
        Logger.info(CONFIG, "Max floors set to " + floors);
    }

    public void setElevatorCount(int count) {
        if (count < MIN_ELEVATORS || count > MAX_ELEVATORS) {
            Logger.error(DISPATCHER, "Invalid elevator count: " + count +
                        " (must be " + MIN_ELEVATORS + "-" + MAX_ELEVATORS + ")");
            return;
        }
        this.elevatorCount = count;
        Logger.info(CONFIG, "Elevator count set to " + count);
    }

    public void setElevatorsSpeed(int speed) {
        Elevator.setSpeed(speed);
    }

    public void setDoorsSpeed(int period) {
        Elevator.setOpenedDoorsPeriod(period);
    }

    public Command.Mode getCommandMode() {
        return commandMode;
    }

    public void setCommandMode(Command.Mode mode) {
        this.commandMode = mode;
        Logger.info(CONFIG, "Command mode set to " + mode);
    }

    public boolean isRunning() {
        return isRunning;
    }

    // ИНИЦИАЛИЗАЦИЯ ЛИФТОВ
    public void initializeElevators() {
        if (isRunning) {
            Logger.warning(DISPATCHER, "Elevators already running");
            return;
        }

        if (elevatorCount <= 0) {
            Logger.error(DISPATCHER, "Elevator count not set");
            return;
        }

        Logger.dispatcher("Initializing " + elevatorCount + " elevators...");

        for (int i = 1; i <= elevatorCount; i++) {
            Elevator elevator = new Elevator(i);
            elevators.add(elevator);

            Thread thread = new Thread(elevator, "Elevator-" + i);
            thread.setDaemon(false); // Не daemon - ждём завершения
            elevatorThreads.add(thread);
            thread.start();
        }

        isRunning = true;
        Logger.dispatcher("All " + elevatorCount + " elevators initialized and running");
    }

    // ОБРАБОТКА КОМАНД
    public void dispatch(Command command) {
        // Валидация команды
        if (command == null) {
            Logger.error(DISPATCHER, "Received null command");
            return;
        }

        if (!command.isValid()) {
            Logger.error(DISPATCHER, "Cannot dispatch invalid command: " +
                        command.getValidationError());
            return;
        }

        // Проверка состояния системы
        if (!isRunning) {
            Logger.error(DISPATCHER, "System not running");
            return;
        }

        if (elevators.isEmpty()) {
            Logger.error(DISPATCHER, "No elevators available");
            return;
        }

        // Валидация этажей относительно настроек здания
        if (command.getCallFloor() > maxFloors) {
            Logger.error(DISPATCHER, "Call floor " + command.getCallFloor() +
                        " exceeds max floors (" + maxFloors + ")");
            return;
        }

        if (command.getMode() == Command.Mode.INTERNAL &&
            command.getTargetFloor() > maxFloors) {
            Logger.error(DISPATCHER, "Target floor " + command.getTargetFloor() +
                        " exceeds max floors (" + maxFloors + ")");
            return;
        }

        Logger.dispatcher("Received request: " + command);

        // Выбор оптимального лифта
        Elevator selectedElevator = selectOptimalElevator(command);

        if (selectedElevator != null) {
            selectedElevator.addCommand(command);
            Logger.dispatcher("Assigned to Elevator №" + selectedElevator.getID());
        } else {
            Logger.error(DISPATCHER, "Failed to select elevator");
        }
    }

    // Выбор оптимального лифта
    private Elevator selectOptimalElevator(Command command) {
        if (elevators.size() == 1) {
            return elevators.peek();
        }

        int requestFloor = command.getCallFloor();
        boolean requestDirection = command.getDirection();

        Elevator bestElevator = null;
        int bestScore = Integer.MIN_VALUE;

        for (Elevator elevator : elevators) {
            int score = calculateScore(elevator, requestFloor, requestDirection);

            if (score > bestScore) {
                bestScore = score;
                bestElevator = elevator;
            }
        }

        return bestElevator;
    }

    // Расчёт счета лифта для запроса
    private int calculateScore(Elevator elevator, int requestFloor, boolean requestDirection) {
        int score = 0;

        // Штраф за количество задач в очереди
        int taskCount = elevator.getTaskCount();
        score -= taskCount * FINE_TASK_COUNT;

        // Большой бонус за свободный лифт
        if (elevator.getStatus() == Status.STOPPED && taskCount == 0 && !elevator.isProcessing()) {
            score += BONUS_IS_FREE_ELEVATOR;
        }

        // Штраф за расстояние
        int distance = elevator.calculateDistance(requestFloor);
        score -= distance * FINE_FOR_DISTANCE_PER_FLOOR;

        // Бонус если лифт движется в нужном направлении и запрос на пути
        if (elevator.isOnTheWay(requestFloor, requestDirection)) {
            score += BONUS_IS_ON_THE_WAY;
        }

        // Бонус если лифт на том же этаже
        if (elevator.getCurrentFloor() == requestFloor) {
            score += BONUS_IS_TRUE_FLOOR;
        }

        return score;
    }

    // ЗАВЕРШЕНИЕ РАБОТЫ СИСТЕМЫ
    public void shutdown() {
        if (!isRunning) {
            Logger.warning(DISPATCHER, "System not running");
            return;
        }

        Logger.dispatcher("Initiating shutdown sequence...");
        isRunning = false;

        // Прерываем все потоки лифтов
        for (Thread thread : elevatorThreads) {
            thread.interrupt();
        }

        // Ждём завершения потоков (максимум 3 секунды на каждый)
        for (Thread thread : elevatorThreads) {
            try {
                thread.join(3000);
                if (thread.isAlive()) {
                    Logger.warning(DISPATCHER, thread.getName() + " did not stop in time");
                }
            } catch (InterruptedException e) {
                Logger.error(DISPATCHER, "Interrupted while waiting for " + thread.getName());
                Thread.currentThread().interrupt();
            }
        }

        elevators.clear();
        elevatorThreads.clear();

        Logger.dispatcher("All elevators stopped. System shutdown complete.");
    }

    // СТАТИСТИКА
    // Общее количество задач во всех очередях
    public int getTotalTaskCount() {
        int total = 0;
        for (Elevator e : elevators) {
            total += e.getTaskCount();
        }
        return total;
    }

    // Количество свободных лифтов
    public int getIdleElevatorCount() {
        int count = 0;
        for (Elevator e : elevators) {
            if (e.getStatus() == Status.STOPPED && e.getTaskCount() == 0 && !e.isProcessing()) {
                count++;
            }
        }
        return count;
    }
}