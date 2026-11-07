package effects;

import java.awt.Color;

// ステータス効果の列挙型
public enum StatusEffect {
    NONE("正常", new Color(255, 255, 255)),
    POISON("毒", new Color(150, 0, 200)),
    CONFUSION("混乱", new Color(255, 200, 0)),
    SLEEP("睡眠", new Color(100, 100, 255));

    public final String name;
    public final Color color;

    StatusEffect(String name, Color color) {
        this.name = name;
        this.color = color;
    }
}
