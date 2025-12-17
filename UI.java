import java.util.Scanner;

// Пользовательский интерфейс
public class UI implements Constants {

    private final Dispatcher dispatcher;
    private final Scanner scanner;

    public UI(Scanner scanner, Dispatcher dispatcher) {
        this.scanner = scanner;
        this.dispatcher = dispatcher;
    }

    // ОТОБРАЖЕНИЕ ИНФОРМАЦИИ
    public void displayElevators() {
        if (dispatcher.getElevators().isEmpty()) {
            Logger.warning(UI, "No elevators in system");
            return;
        }

        Logger.printHeader(ELEVATOR_STATUS_HEADER);

        for (Elevator elevator : dispatcher.getElevators()) {
            Logger.print(elevator.toString());
        }

        Logger.printSeparator();
        Logger.print("Total tasks in queues: " + dispatcher.getTotalTaskCount());
        Logger.print("Idle elevators: " + dispatcher.getIdleElevatorCount());
        Logger.printSeparator();
    }

    public void printBuildingParameters() {
        Logger.printHeader(BUILDING_PARAMETERS_HEADER);
        Logger.print("Floors:         " + dispatcher.getMaxFloors());
        Logger.print("Elevators:      " + dispatcher.getElevators().size());
        Logger.print("Elevator speed: " + Elevator.getSpeed() + " s/floor");
        Logger.print("Door time:      " + Elevator.getOpenedDoorsPeriod() + " s");
        Logger.print("Command mode:   " + dispatcher.getCommandMode());
        Logger.print("System status:  " + (dispatcher.isRunning() ? "RUNNING" : "STOPPED"));
        Logger.printSeparator();
    }

    // ВВОД ДАННЫХ С ВАЛИДАЦИЕЙ
    // Получение валидного числа от пользователя
    private int getValidNumber(String prompt, int min, int max, int defaultValue) {
        while (true) {
            try {
                Logger.prompt(prompt + " [" + min + "-" + max + ", default: " + defaultValue + "]: ");

                Logger.startInputMode();
                String input = scanner.nextLine().trim();
                Logger.endInputMode();

                // Пустой ввод = значение по умолчанию
                if (input.isEmpty()) {
                    Logger.info(INPUT, "Using default: " + defaultValue);
                    return defaultValue;
                }

                int num = Integer.parseInt(input);

                if (num < min || num > max) {
                    Logger.error(INPUT, "Value must be between " + min + " and " + max);
                    continue;
                }

                return num;

            } catch (NumberFormatException e) {
                Logger.error(INPUT, "Invalid number format. Please enter a valid integer.");
            }
        }
    }

    // Получить направление от пользователя
    private Boolean getDirection() {
        Logger.prompt("Direction ('up' or 'down'): ");

        Logger.startInputMode();
        String input = scanner.nextLine().trim().toLowerCase();
        Logger.endInputMode();

        switch (input) {
            case UP_STR:
            case UP_STR_LOW:
                return UP;
            case DOWN_STR:
            case DOWN_STR_LOW:
                return DOWN;
            default:
                Logger.error(INPUT, "Invalid direction. Use 'up' or 'down'");
                return null;
        }
    }

    // ПОЛУЧЕНИЕ КОМАНДЫ
    public Command getCommand() {
        Command.Mode currentMode = dispatcher.getCommandMode();
        int maxFloors = dispatcher.getMaxFloors();

        Logger.printHeader(REQUEST_ELEVATOR_HEADER);
        Logger.print("Mode: " + currentMode);
        Logger.print("Building floors: 1-" + maxFloors);
        Logger.println();

        if (currentMode == Command.Mode.EXTERNAL) {
            return getExternalCommand(maxFloors);
        } else {
            return getInternalCommand(maxFloors);
        }
    }

    // EXTERNAL режим: этаж вызова + направление + целевой этаж
    private Command getExternalCommand(int maxFloors) {
        int callFloor = getValidNumber("Your current floor", MIN_FLOORS, maxFloors, 1);

        Boolean direction = getDirection();
        if (direction == null) {
            return new Command(-1, false); // Invalid command
        }

        // Проверка логики движения
        if (direction == UP && callFloor == maxFloors) {
            Logger.error(INPUT, "Cannot go UP from the top floor");
            return new Command(-1, false);
        }
        if (direction == DOWN && callFloor == MIN_FLOORS) {
            Logger.error(INPUT, "Cannot go DOWN from the bottom floor");
            return new Command(-1, false);
        }

        // Создаём команду с направлением
        Command command = new Command(callFloor, direction);

        // ВАЖНО: Сразу запрашиваем целевой этаж (пассажир знает, куда едет)
        Logger.println();
        Logger.print("Now specify your destination floor:");

        // Определяем допустимые этажи в зависимости от направления
        int minTarget, maxTarget, defaultTarget;
        if (direction == UP) {
            minTarget = callFloor + 1;
            maxTarget = maxFloors;
            defaultTarget = maxFloors;
        } else {
            minTarget = MIN_FLOORS;
            maxTarget = callFloor - 1;
            defaultTarget = MIN_FLOORS;
        }

        int targetFloor = getValidNumber("Your destination floor", minTarget, maxTarget, defaultTarget);

        // Проверка соответствия направлению
        boolean correctDirection = (direction == UP && targetFloor > callFloor) ||
                (direction == DOWN && targetFloor < callFloor);

        if (!correctDirection) {
            Logger.error(INPUT, "Destination floor doesn't match the selected direction");
            return new Command(-1, false);
        }

        // Устанавливаем целевой этаж
        command.setTargetFloor(targetFloor);

        if (!command.isValid()) {
            Logger.error(INPUT, "Failed to set destination: " + command.getValidationError());
            return new Command(-1, false);
        }

        return command;
    }

