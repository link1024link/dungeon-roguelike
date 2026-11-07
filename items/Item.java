package items;

// アイテムクラス
public class Item {
    public int x, y;
    public ItemType type;

    public Item(int x, int y, ItemType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public Item(ItemType type) {
        this.type = type;
        this.x = -1;
        this.y = -1;
    }

    public String getName() {
        return type.name;
    }
}
