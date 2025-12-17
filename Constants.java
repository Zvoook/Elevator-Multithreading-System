public interface Constants {
    // Ограничения этажей
    public static final int MIN_FLOORS = 1;
    public static final int MAX_FLOORS = 100;
    public static final int DEFAULT_FLOORS = MAX_FLOORS / 2;

    // Ограничения лифтов
    public static final int MIN_ELEVATORS = 1;
    public static final int MAX_ELEVATORS = 20;
    public static final int DEFAULT_ELEVATORS = MAX_ELEVATORS / 2;

    // Ограничения скорости
    public static final int MIN_SPEED = 1;
    public static final int MAX_ELEVATOR_SPEED = 50;
    public static final int DEFAULT_ELEVATOR_SPEED = MAX_ELEVATOR_SPEED / 2;
    public static final int MAX_DOOR_SPEED = 15;
    public static final int DEFAULT_DOOR_SPEED = MAX_DOOR_SPEED / 2;

    // Бонусы и штрафы диспетчера
    public static final int FINE_TASK_COUNT = 100;
    public static final int FINE_FOR_DISTANCE_PER_FLOOR = 5;
    public static final int BONUS_IS_FREE_ELEVATOR = 200;
    public static final int BONUS_IS_ON_THE_WAY = 150;
    public static final int BONUS_IS_TRUE_FLOOR = 50;

    // Направления движения
    public static final boolean UP = true;
    public static final boolean DOWN = false;

    // Специальные значения
    public static final int UNKNOWN_VALUE = -1;
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String LOG_FORMAT = "%s[%s]%s %s%-8s%s | %-12s | %s%n";
    public static final String SEPARATOR = "─";

    // ЦВЕТОВЫЕ ANSI КОДЫ (для форматирования вывода)
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String CYAN = "\u001B[36m";
    public static final String GRAY = "\u001B[90m";
    public static final String WHITE = "\u001B[97m";
    public static final String MAGENTA = "\u001B[35m";

    // Команды
    public static final String EXIT = "exit";
    public static final String RUN = "run";
    public static final String LIST = "list";
    public static final String STATUS = "status";
    public static final String HELP = "help";
    public static final String INFO = "info";

    // Уровни и источники логирования
    public static final String UI = "UI";
    public static final String INPUT = "INPUT";
    public static final String CONFIG = "CONFIG";
    public static final String SYSTEM = "SYSTEM";
    public static final String DISPATCHER = "DISPATCH";
    public static final String MAIN = "MAIN";
    public static final String ELEVATOR = "ELEVATOR";
    public static final String WARN = "WARN";
    public static final String SUCCESS = "SUCCESS";
    public static final String ERROR = "ERROR";

    // Моды команды
    public static final String INTERNAL_MODE = "INTERNAL";
    public static final String EXTERNAL_MODE = "EXTERNAL";

    // Строковое представление направлений
    public static final String UP_STR = "UP";
    public static final String UP_STR_LOW = "up";
    public static final String DOWN_STR = "DOWN";
    public static final String DOWN_STR_LOW = "down";

    // Заголовки для логов
    public static final String GENERAL_HEADER = "ELEVATOR CONTROL SYSTEM";
    public static final String SYSTEM_SETUP_HEADER = "ELEVATOR CONTROL SYSTEM SETUP";
    public static final String REQUEST_ELEVATOR_HEADER = "REQUEST ELEVATOR";
    public static final String BUILDING_PARAMETERS_HEADER = "BUILDING PARAMETERS";
    public static final String ELEVATOR_STATUS_HEADER = "ELEVATOR STATUS";
    public static final String AVAILABLE_COMMANDS_HEADER = "AVAILABLE COMMANDS";
}
