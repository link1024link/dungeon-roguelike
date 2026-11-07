package characters;

import java.awt.Color;

// 敵タイプ enum
public enum EnemyType {
    NORMAL("スライム", new Color(100, 200, 100)),
    STRONG("ゴブリン", new Color(200, 100, 100)),
    BOSS_LEVEL("オーガ", new Color(200, 50, 50)),
    EXP_BONUS("メタルスライム", new Color(180, 180, 255));

    public final String name;
    public final Color color;

    private EnemyType(String name, Color color) {
        this.name = name;
        this.color = color;
    }
}
