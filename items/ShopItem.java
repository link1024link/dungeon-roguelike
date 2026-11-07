package items;

// ショップアイテムクラス
public class ShopItem {
    public int x, y;
    public ItemType type; // アイテムタイプ
    public int price; // 価格

    public ShopItem(int x, int y, ItemType type, int price) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.price = price;
    }

    public String getName() {
        return type.name;
    }
}
