package characters;

import effects.StatusEffect;

// 敵クラス
public class Enemy {
    public int x, y;
    public int hp, maxHP;
    public int attack;
    public int defense;
    public int intelligence;
    public int expValue;
    public EnemyType type;
    public Direction direction = Direction.DOWN;
    public StatusEffect status = StatusEffect.NONE;
    public int statusTurns = 0;

    public Enemy(int x, int y, int floor, EnemyType type) {
        this.x = x;
        this.y = y;
        this.type = type;

        // 5階層ごとの難易度倍率を計算（1.0 -> 1.3 -> 1.6 -> 1.9...）
        double difficultyMultiplier = 1.0 + (floor / 5) * 0.3;

        if (type == EnemyType.EXP_BONUS) { // 経験値ボーナスモンスター（詰み防止）
            this.hp = 2 + floor;
            this.attack = 1;
            this.defense = 0;
            this.expValue = 30 + floor * 8; // 通常の約3倍の経験値
        } else if (type == EnemyType.BOSS_LEVEL) { // ボス級（強い敵）
            this.hp = (int) ((12 + floor * 4) * difficultyMultiplier);
            this.attack = (int) ((3 + floor * 2) * difficultyMultiplier);
            this.defense = (int) ((2 + floor / 2) * difficultyMultiplier);
            this.expValue = 30 + floor * 10;
        } else if (type == EnemyType.STRONG) { // 強敵
            this.hp = (int) ((6 + floor * 2) * difficultyMultiplier * 0.9);
            this.attack = (int) ((2 + floor) * difficultyMultiplier * 0.9);
            this.defense = (int) ((1 + floor / 3) * difficultyMultiplier * 0.9);
            this.expValue = 12 + floor * 4;
        } else { // 通常
            this.hp = (int) ((3 + floor * 2) * difficultyMultiplier);
            this.attack = (int) ((1 + floor) * difficultyMultiplier);
            this.defense = (int) ((floor / 3) * difficultyMultiplier);
            this.expValue = 5 + floor * 2;
        }

        this.maxHP = this.hp;
        this.intelligence = 0;
    }
}
