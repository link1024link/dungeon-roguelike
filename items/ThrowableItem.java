package items;

// 投擲アイテムクラス
public class ThrowableItem {
    public ItemType type; // STONE または ARROW
    public int count;

    public ThrowableItem(ItemType type, int count) {
        this.type = type;
        this.count = count;
    }

    public String getName() {
        return type.name;
    }

    public int getDamage() {
        switch (type) {
            case STONE:
                return 5; // 石：固定5ダメージ
            case ARROW:
                return 8; // 矢：固定8ダメージ
            default:
                return 0;
        }
    }

    public int getRange() {
        switch (type) {
            case STONE:
                return 3; // 石：射程3
            case ARROW:
                return 5; // 矢：射程5
            default:
                return 0;
        }
    }
}
