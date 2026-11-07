package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.io.File;

public final class GameScreens {
    private GameScreens() {
    }

    public static void drawTitle(Graphics g, int width, int height, int menuCursor, String saveFilePath) {
        g.setColor(new Color(20, 20, 40));
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(255, 215, 0));
        g.setFont(new Font("MS Gothic", Font.BOLD, 36));
        String title = "ローグライク ダンジョン";
        FontMetrics fm = g.getFontMetrics();
        int titleX = (width - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 100);

        g.setColor(new Color(200, 200, 200));
        g.setFont(new Font("MS Gothic", Font.PLAIN, 18));
        String subtitle = "ダンジョン探索ゲーム";
        fm = g.getFontMetrics();
        int subtitleX = (width - fm.stringWidth(subtitle)) / 2;
        g.drawString(subtitle, subtitleX, 135);

        g.setFont(new Font("MS Gothic", Font.BOLD, 20));
        String[] menuItems = { "新しい冒険を始める", "セーブデータから再開", "操作説明" };
        int menuY = 220;
        int menuSpacing = 45;

        boolean hasSaveData = new File(saveFilePath).exists();

        for (int i = 0; i < menuItems.length; i++) {
            fm = g.getFontMetrics();
            int menuX = (width - fm.stringWidth(menuItems[i])) / 2;

            if (i == menuCursor) {
                g.setColor(new Color(255, 215, 0));
                g.fillRect(menuX - 30, menuY + i * menuSpacing - 18, 20, 20);
                g.setColor(Color.BLACK);
                g.drawString("▶", menuX - 28, menuY + i * menuSpacing - 2);
                g.setColor(Color.WHITE);
            } else {
                if (i == 1 && !hasSaveData) {
                    g.setColor(new Color(100, 100, 100));
                } else {
                    g.setColor(new Color(180, 180, 180));
                }
            }

            g.drawString(menuItems[i], menuX, menuY + i * menuSpacing);
        }

        g.setColor(new Color(150, 150, 150));
        g.setFont(new Font("MS Gothic", Font.PLAIN, 14));
        String hint = "↑↓: 選択  Enter: 決定";
        fm = g.getFontMetrics();
        int hintX = (width - fm.stringWidth(hint)) / 2;
        g.drawString(hint, hintX, 380);

        g.setFont(new Font("MS Gothic", Font.PLAIN, 11));
        g.setColor(new Color(120, 120, 120));
        g.drawString("Version 3.0", 10, height - 10);
    }

    public static void drawHelp(Graphics g, int width, int height) {
        g.setColor(new Color(20, 20, 40));
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(255, 215, 0));
        g.setFont(new Font("MS Gothic", Font.BOLD, 28));
        String title = "操作説明";
        FontMetrics fm = g.getFontMetrics();
        int titleX = (width - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 50);

        g.setColor(Color.WHITE);
        g.setFont(new Font("MS Gothic", Font.BOLD, 16));
        g.drawString("【基本操作】", 50, 90);

        g.setFont(new Font("MS Gothic", Font.PLAIN, 14));
        g.setColor(new Color(200, 200, 200));
        String[] basic = {
                "↑↓←→ キー ... プレイヤーの移動",
                "S キー ......... セーブ",
                "L キー ......... ロード（タイトル画面）",
                "I キー ......... インベントリ表示/非表示",
                "M キー ......... 全体マップ表示切替（マップ取得後）",
                "T キー ......... 投擲モード切替"
        };

        for (int i = 0; i < basic.length; i++) {
            g.drawString(basic[i], 60, 115 + i * 22);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("MS Gothic", Font.BOLD, 16));
        g.drawString("【ゲームシステム】", 50, 260);

        g.setFont(new Font("MS Gothic", Font.PLAIN, 14));
        g.setColor(new Color(200, 200, 200));
        String[] system = {
                "・階段を見つけて次のフロアへ進もう",
                "・敵に隣接すると自動的に攻撃",
                "・アイテムに重なると自動的に拾得",
                "・空腹度が0になるとダメージを受ける",
                "・経験値を稼いでレベルアップ"
        };

        for (int i = 0; i < system.length; i++) {
            g.drawString(system[i], 60, 285 + i * 22);
        }

        g.setColor(new Color(255, 200, 100));
        g.setFont(new Font("MS Gothic", Font.BOLD, 14));
        g.drawString("★ ショップ（青い床）: 3階以降出現、アイテム購入可能", 50, 410);
        g.drawString("★ モンスターハウス（赤い床）: 5階以降出現、大量の敵", 50, 433);
        g.drawString("★ ボス戦: 5階、10階、15階に強力なボスが待ち構える", 50, 456);

        g.setColor(new Color(150, 150, 150));
        g.setFont(new Font("MS Gothic", Font.PLAIN, 14));
        String hint = "Enter または ESC でタイトルに戻る";
        fm = g.getFontMetrics();
        int hintX = (width - fm.stringWidth(hint)) / 2;
        g.drawString(hint, hintX, 500);
    }
}
