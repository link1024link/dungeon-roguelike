package characters;

// ボスクラス（特別な敵）
public class Boss extends Enemy {
    public String name; // ボス名
    public int attackPattern = 0; // 攻撃パターン
    public int turnCounter = 0; // ターンカウンター
    public boolean isCharging = false; // 特殊技を溜めているか
    public int chargeCounter = 0; // 溜めカウンター

    public Boss(int x, int y, int floor) {
        super(x, y, floor, EnemyType.BOSS_LEVEL);

        // ボスは通常の敵より強力（弱体化）
        this.hp = 20 + floor * 5;
        this.maxHP = this.hp;
        this.attack = 4 + floor * 2;
        this.defense = 2 + floor / 2;
        this.expValue = 50 + floor * 15;

        // 階層に応じたボス名
        if (floor == 5) {
            this.name = "ゴブリンキング";
        } else if (floor == 10) {
            this.name = "ドラゴン";
        } else if (floor == 15) {
            this.name = "魔王";
        } else {
            this.name = "階層の守護者";
        }
    }

    // ボスの特殊攻撃を判定
    public boolean shouldUseSpecialAttack() {
        turnCounter++;
        // 3ターンごとに特殊攻撃
        return turnCounter % 3 == 0;
    }

    // HPが少ない時の強化判定
    public boolean isEnraged() {
        return hp < maxHP / 3;
    }
}
