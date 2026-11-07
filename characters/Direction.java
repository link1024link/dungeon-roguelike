package characters;

// 方向 enum
public enum Direction {
    DOWN(0, "下"),
    UP(1, "上"),
    LEFT(2, "左"),
    RIGHT(3, "右");

    public final int value;
    public final String name;

    private Direction(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public static Direction fromValue(int value) {
        for (Direction d : values()) {
            if (d.value == value)
                return d;
        }
        return DOWN;
    }
}
