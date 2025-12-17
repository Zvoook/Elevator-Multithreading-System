import java.util.Scanner;

public class Main implements Constants {

    public static void main(String[] args) {
        Logger.printHeader(GENERAL_HEADER);
        Logger.print("Type 'help' for available commands");
        Logger.printSeparator();

        Scanner scanner = new Scanner(System.in);
        Dispatcher dispatcher = new Dispatcher();
        UI ui = new UI(scanner, dispatcher);

        // Настройка здания
        ui.buildingSetup();

        // Основной цикл обработки команд
        work(scanner, dispatcher, ui);

        // Завершение
        scanner.close();
        Logger.system("The END of simulation!");
        System.exit(0);
    }

    // Основной цикл обработки команд
    private static void work(Scanner scanner, Dispatcher dispatcher, UI ui) {
        while (true) {
            Logger.prompt("Enter command: ");

            Logger.startInputMode();
            String input = scanner.nextLine().toLowerCase().trim();
            Logger.endInputMode();

            // Пустой ввод - игнорируем
            if (input.isEmpty()) {
                continue;
            }

            // Обработка команд
            switch (input) {
                case EXIT:
                    dispatcher.shutdown();
                    break;

                case LIST:
                    ui.displayElevators();
                    break;

                case INFO:
                    ui.printBuildingParameters();
                    break;

                case RUN:
                    handleRunCommand(dispatcher, ui);
                    break;

                case HELP:
                    ui.printHelp();
                    break;

                default:
                    Logger.warning(MAIN, "Unknown command: ");
                    Logger.print("Type 'help' for available commands");
                    break;
            }
        }
    }

    // Обработка команды вызова лифта
    private static void handleRunCommand(Dispatcher dispatcher, UI ui) {
        if (!dispatcher.isRunning()) {
            Logger.error(MAIN, "System not running. Cannot process requests.");
            return;
        }

        Command command = ui.getCommand();

        if (command == null) {
            Logger.error(MAIN, "Failed to create command");
            return;
        }

        if (!command.isValid()) {
            Logger.error(MAIN, "Invalid command: " +
                        (command.getValidationError() != null ?
                         command.getValidationError() : "unknown error"));
            return;
        }

        dispatcher.dispatch(command);
    }
}
