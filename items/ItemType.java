package items;

import java.awt.Color;

// アイテムタイプ enum
public enum ItemType {
    HEALING_POTION("回復薬", new Color(255, 100, 100)),
    POWER_UP("パワーアップ", new Color(255, 200, 50)),
    MAX_HP_INCREASE("最大HP増加", new Color(100, 255, 100)),
    RICE_BALL("おにぎり", new Color(255, 255, 200)),
    TREASURE_CHEST("宝箱", new Color(255, 215, 0)),
    MAP("マップ", new Color(200, 200, 255)),
    STONE("石", new Color(150, 150, 150)),
    ARROW("矢", new Color(139, 69, 19));

    public final String name;
    public final Color color;

    private ItemType(String name, Color color) {
        this.name = name;
        this.color = color;
    }
}