    // INTERNAL режим: этаж вызова + целевой этаж
    private Command getInternalCommand(int maxFloors) {
        int callFloor = getValidNumber("Your current floor", MIN_FLOORS, maxFloors, 1);

        // Определяем значение по умолчанию для целевого этажа
        int defaultTarget = (callFloor == maxFloors) ? 1 : maxFloors;

        int targetFloor = getValidNumber("Your destination floor", MIN_FLOORS, maxFloors, defaultTarget);

        if (callFloor == targetFloor) {
            Logger.error(INPUT, "Current floor and destination cannot be the same");
            return new Command(-1, -1);
        }

        return new Command(callFloor, targetFloor);
    }

    // НАСТРОЙКА ЗДАНИЯ
    public void buildingSetup() {
        Logger.printHeader(SYSTEM_SETUP_HEADER);
        Logger.print("Configure your building parameters");
        Logger.print("Press Enter to use default values");
        Logger.println();

        // Количество этажей
        int maxFloors = getValidNumber("Number of floors", MIN_FLOORS, MAX_FLOORS, DEFAULT_FLOORS);
        dispatcher.setMaxFloors(maxFloors);

        // Количество лифтов
        int elevatorCount = getValidNumber("Number of elevators", MIN_ELEVATORS, MAX_ELEVATORS, DEFAULT_ELEVATORS);
        dispatcher.setElevatorCount(elevatorCount);

        // Скорость лифтов
        int elevatorSpeed = getValidNumber("Elevator speed (seconds/floor)", MIN_SPEED, MAX_ELEVATOR_SPEED,
                DEFAULT_ELEVATOR_SPEED);
        dispatcher.setElevatorsSpeed(elevatorSpeed);

        // Время открытых дверей
        int doorsTime = getValidNumber("Door open time (seconds)", MIN_SPEED, MAX_DOOR_SPEED, DEFAULT_DOOR_SPEED);
        dispatcher.setDoorsSpeed(doorsTime);

        // Режим команд
        selectCommandMode();

        Logger.printSeparator();
        Logger.success(CONFIG, "Configuration completed!");
        Logger.printSeparator();

        // Инициализация лифтов
        dispatcher.initializeElevators();

        // Показываем справку
        printHelp();
    }

    // Выбор режима команд
    private void selectCommandMode() {
        Logger.println();
        Logger.print("Select Command Mode:");
        Logger.println();
        Logger.print("  1. " + EXTERNAL_MODE + " - Call elevator with direction, then specify destination");
        Logger.print("     (Realistic mode: press UP/DOWN button, then select floor inside)");
        Logger.println();
        Logger.print("  2. " + INTERNAL_MODE + " - Specify exact destination floor immediately");
        Logger.print("     (Simplified mode: good for testing)");
        Logger.println();

        while (true) {
            Logger.prompt("Select mode (1 or 2) [default: 2]: ");

            Logger.startInputMode();
            String input = scanner.nextLine().trim();
            Logger.endInputMode();

            if (input.isEmpty() || input.equals("2") || input.equalsIgnoreCase("internal")) {
                dispatcher.setCommandMode(Command.Mode.INTERNAL);
                Logger.success(CONFIG, "Command mode: " + INTERNAL_MODE);
                break;
            } else if (input.equals("1") || input.equalsIgnoreCase("external")) {
                dispatcher.setCommandMode(Command.Mode.EXTERNAL);
                Logger.success(CONFIG, "Command mode: " + EXTERNAL_MODE);
                break;
            } else {
                Logger.error(INPUT, "Invalid choice. Please enter 1 or 2");
            }
        }
    }

    // Вывод справки по командам
    public void printHelp() {
        Logger.printHeader(AVAILABLE_COMMANDS_HEADER);
        Logger.printMenu(new String[] {
                RUN + " - Request an elevator",
                LIST + " - Show all elevators status",
                INFO + " - Show building parameters",
                HELP + " - Show this help message",
                EXIT + " - Shutdown system"
        });
        Logger.printSeparator();
    }
}