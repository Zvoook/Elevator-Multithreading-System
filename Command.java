public class Command implements Constants {

    public enum Mode {
        EXTERNAL, // Вызов с этажа + направление + целевой этаж (как в реальных лифтах)
        INTERNAL // Вызов с этажа на конкретный этаж (упрощённый режим)
    }

    private final Mode mode;
    private final int callFloor;
    private final boolean direction;
    private int targetFloor;
    private boolean isValid;
    private String validationError;

    // КОНСТРУКТОРЫ

    // EXTERNAL: вызов с этажа + направление (цель устанавливается позже)
    public Command(int callFloor, boolean direction) {
        this.mode = Mode.EXTERNAL;
        this.callFloor = callFloor;
        this.direction = direction;
        this.targetFloor = UNKNOWN_VALUE;
        this.validationError = null;

        validate();
    }

    // INTERNAL: вызов с этажа на конкретный этаж
    public Command(int callFloor, int targetFloor) {
        this.mode = Mode.INTERNAL;
        this.callFloor = callFloor;
        this.targetFloor = targetFloor;
        this.direction = (targetFloor > callFloor);
        this.validationError = null;

        validate();
    }

    // ВАЛИДАЦИЯ
    private void validate() {
        this.isValid = true;

        // Проверка callFloor
        if (callFloor < MIN_FLOORS || callFloor > MAX_FLOORS) {
            this.isValid = false;
            this.validationError = "Call floor " + callFloor + " out of range [" +
                    MIN_FLOORS + "-" + MAX_FLOORS + "]";
            Logger.error("Command", validationError);
            return;
        }

        // Для INTERNAL режима - проверка targetFloor
        if (mode == Mode.INTERNAL) {
            if (targetFloor < MIN_FLOORS || targetFloor > MAX_FLOORS) {
                this.isValid = false;
                this.validationError = "Target floor " + targetFloor + " out of range [" +
                        MIN_FLOORS + "-" + MAX_FLOORS + "]";
                Logger.error("Command", validationError);
                return;
            }

            if (callFloor == targetFloor) {
                this.isValid = false;
                this.validationError = "Call floor and target floor cannot be the same (" + callFloor + ")";
                Logger.error("Command", validationError);
                return;
            }
        }

        // Для EXTERNAL режима - проверка логики направления
        if (mode == Mode.EXTERNAL) {
            // Нельзя ехать вверх с последнего этажа
            if (direction == UP && callFloor == MAX_FLOORS) {
                this.isValid = false;
                this.validationError = "Cannot go UP from top floor (" + MAX_FLOORS + ")";
                Logger.error("Command", validationError);
                return;
            }

            // Нельзя ехать вниз с первого этажа
            if (direction == DOWN && callFloor == MIN_FLOORS) {
                this.isValid = false;
                this.validationError = "Cannot go DOWN from bottom floor (" + MIN_FLOORS + ")";
                Logger.error("Command", validationError);
                return;
            }
        }
    }

    // Установить целевой этаж (для EXTERNAL режима)
    public void setTargetFloor(int floor) {
        if (mode != Mode.EXTERNAL) {
            Logger.warning("Command", "Cannot set target floor for INTERNAL command");
            this.isValid = false;
            this.validationError = "Cannot modify INTERNAL command";
            return;
        }

        if (floor < MIN_FLOORS || floor > MAX_FLOORS) {
            Logger.error("Command", "Invalid target floor: " + floor +
                    " (must be " + MIN_FLOORS + "-" + MAX_FLOORS + ")");
            this.isValid = false;
            this.validationError = "Target floor out of range";
            return;
        }

        if (floor == callFloor) {
            Logger.error("Command", "Target floor cannot equal call floor (" + callFloor + ")");
            this.isValid = false;
            this.validationError = "Target equals call floor";
            return;
        }

        // Проверка соответствия направлению
        boolean correctDirection = (direction == UP && floor > callFloor) ||
                (direction == DOWN && floor < callFloor);

        if (!correctDirection) {
            String expectedDir = direction ? "above" : "below";
            Logger.error("Command", "Target floor " + floor + " must be " + expectedDir +
                    " call floor " + callFloor + " for " + (direction ? "UP" : "DOWN") + " direction");
            this.isValid = false;
            this.validationError = "Target doesn't match direction";
            return;
        }

        this.targetFloor = floor;
        this.isValid = true;
        this.validationError = null;
    }

    // ГЕТТЕРЫ
    public Mode getMode() {
        return mode;
    }

    public int getCallFloor() {
        return callFloor;
    }

    public int getFirstTarget() {
        return callFloor;
    }

    public boolean getDirection() {
        return direction;
    }

    public int getTargetFloor() {
        return targetFloor;
    }

    public int getSecondTarget() {
        return targetFloor;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getValidationError() {
        return validationError;
    }

    @Override
    public String toString() {
        String dir = direction ? UP_STR : DOWN_STR;
        String validity = isValid ? "" : " [INVALID]";

        if (mode == Mode.EXTERNAL) {
            if (targetFloor == UNKNOWN_VALUE) {
                return "Command {EXTERNAL, floor = " + callFloor + ", " + dir +
                        ", destination = UNKNOWN}" + validity;
            } else {
                return "Command {EXTERNAL, floor = " + callFloor + ", " + dir +
                        ", destination = " + targetFloor + "}" + validity;
            }
        } else {
            return "Command {INTERNAL, " + callFloor + " -> " + targetFloor + ", " + dir + "}" + validity;
        }
    }
}