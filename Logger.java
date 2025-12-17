import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LOGGER - Централизованная система логирования
 * *
 * - Используется ReentrantLock, который гарантирует, что только ОДИН поток пишет в консоль
 *
 * - Когда пользователь вводит команду, логи не мешают, так как:
 * - inputMode = true - логи накапливаются в буфере
 * - inputMode = false - логи сразу выводятся в консоль
 * - После ввода все накопленные логи выводятся разом
 *
 * - Логи хорошо читаются, так как в консоли используются ANSI коды для цветов
 */
public class Logger implements Constants {

    // ПЕРЕМЕННЫЕ СОСТОЯНИЯ
    private static final ReentrantLock lock = new ReentrantLock();
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
    private static volatile boolean inputMode = false;
    private static final StringBuilder buffer = new StringBuilder();

    // ФУНКЦИИ ДЛЯ УПРАВЛЕНИЯ РЕЖИМОМ ВВОДА
    // Включить режим ввода (перед вводом команды пользователем)
    public static void startInputMode() {
        lock.lock();
        try {
            inputMode = true;
        } finally {
            lock.unlock();
        }
    }

    // Выключить режим ввода (после ввода команды пользователем)
    public static void endInputMode() {
        lock.lock();
        try {
            inputMode = false;
            if (buffer.length() > 0) {
                System.out.print(buffer.toString());
                buffer.setLength(0);
            }
        } finally {
            lock.unlock();
        }
    }

    // УНИВЕРСАЛЬНАЯ ФУНКЦИЯ ЛОГИРОВАНИЯ
    private static void log(String level, String source, String message, String color) {
        lock.lock();
        try {
            String timestamp = LocalTime.now().format(timeFormatter);
            String logLine = String.format(LOG_FORMAT, GRAY, timestamp, RESET,
                    color, level, RESET, source, message
            );

            if (inputMode) {
                buffer.append(logLine);
            } else {
                System.out.print(logLine);
            }
        } finally {
            lock.unlock();
        }
    }

    // ПУБЛИЧНЫЕ МЕТОДЫ ЛОГИРОВАНИЯ
    public static void info(String source, String message) {
        log(INFO, source, message, BLUE);
    }

    public static void success(String source, String message) {
        log(SUCCESS, source, message, GREEN);
    }

    public static void error(String source, String message) {
        log(ERROR, source, message, RED);
    }

    public static void warning(String source, String message) {
        log(WARN, source, message, YELLOW);
    }

    public static void elevator(int elevatorId, String message) {
        log(ELEVATOR, "Elevator №" + elevatorId, message, CYAN);
    }

    public static void dispatcher(String message) {
        log(DISPATCHER, DISPATCHER, message, GREEN);
    }

    public static void system(String message) {
        log(SYSTEM, SYSTEM, message, BLUE);
    }

    public static void input(String message) {
        log(INPUT, INPUT, message, MAGENTA);
    }

    // МЕТОДЫ ДЛЯ ВВОДА/ВЫВОДА БЕЗ ФОРМАТИРОВАНИЯ

    // Приглашение для ввода (без перевода строки)
    public static void prompt(String message) {
        lock.lock();
        try {
            if (!inputMode) {
                System.out.print(YELLOW + ">> " + RESET + message);
            }
        } finally {
            lock.unlock();
        }
    }

    // Простой вывод строки (без форматирования времени и уровня)
    public static void print(String message) {
        lock.lock();
        try {
            String line = "   " + message + System.lineSeparator();
            if (inputMode) {
                buffer.append(line);
            } else {
                System.out.print(line);
            }
        } finally {
            lock.unlock();
        }
    }

    // Вывод пустой строки
    public static void println() {
        lock.lock();
        try {
            if (inputMode) {
                buffer.append(System.lineSeparator());
            } else {
                System.out.println();
            }
        } finally {
            lock.unlock();
        }
    }

    // ДЕКОРАТИВНЫЕ ЭЛЕМЕНТЫ (для лучшей читаемости)
    public static void printSeparator() {
        lock.lock();
        try {
            String line = GRAY + SEPARATOR.repeat(70) + RESET + System.lineSeparator();
            if (inputMode) {
                buffer.append(line);
            } else {
                System.out.print(line);
            }
        } finally {
            lock.unlock();
        }
    }

    public static void printHeader(String title) {
        lock.lock();
        try {
            String line = System.lineSeparator() +
                    CYAN + "═".repeat(70) + RESET + System.lineSeparator() +
                    CYAN + " " + title + RESET + System.lineSeparator() +
                    CYAN + "═".repeat(70) + RESET + System.lineSeparator();
            if (inputMode) {
                buffer.append(line);
            } else {
                System.out.print(line);
            }
        } finally {
            lock.unlock();
        }
    }

    // Вывод меню
    public static void printMenu(String[] options) {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            for (String option : options) {
                sb.append("   ").append(WHITE).append(option).append(RESET).append(System.lineSeparator());
            }
            if (inputMode) {
                buffer.append(sb);
            } else {
                System.out.print(sb);
            }
        } finally {
            lock.unlock();
        }
    }
}
