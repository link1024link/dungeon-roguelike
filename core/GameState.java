package core;

// ゲーム状態 enum
public enum GameState {
    TITLE(0, "タイトル"),
    PLAYING(1, "ゲーム中"),
    HELP(2, "操作説明");

    public final int value;
    public final String name;

    private GameState(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public static GameState fromValue(int value) {
        for (GameState g : values()) {
            if (g.value == value) {
                return g;
            }
        }
        return TITLE;
    }
}
