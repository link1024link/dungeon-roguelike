import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import characters.Boss;
import characters.Direction;
import characters.Enemy;
import characters.EnemyType;
import core.ActionLog;
import core.GameState;
import effects.StatusEffect;
import items.Item;
import items.ItemType;
import items.ShopItem;
import items.ThrowableItem;
import traps.Trap;
import ui.GameScreens;

public class MoveGame_enemy extends JPanel implements KeyListener {
    // ========== 定数定義 ==========
    private static final int TILE_SIZE = 32; // 1マスあたりのピクセルサイズ
    private static final int MAP_COLS = 25; // マップの横幅（列数）25マス
    private static final int MAP_ROWS = 20; // マップの縦幅（行数）20マス
    private static final int VIEWPORT_COLS = 15; // 画面に表示する横幅（列数）
    private static final int VIEWPORT_ROWS = 10; // 画面に表示する縦幅（行数）
    private static final int LOG_WIDTH = 280; // 行動ログの幅（ピクセル）
    private static final int BASE_WIDTH = VIEWPORT_COLS * TILE_SIZE + LOG_WIDTH;
    private static final int GAME_AREA_HEIGHT = VIEWPORT_ROWS * TILE_SIZE + 60;
    private static final int BASE_HEIGHT = 520;
    private static final int TITLE_SCREEN_HEIGHT = 400;
    private static final int MAX_FLOOR = 50; // 最大階層（50階まで）

    // ========== マップデータ（2次元配列：20行×25列）==========
    private int[][] map = new int[MAP_ROWS][MAP_COLS]; // メインマップ配列（0=床、1=壁）

    // 事前定義マップ配列：3種類×20行×25列
    // [0]=1階用、[1]=ボス階用、[2]=50階用
    private int[][][] predefinedMaps = new int[3][MAP_ROWS][MAP_COLS];

    // 事前定義マップのビットマップ文字列（装飾オブジェクト情報用）
    private String[][] predefinedMapBitmaps = new String[3][];

    // 探索済みフラグ配列：各マスが探索済みかを記録（true=探索済み）
    private boolean[][] explored = new boolean[MAP_ROWS][MAP_COLS];

    // ショップエリアフラグ配列：各マスがショップエリアかを記録
    private boolean[][] isShop = new boolean[MAP_ROWS][MAP_COLS];

    // モンスターハウスフラグ配列：各マスがモンスターハウスかを記録
    private boolean[][] isMonsterHouse = new boolean[MAP_ROWS][MAP_COLS];

    // モンスターハウス発動済みフラグ（1フロアで1回のみ発動）
    private boolean monsterHouseTriggered = false;

    private int playerX = 2;
    private int playerY = 2;
    private int playerHP = 20;
    private int playerMaxHP = 20;
    private int playerAttack = 3;
    private int playerDefense = 1;
    private int playerLevel = 1;
    private int playerExp = 0;
    private int expToNextLevel = 10;
    private int hunger = 100;
    private int maxHunger = 100;
    private int viewRange = 5;
    private int playerGold = 0; // 所持金

    private int currentFloor = 1;
    private int stairsX = -1;
    private int stairsY = -1;

    private boolean hasMap = false;
    private boolean showFullMap = false;

    private Direction playerDirection = Direction.DOWN;
    private Image playerImageDown, playerImageUp, playerImageLeft, playerImageRight;

    // 敵の画像
    private Image enemyImageDown, enemyImageUp, enemyImageLeft, enemyImageRight; // 雑魚敵
    private Image dragonImage; // ドラゴン（強敵）
    private Image bossImageDown, bossImageUp, bossImageLeft, bossImageRight; // ボス

    // マップ画像
    private Image floorImage; // 床画像
    private Image wallImage; // 壁画像
    private Image stairsImage; // 階段画像
    private Image torchImage; // 燭台画像

    private GameState gameState = GameState.TITLE;
    private int menuCursor = 0; // タイトル画面のメニューカーソル (0: 新規, 1: ロード, 2: 操作説明)
    private static final String SAVE_FILE = "savegame.dat";

    private List<Item> items = new ArrayList<>();
    private List<Item> inventory = new ArrayList<>();
    private boolean showInventory = false;
    private int inventoryCursor = 0; // インベントリカーソル位置
    private List<Enemy> enemies = new ArrayList<>();
    private List<Trap> traps = new ArrayList<>();
    private List<ShopItem> shopItems = new ArrayList<>(); // ショップアイテム
    private Random random = new Random();
    private int turnCount = 0;
    private int moveCount = 0; // 空腹カウント用

    // 状態異常システム
    private StatusEffect playerStatus = StatusEffect.NONE;
    private int playerStatusTurns = 0;

    // 投擲システム
    private boolean throwMode = false;
    private List<ThrowableItem> throwables = new ArrayList<>();

    // レベルアップメッセージ（任意のキーで消去）
    private String levelUpMessage = null;

    // ボスシステム
    private Boss currentBoss = null; // 現在のボス
    private boolean isBossFloor = false; // ボスフロアかどうか
    private boolean[][] isBossRoom = new boolean[MAP_ROWS][MAP_COLS]; // ボス部屋エリア
    private boolean bossCharging = false; // ボスが特殊技を溜めているか

    // 装飾オブジェクト
    private List<Point> torches = new ArrayList<>(); // 燭台の座標リスト

    // 行動ログシステム
    private ActionLog actionLog = new ActionLog(15);

    public MoveGame_enemy() {
        setPreferredSize(new Dimension(BASE_WIDTH, BASE_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        loadImages();
        initPredefinedMaps();
        // タイトル画面からスタート
        gameState = GameState.TITLE;
    }

    private void loadImages() {
        try {
            // プレイヤー画像（画像フォルダから読み込み）
            playerImageDown = ImageIO.read(new File("画像/player_d.png"));
            playerImageUp = ImageIO.read(new File("画像/player_u.png"));
            playerImageLeft = ImageIO.read(new File("画像/player_l.png"));
            playerImageRight = ImageIO.read(new File("画像/player_r.png"));

            // 雑魚敵画像
            enemyImageDown = ImageIO.read(new File("画像/enemy_d.png"));
            enemyImageUp = ImageIO.read(new File("画像/enemy_u.png"));
            enemyImageLeft = ImageIO.read(new File("画像/enemy_l.png"));
            enemyImageRight = ImageIO.read(new File("画像/enemy_r.png"));

            // 敵画像
            dragonImage = ImageIO.read(new File("画像/dragon.png"));

            // ボス画像
            bossImageDown = ImageIO.read(new File("画像/boss_d.png"));
            bossImageUp = ImageIO.read(new File("画像/boss_u.png"));
            bossImageLeft = ImageIO.read(new File("画像/boss_l.png"));
            bossImageRight = ImageIO.read(new File("画像/boss_r.png"));

            // マップ画像
            floorImage = ImageIO.read(new File("画像/green.png"));
            wallImage = ImageIO.read(new File("画像/gray.png"));
            stairsImage = ImageIO.read(new File("画像/stairs.png"));

            // 装飾オブジェクト画像
            torchImage = ImageIO.read(new File("画像/torch.png"));
        } catch (IOException e) {
            // 画像が読み込めなくても続行（色で描画）
            System.out.println("画像ファイルが見つかりません。図形で描画します。");
        }
    }

    // ========== Enum変換ヘルパーメソッド ==========
    private ItemType intToItemType(int value) {
        ItemType[] types = ItemType.values();
        if (value >= 0 && value < types.length) {
            return types[value];
        }
        return ItemType.HEALING_POTION; // デフォルト
    }

    private EnemyType intToEnemyType(int value) {
        EnemyType[] types = EnemyType.values();
        if (value >= 0 && value < types.length) {
            return types[value];
        }
        return EnemyType.NORMAL; // デフォルト
    }

    // 事前定義マップの初期化
    // ビットマップ記号の意味:
    // '#' = 壁 (通行不可)
    // '.' = 床 (通行可能、アイテム・敵が配置される)
    // 'T' = 燭台 (通行可能、装飾オブジェクト)
    // map配列の数値定義:
    // 0 = 床 (通行可能)
    // 1 = 壁 (通行不可)
    // 注意: 階段、アイテム、敵、燭台はmap配列には含まれず、床(0)の位置にプログラムが自動配置します
    private void initPredefinedMaps() {
        // マップ0: 1階用
        initPredefinedMap(0, new String[] {
                "#########################",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#########################"
        });

        // マップ1: ボス階用（燭台配置例）
        initPredefinedMap(1, new String[] {
                "#########################",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#......T#.......#T......#",
                "#.......#.......#.......#",
                "#.......#.......#.......#",
                "#......T#.......#T......#",
                "#.......#.......#.......#",
                "#.......#.......#.......#",
                "#......T#.......#T......#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#########################"
        });

        // マップ2: 50階用
        initPredefinedMap(2, new String[] {
                "#########################",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#.......................#",
                "#########################"
        });
    }

    // ========== 事前定義マップの変換処理 ==========
    // ビットマップ文字列（'#'と'.'と'T'の配列）を数値配列に変換する
    // '#' = 壁(1), '.' = 床(0), 'T' = 燭台(0、床として扱う)
    //
    // 引数:
    // index: 格納先のインデックス（0=1階、1=ボス階、2=50階）
    // bitmap: 文字列配列（各要素が1行分、例："#...#"）
    private void initPredefinedMap(int index, String[] bitmap) {
        // ビットマップ文字列を保存（後で燭台位置を読み取るため）
        predefinedMapBitmaps[index] = bitmap;

        // 縦方向のループ：y=0からMAP_ROWS-1（19）まで、つまり20行分を処理
        for (int y = 0; y < MAP_ROWS && y < bitmap.length; y++) {
            String line = bitmap[y]; // y行目の文字列を取得（例："#.....#"）

            // 横方向のループ：x=0からMAP_COLS-1（24）まで、つまり25列分を処理
            for (int x = 0; x < MAP_COLS && x < line.length(); x++) {
                char c = line.charAt(x); // x列目の文字を取得（'#'、'.'、'T'）

                // 文字を数値に変換してpredefinedMaps配列に格納
                // '#'なら1（壁）、それ以外（'.'、'T'）なら0（床）
                predefinedMaps[index][y][x] = (c == '#') ? 1 : 0;
            }
        }
    }

    // 現在の階層が事前定義マップを使用する階層かどうか判別
    // ========== どのマップを使うか判定 ==========
    // 現在の階層に応じて、事前定義マップのインデックスを返す
    // 戻り値: 0=1階用、1=ボス階用、2=50階用、-1=ランダム生成
    private int getPredefinedMapIndex() {
        if (currentFloor == 1) {
            return 0; // 1階は事前定義マップ[0]を使用
        } else if (isBossFloor) {
            return 1; // ボス階は事前定義マップ[1]を使用
        } else if (currentFloor == MAX_FLOOR) {
            return 2; // 50階は事前定義マップ[2]を使用
        }
        return -1; // 上記以外はランダム生成（事前定義マップを使わない）
    }

    // 事前定義マップを使う階層かどうかを判定
    // 戻り値: true=事前定義マップを使う、false=ランダム生成
    private boolean isPredefinedFloor() {
        return getPredefinedMapIndex() >= 0;
    }

    // ========== フロア生成のメイン処理 ==========
    // 新しい階層のマップ、敵、アイテムなどを全て生成する
    private void generateFloor() {
        // ステップ0: フロア固有のデータをクリア
        torches.clear(); // 燭台リストをクリア
        monsterHouseTriggered = false; // モンスターハウス発動フラグをリセット
        shopItems.clear();
        for (int r = 0; r < MAP_ROWS; r++) {
            for (int c = 0; c < MAP_COLS; c++) {
                isShop[r][c] = false;
            }
        }

        // ステップ1: ボスフロア判定（5の倍数階層かチェック）
        // 5階、10階、15階...などはボスフロア
        isBossFloor = (currentFloor % 5 == 0);

        // ステップ2: どのマップを使うか判定
        // 1階、ボス階、50階は事前定義マップ、それ以外はランダム生成
        int mapIndex = getPredefinedMapIndex();

        // ステップ3: マップ生成（事前定義 or ランダム）
        if (mapIndex >= 0) {
            // ========== 事前定義マップを使用する場合 ==========
            loadPredefinedMap(mapIndex); // predefinedMaps[mapIndex]をmapにコピー（燭台も自動配置）

            if (isBossFloor) {
                // ボス階の事前定義マップの場合：プレイヤーと階段の位置を設定
                // プレイヤーを下部中央に配置
                playerX = MAP_COLS / 2;
                playerY = MAP_ROWS - 3;

                // 階段を上部中央に配置
                stairsX = MAP_COLS / 2;
                stairsY = 8; // ボス部屋内の上部

                // ボス部屋エリアをマーク
                for (int y = 6; y <= 14; y++) {
                    for (int x = 7; x <= 17; x++) {
                        if (map[y][x] == 0) { // 床の場合のみ
                            isBossRoom[y][x] = true;
                        }
                    }
                }
            } else {
                // 通常階の場合：階段を配置して周囲をクリア
                placeStairs();
                clearAroundPlayer(); // プレイヤー周囲3×3を床にする
                clearAroundStairs(); // 階段周囲3×3を床にする
            }
        } else {
            // ========== ランダム生成の場合 ==========
            int attempts = 0; // 再生成の試行回数
            boolean validMap = false; // 有効なマップが生成できたか

            // 最大10回まで再生成を試みる
            while (!validMap && attempts < 10) {
                initMap(); // マップを初期化（外周壁→部屋→壁を生成）

                if (isBossFloor) {
                    generateBossRoom(); // ボス部屋を生成
                } else {
                    placeStairs(); // 階段を配置

                    // プレイヤーと階段の周囲を空ける
                    clearAroundPlayer(); // プレイヤー周囲3×3を床にする
                    clearAroundStairs(); // 階段周囲3×3を床にする
                }

                // プレイヤー位置から階段まで到達可能かチェック
                if (isReachable(playerX, playerY, stairsX, stairsY)) {
                    validMap = true; // 到達可能なら生成成功
                } else {
                    attempts++; // 到達不可能なら再生成
                }
            }
        }

        if (isBossFloor) {
            // ボス部屋の場合は通常敵、ショップ、モンスターハウスなし
            placeBoss();
            placeItems(); // アイテムは少し配置
        } else {
            initEnemies();
            generateShop();
            generateMonsterHouse(); // モンスターハウスを生成
            placeItems(); // アイテムを配置
            placeTraps(); // 罠を配置
        }

        // ステップ4: 視界を更新（探索済みエリアをマーク）
        updateVisibility();
    }

    // ========== 事前定義マップの読み込み ==========
    // predefinedMaps配列から現在のmap配列にコピーし、装飾オブジェクトを配置する
    // 引数: index - コピー元のインデックス（0=1階、1=ボス階、2=50階）
    private void loadPredefinedMap(int index) {
        // マップデータをコピー
        for (int y = 0; y < MAP_ROWS; y++) {
            for (int x = 0; x < MAP_COLS; x++) {
                map[y][x] = predefinedMaps[index][y][x];
            }
        }

        // ビットマップから燭台位置を読み取る
        torches.clear();
        String[] bitmap = predefinedMapBitmaps[index];
        if (bitmap != null) {
            for (int y = 0; y < MAP_ROWS && y < bitmap.length; y++) {
                String line = bitmap[y];
                for (int x = 0; x < MAP_COLS && x < line.length(); x++) {
                    char c = line.charAt(x);
                    if (c == 'T') {
                        // 燭台位置を記録
                        torches.add(new Point(x, y));
                    }
                }
            }
        }
    }

    // ========== ランダムマップの生成 ==========
    // マップをランダムに生成する（外周壁→部屋→壁の順に配置）
    private void initMap() {
        // ========== ステップ1: 外周を壁で囲む ==========
        // 全マス（20行×25列）を走査
        for (int r = 0; r < MAP_ROWS; r++) { // r=0から19まで（縦20マス）
            for (int c = 0; c < MAP_COLS; c++) { // c=0から24まで（横25マス）
                // 各フラグを初期化
                explored[r][c] = false; // 未探索状態にリセット
                isShop[r][c] = false; // ショップエリアフラグをクリア
                isMonsterHouse[r][c] = false; // モンスターハウスフラグをクリア
                isBossRoom[r][c] = false; // ボス部屋フラグをクリア

                // 外周（r=0, r=19, c=0, c=24）は壁、内側は床
                if (r == 0 || r == MAP_ROWS - 1 || c == 0 || c == MAP_COLS - 1) {
                    map[r][c] = 1; // 外周を壁(1)に設定
                } else {
                    map[r][c] = 0; // 内側を床(0)に設定
                }
            }
        }

        // ========== ステップ2: ランダムに部屋を生成 ==========
        int numRooms = random.nextInt(4) + 3; // 3～6個の部屋を生成

        for (int i = 0; i < numRooms; i++) {
            // 部屋のサイズをランダムに決定
            int roomW = random.nextInt(5) + 4; // 幅4～8マス
            int roomH = random.nextInt(4) + 3; // 高さ3～6マス

            // 部屋の左上座標をランダムに決定
            int roomX = random.nextInt(MAP_COLS - roomW - 2) + 1;
            int roomY = random.nextInt(MAP_ROWS - roomH - 2) + 1;

            // 部屋の周囲に壁を配置（70%の確率）
            for (int y = roomY; y < roomY + roomH; y++) { // 部屋の縦範囲
                for (int x = roomX; x < roomX + roomW; x++) { // 部屋の横範囲
                    // 部屋の外周（上下左右の端）かチェック
                    if (x == roomX || x == roomX + roomW - 1 ||
                            y == roomY || y == roomY + roomH - 1) {
                        // 70%の確率で壁を配置（30%は開口部になる）
                        if (random.nextDouble() > 0.3) {
                            map[y][x] = 1; // 壁を配置
                        }
                    }
                }
            }
        }

        // ========== ステップ3: ランダムに直線壁を配置 ==========
        int numWalls = random.nextInt(5) + 3; // 3～7本の壁を生成

        for (int i = 0; i < numWalls; i++) {
            int wallLength = random.nextInt(7) + 3; // 壁の長さ3～9マス
            int startX = random.nextInt(MAP_COLS - 4) + 2; // 壁の開始X座標
            int startY = random.nextInt(MAP_ROWS - 4) + 2; // 壁の開始Y座標
            boolean horizontal = random.nextBoolean(); // true=横壁、false=縦壁

            // 壁を1マスずつ配置していく
            for (int j = 0; j < wallLength; j++) {
                // 横壁の場合：x座標を増やす、縦壁の場合：y座標を増やす
                int x = horizontal ? Math.min(startX + j, MAP_COLS - 2) : startX;
                int y = horizontal ? startY : Math.min(startY + j, MAP_ROWS - 2);

                // プレイヤーの初期位置には壁を配置しない
                if (!(x == playerX && y == playerY)) {
                    map[y][x] = 1; // 壁を配置
                }
            }
        }
    }

    // ========== プレイヤー周囲のクリア処理 ==========
    // プレイヤーの周囲3×3マスを床にして、初期位置で動けない状況を防ぐ
    private void clearAroundPlayer() {
        // 縦方向：-1, 0, +1（プレイヤーの上、中央、下）
        for (int dy = -1; dy <= 1; dy++) {
            // 横方向：-1, 0, +1（プレイヤーの左、中央、右）
            for (int dx = -1; dx <= 1; dx++) {
                int x = playerX + dx; // プレイヤーのX座標 ± 1
                int y = playerY + dy; // プレイヤーのY座標 ± 1

                // マップの範囲内かチェック（外周の壁は残す）
                if (x > 0 && x < MAP_COLS - 1 && y > 0 && y < MAP_ROWS - 1) {
                    map[y][x] = 0; // 床(0)に変更
                }
            }
        }
    }

    // ========== 階段周囲のクリア処理 ==========
    // 階段の周囲3×3マスを床にして、階段に到達できない状況を防ぐ
    private void clearAroundStairs() {
        // 縦方向：-1, 0, +1（階段の上、中央、下）
        for (int dy = -1; dy <= 1; dy++) {
            // 横方向：-1, 0, +1（階段の左、中央、右）
            for (int dx = -1; dx <= 1; dx++) {
                int x = stairsX + dx; // 階段のX座標 ± 1
                int y = stairsY + dy; // 階段のY座標 ± 1

                // マップの範囲内かチェック（外周の壁は残す）
                if (x > 0 && x < MAP_COLS - 1 && y > 0 && y < MAP_ROWS - 1) {
                    map[y][x] = 0; // 床(0)に変更
                }
            }
        }
    }

    // ボス部屋を生成
    private void generateBossRoom() {
        // マップ中央に大きなボス部屋を作成
        int roomW = 12;
        int roomH = 10;
        int roomX = (MAP_COLS - roomW) / 2;
        int roomY = (MAP_ROWS - roomH) / 2;

        // ボス部屋エリアを床にする
        for (int y = roomY; y < roomY + roomH; y++) {
            for (int x = roomX; x < roomX + roomW; x++) {
                map[y][x] = 0;
                isBossRoom[y][x] = true;
            }
        }

        // 部屋の周囲に壁を配置
        for (int y = roomY - 1; y <= roomY + roomH; y++) {
            for (int x = roomX - 1; x <= roomX + roomW; x++) {
                if (x == roomX - 1 || x == roomX + roomW ||
                        y == roomY - 1 || y == roomY + roomH) {
                    if (x >= 0 && x < MAP_COLS && y >= 0 && y < MAP_ROWS) {
                        map[y][x] = 1;
                    }
                }
            }
        }

        // 入り口を作成（下側中央）
        int entranceX = roomX + roomW / 2;
        int entranceY = roomY + roomH;
        map[entranceY][entranceX] = 0;

        // プレイヤーを入り口前に配置
        playerX = entranceX;
        playerY = entranceY + 1;

        // 階段をボス部屋中央奥に配置
        stairsX = roomX + roomW / 2;
        stairsY = roomY + 2;

        // ボス部屋に燭台を配置（ランダム生成時のみ）
        placeTorchesInBossRoom(roomX, roomY, roomW, roomH);
    }

    // ボスを配置
    private void placeBoss() {
        // ボス部屋中央にボスを配置
        int bossX = MAP_COLS / 2;
        int bossY = MAP_ROWS / 2;

        currentBoss = new Boss(bossX, bossY, currentFloor);
        enemies.clear();
        enemies.add(currentBoss);
    }

    // ボス部屋に燭台を配置（ランダム生成時のみ使用）
    private void placeTorchesInBossRoom(int roomX, int roomY, int roomW, int roomH) {
        // 部屋の四隅に燭台を配置
        torches.add(new Point(roomX + 1, roomY + 1)); // 左上
        torches.add(new Point(roomX + roomW - 2, roomY + 1)); // 右上
        torches.add(new Point(roomX + 1, roomY + roomH - 2)); // 左下
        torches.add(new Point(roomX + roomW - 2, roomY + roomH - 2)); // 右下

        // 左右の壁沿いに追加の燭台を配置
        int midY = roomY + roomH / 2;
        torches.add(new Point(roomX + 1, midY)); // 左壁中央
        torches.add(new Point(roomX + roomW - 2, midY)); // 右壁中央
    }

    private boolean isReachable(int startX, int startY, int goalX, int goalY) {
        // BFSで到達可能性をチェック
        boolean[][] visited = new boolean[MAP_ROWS][MAP_COLS];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        queue.offer(new int[] { startX, startY });
        visited[startY][startX] = true;

        int[][] directions = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int x = current[0];
            int y = current[1];

            if (x == goalX && y == goalY) {
                return true;
            }

            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];

                if (nx >= 0 && nx < MAP_COLS && ny >= 0 && ny < MAP_ROWS &&
                        !visited[ny][nx] && map[ny][nx] == 0) {
                    visited[ny][nx] = true;
                    queue.offer(new int[] { nx, ny });
                }
            }
        }

        return false;
    }

    private void updateVisibility() {
        for (int dy = -viewRange; dy <= viewRange; dy++) {
            for (int dx = -viewRange; dx <= viewRange; dx++) {
                int checkX = playerX + dx;
                int checkY = playerY + dy;

                if (checkX >= 0 && checkX < MAP_COLS && checkY >= 0 && checkY < MAP_ROWS) {
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance <= viewRange) {
                        explored[checkY][checkX] = true;
                        // 罠を発見
                        for (Trap trap : traps) {
                            if (trap.x == checkX && trap.y == checkY && distance <= 2) {
                                trap.visible = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private void placeStairs() {
        do {
            stairsX = random.nextInt(MAP_COLS - 4) + 2;
            stairsY = random.nextInt(MAP_ROWS - 4) + 2;
        } while (map[stairsY][stairsX] != 0 || (stairsX == playerX && stairsY == playerY) ||
                Math.abs(stairsX - playerX) + Math.abs(stairsY - playerY) < 10);
    }

    // ========== 敵の配置 ==========
    // 現在の階層に応じて敵を配置する（1階は敵なし）
    private void initEnemies() {
        enemies.clear(); // 既存の敵リストをクリア

        // ========== 1階の特殊処理：敵を配置しない ==========
        // チュートリアル的な役割で、安全にアイテムを集められる
        if (currentFloor == 1) {
            return; // 処理を終了（敵を配置せずに戻る）
        }

        // 敵の数を計算：3 + 階層×2（例：5階なら13体）
        int numEnemies = 3 + currentFloor * 2;

        // プレイヤーの適正レベルを計算（階層 / 2 + 1）
        int expectedLevel = currentFloor / 2 + 1;
        // レベルが低すぎるかチェック（適正レベル-1未満）
        boolean isUnderleveled = playerLevel < expectedLevel - 1;

        // 詰み防止: レベルが低すぎる場合、経験値ボーナスモンスターを出現
        if (isUnderleveled && random.nextDouble() < 0.4) {
            int ex, ey;
            do {
                ex = random.nextInt(MAP_COLS - 4) + 2;
                ey = random.nextInt(MAP_ROWS - 4) + 2;
            } while (map[ey][ex] != 0 || (ex == playerX && ey == playerY) ||
                    (ex == stairsX && ey == stairsY) || isEnemyAt(ex, ey));

            enemies.add(new Enemy(ex, ey, currentFloor, EnemyType.EXP_BONUS)); // 経験値ボーナスモンスター
        }

        // ボス敵（5階ごと）
        if (currentFloor % 5 == 0) {
            int ex, ey;
            do {
                ex = random.nextInt(MAP_COLS - 4) + 2;
                ey = random.nextInt(MAP_ROWS - 4) + 2;
            } while (map[ey][ex] != 0 || (ex == playerX && ey == playerY) ||
                    (ex == stairsX && ey == stairsY) || isEnemyAt(ex, ey));

            enemies.add(new Enemy(ex, ey, currentFloor, EnemyType.BOSS_LEVEL));
        }

        // 強敵（30%の確率）
        int numStrongEnemies = random.nextInt(2) + 1;
        for (int i = 0; i < numStrongEnemies && i < numEnemies / 2; i++) {
            int ex, ey;
            do {
                ex = random.nextInt(MAP_COLS - 4) + 2;
                ey = random.nextInt(MAP_ROWS - 4) + 2;
            } while (map[ey][ex] != 0 || (ex == playerX && ey == playerY) ||
                    (ex == stairsX && ey == stairsY) || isEnemyAt(ex, ey));

            enemies.add(new Enemy(ex, ey, currentFloor, EnemyType.STRONG));
        }

        // 通常敵
        for (int i = enemies.size(); i < numEnemies; i++) {
            int ex, ey;
            do {
                ex = random.nextInt(MAP_COLS - 4) + 2;
                ey = random.nextInt(MAP_ROWS - 4) + 2;
            } while (map[ey][ex] != 0 || (ex == playerX && ey == playerY) ||
                    (ex == stairsX && ey == stairsY) || isEnemyAt(ex, ey));

            enemies.add(new Enemy(ex, ey, currentFloor, EnemyType.NORMAL));
        }
    }

    // ========== アイテムの配置 ==========
    // マップにアイテムを配置する（1階は固定アイテム）
    private void placeItems() {
        items.clear(); // 既存のアイテムリストをクリア

        // ========== 1階の特殊処理：固定アイテムを配置 ==========
        // 回復薬2個とおにぎり2個を必ず配置
        if (currentFloor == 1) {
            // 回復薬を2個配置
            for (int i = 0; i < 2; i++) {
                int ix, iy; // アイテムの座標
                // 床の位置をランダムに探す
                do {
                    ix = random.nextInt(MAP_COLS - 4) + 2; // X座標（2～22）
                    iy = random.nextInt(MAP_ROWS - 4) + 2; // Y座標（2～17）

                    // 以下の条件を全て満たすまでループ：
                    // - 床(0)である
                    // - プレイヤーの位置ではない
                    // - 階段の位置ではない
                    // - 既に他のアイテムがない
                } while (map[iy][ix] != 0 || (ix == playerX && iy == playerY) ||
                        (ix == stairsX && iy == stairsY) || isItemAt(ix, iy));

                // 回復薬を配置
                items.add(new Item(ix, iy, ItemType.HEALING_POTION));
            }

            // おにぎりを2個配置
            for (int i = 0; i < 2; i++) {
                int ix, iy; // アイテムの座標
                // 床の位置をランダムに探す
                do {
                    ix = random.nextInt(MAP_COLS - 4) + 2; // X座標（2～22）
                    iy = random.nextInt(MAP_ROWS - 4) + 2; // Y座標（2～17）

                    // 配置可能な位置が見つかるまでループ
                } while (map[iy][ix] != 0 || (ix == playerX && iy == playerY) ||
                        (ix == stairsX && iy == stairsY) || isItemAt(ix, iy));

                // おにぎりを配置
                items.add(new Item(ix, iy, ItemType.RICE_BALL));
            }
            return; // 1階の処理を終了（通常のランダム配置はしない）
        }

        int numItems = random.nextInt(4) + 2;

        // マップアイテムを20%の確率で配置
        if (!hasMap && random.nextDouble() < 0.2) {
            int ix, iy;
            do {
                ix = random.nextInt(MAP_COLS - 4) + 2;
                iy = random.nextInt(MAP_ROWS - 4) + 2;
            } while (map[iy][ix] != 0 || (ix == playerX && iy == playerY) ||
                    (ix == stairsX && iy == stairsY) || isEnemyAt(ix, iy) || isItemAt(ix, iy));

            items.add(new Item(ix, iy, ItemType.MAP)); // マップアイテム
        }

        for (int i = 0; i < numItems; i++) {
            int ix, iy;
            do {
                ix = random.nextInt(MAP_COLS - 4) + 2;
                iy = random.nextInt(MAP_ROWS - 4) + 2;
            } while (map[iy][ix] != 0 || (ix == playerX && iy == playerY) ||
                    (ix == stairsX && iy == stairsY) || isEnemyAt(ix, iy) || isItemAt(ix, iy));

            // アイテムタイプをランダムに決定
            int itemType = random.nextInt(100);
            ItemType type;
            if (itemType < 30)
                type = ItemType.HEALING_POTION; // 回復薬 30%
            else if (itemType < 45)
                type = ItemType.RICE_BALL; // おにぎり 15%
            else if (itemType < 60)
                type = ItemType.POWER_UP; // パワーアップ 15%
            else if (itemType < 73)
                type = ItemType.MAX_HP_INCREASE; // 最大HP増加 13%
            else if (itemType < 83)
                type = ItemType.TREASURE_CHEST; // 宝箱 10%
            else if (itemType < 93)
                type = ItemType.STONE; // 石 10%
            else
                type = ItemType.ARROW; // 矢 7%

            items.add(new Item(ix, iy, type));
        }
    }

    private void placeTraps() {
        traps.clear();
        int numTraps = random.nextInt(3) + 2;

        for (int i = 0; i < numTraps; i++) {
            int tx, ty;
            do {
                tx = random.nextInt(MAP_COLS - 4) + 2;
                ty = random.nextInt(MAP_ROWS - 4) + 2;
            } while (map[ty][tx] != 0 || (tx == playerX && ty == playerY) ||
                    (tx == stairsX && ty == stairsY) || isEnemyAt(tx, ty) ||
                    isItemAt(tx, ty) || isTrapAt(tx, ty));

            traps.add(new Trap(tx, ty));
        }
    }

    private boolean isTrapAt(int x, int y) {
        for (Trap trap : traps) {
            if (trap.x == x && trap.y == y) {
                return true;
            }
        }
        return false;
    }

    private boolean isItemAt(int x, int y) {
        for (Item item : items) {
            if (item.x == x && item.y == y) {
                return true;
            }
        }
        return false;
    }

    private void generateShop() {
        shopItems.clear();

        // 3階以降、30%の確率でショップ生成
        if (currentFloor < 3 || random.nextDouble() > 0.3) {
            return;
        }

        // ショップエリアを探す（5x5程度の空きスペース）
        int shopX = -1, shopY = -1;
        int attempts = 0;

        while (shopX == -1 && attempts < 50) {
            int sx = random.nextInt(MAP_COLS - 8) + 3;
            int sy = random.nextInt(MAP_ROWS - 7) + 3;

            boolean validArea = true;
            for (int y = sy; y < sy + 5 && validArea; y++) {
                for (int x = sx; x < sx + 5 && validArea; x++) {
                    if (map[y][x] != 0 || isEnemyAt(x, y) ||
                            Math.abs(x - playerX) + Math.abs(y - playerY) < 8) {
                        validArea = false;
                    }
                }
            }

            if (validArea) {
                shopX = sx;
                shopY = sy;
            }
            attempts++;
        }

        if (shopX == -1)
            return;

        // ショップエリアをマーク
        for (int y = shopY; y < shopY + 5; y++) {
            for (int x = shopX; x < shopX + 5; x++) {
                isShop[y][x] = true;
            }
        }

        // ショップアイテムを配置（4-6個）
        int numShopItems = random.nextInt(3) + 4;
        for (int i = 0; i < numShopItems; i++) {
            int sx = shopX + 1 + (i % 3);
            int sy = shopY + 1 + (i / 3);

            // アイテムタイプとベース価格
            int randValue = random.nextInt(8);
            ItemType itemType;
            int basePrice = 0;
            switch (randValue) {
                case 0:
                    itemType = ItemType.HEALING_POTION;
                    basePrice = 50;
                    break; // 回復薬
                case 1:
                    itemType = ItemType.POWER_UP;
                    basePrice = 100;
                    break; // パワーアップ
                case 2:
                    itemType = ItemType.MAX_HP_INCREASE;
                    basePrice = 150;
                    break; // 最大HP増加
                case 3:
                    itemType = ItemType.RICE_BALL;
                    basePrice = 30;
                    break; // おにぎり
                case 4:
                    itemType = ItemType.TREASURE_CHEST;
                    basePrice = 120;
                    break; // 防御UP
                case 5:
                    itemType = ItemType.MAP;
                    basePrice = 200;
                    break; // マップ
                case 6:
                    itemType = ItemType.STONE;
                    basePrice = 40;
                    break; // 石
                default:
                    itemType = ItemType.ARROW;
                    basePrice = 60;
                    break; // 矢
            }

            int price = basePrice + currentFloor * 10;
            shopItems.add(new ShopItem(sx, sy, itemType, price));
        }
    }

    private void generateMonsterHouse() {
        monsterHouseTriggered = false;

        // 5階以降、20%の確率でモンスターハウス生成
        if (currentFloor < 5 || random.nextDouble() > 0.2) {
            return;
        }

        // モンスターハウスエリアを探す（6x6程度の空きスペース）
        int mhX = -1, mhY = -1;
        int attempts = 0;

        while (mhX == -1 && attempts < 50) {
            int mx = random.nextInt(MAP_COLS - 9) + 3;
            int my = random.nextInt(MAP_ROWS - 9) + 3;

            boolean validArea = true;
            for (int y = my; y < my + 6 && validArea; y++) {
                for (int x = mx; x < mx + 6 && validArea; x++) {
                    if (map[y][x] != 0 || isEnemyAt(x, y) || isShop[y][x] ||
                            Math.abs(x - playerX) + Math.abs(y - playerY) < 10) {
                        validArea = false;
                    }
                }
            }

            if (validArea) {
                mhX = mx;
                mhY = my;
            }
            attempts++;
        }

        if (mhX == -1)
            return;

        // モンスターハウスエリアをマーク
        for (int y = mhY; y < mhY + 6; y++) {
            for (int x = mhX; x < mhX + 6; x++) {
                isMonsterHouse[y][x] = true;
            }
        }
    }

    private void triggerMonsterHouse() {
        if (monsterHouseTriggered)
            return;

        monsterHouseTriggered = true;
        JOptionPane.showMessageDialog(this, "モンスターハウスだ！");

        // プレイヤーの周囲に敵を大量配置
        int numMonsters = 5 + currentFloor;
        int spawned = 0;

        for (int dy = -3; dy <= 3 && spawned < numMonsters; dy++) {
            for (int dx = -3; dx <= 3 && spawned < numMonsters; dx++) {
                if (dx == 0 && dy == 0)
                    continue;

                int ex = playerX + dx;
                int ey = playerY + dy;

                if (ex > 0 && ex < MAP_COLS - 1 && ey > 0 && ey < MAP_ROWS - 1 &&
                        map[ey][ex] == 0 && !isEnemyAt(ex, ey) && isMonsterHouse[ey][ex]) {

                    EnemyType enemyType = random.nextBoolean() ? EnemyType.NORMAL : EnemyType.STRONG; // 通常か強敵
                    enemies.add(new Enemy(ex, ey, currentFloor, enemyType));
                    spawned++;
                }
            }
        }

        repaint();
    }

    private void buyShopItem(int x, int y) {
        for (Iterator<ShopItem> iter = shopItems.iterator(); iter.hasNext();) {
            ShopItem shopItem = iter.next();
            if (shopItem.x == x && shopItem.y == y) {
                if (playerGold >= shopItem.price) {
                    playerGold -= shopItem.price;

                    switch (shopItem.type) {
                        case POWER_UP: // パワーアップ
                            playerAttack += 2;
                            JOptionPane.showMessageDialog(this, shopItem.getName() + "を購入！ 攻撃力+2");
                            break;
                        case MAX_HP_INCREASE: // 最大HP増加
                            playerMaxHP += 10;
                            playerHP += 10;
                            JOptionPane.showMessageDialog(this, shopItem.getName() + "を購入！ 最大HP+10");
                            break;
                        case TREASURE_CHEST: // 防御UP
                            playerDefense += 2;
                            JOptionPane.showMessageDialog(this, shopItem.getName() + "を購入！ 防御力+2");
                            break;
                        case MAP: // マップ
                            hasMap = true;
                            JOptionPane.showMessageDialog(this, shopItem.getName() + "を購入！");
                            break;
                        case STONE:
                        case ARROW: // 投擲アイテム
                            boolean found = false;
                            for (ThrowableItem t : throwables) {
                                if (t.type == shopItem.type) {
                                    t.count += random.nextInt(3) + 4; // 4-6個追加
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                throwables.add(new ThrowableItem(shopItem.type, random.nextInt(3) + 4)); // 4-6個
                            }
                            JOptionPane.showMessageDialog(this, shopItem.getName() + "を購入！");
                            break;
                        case HEALING_POTION:
                        case RICE_BALL:
                            // インベントリに追加
                            inventory.add(new Item(shopItem.type));
                            JOptionPane.showMessageDialog(this, shopItem.getName() + "を購入！");
                            break;
                    }

                    iter.remove();
                } else {
                    JOptionPane.showMessageDialog(this, "お金が足りない！ 必要: " + shopItem.price + "G");
                }
                break;
            }
        }
    }

    private void enemyTurn() {
        processEnemyStatus(); // 状態異常処理

        for (Enemy enemy : enemies) {
            // 睡眠中はスキップ
            if (enemy.status == StatusEffect.SLEEP) {
                continue;
            }

            // ボスが溜め中は移動できない（攻撃のみ）
            boolean isBoss = enemy instanceof Boss;
            if (isBoss) {
                Boss boss = (Boss) enemy;
                if (boss.isCharging) {
                    // 溜め中は移動せず攻撃のみ可能
                    int dx = playerX - enemy.x;
                    int dy = playerY - enemy.y;
                    int distance = Math.abs(dx) + Math.abs(dy);
                    if (distance == 1) {
                        attackPlayer(enemy);
                    }
                    continue; // 溜め中は移動しない
                }
            }

            // 混乱中はランダム移動
            if (enemy.status == StatusEffect.CONFUSION) {
                int rx = enemy.x + (random.nextInt(3) - 1);
                int ry = enemy.y + (random.nextInt(3) - 1);
                if (canMoveTo(rx, ry) && !isEnemyAt(rx, ry) && (rx != playerX || ry != playerY)) {
                    int oldX = enemy.x;
                    int oldY = enemy.y;
                    enemy.x = rx;
                    enemy.y = ry;

                    // 向きを更新
                    if (enemy.x < oldX) {
                        enemy.direction = Direction.LEFT;
                    } else if (enemy.x > oldX) {
                        enemy.direction = Direction.RIGHT;
                    } else if (enemy.y < oldY) {
                        enemy.direction = Direction.UP;
                    } else if (enemy.y > oldY) {
                        enemy.direction = Direction.DOWN;
                    }
                }
                continue;
            }

            int dx = playerX - enemy.x;
            int dy = playerY - enemy.y;
            int distance = Math.abs(dx) + Math.abs(dy);

            // 経験値ボーナスモンスターはプレイヤーから逃げる
            if (enemy.type == EnemyType.EXP_BONUS) {
                if (distance <= 5) { // プレイヤーが近くにいる場合は逃げる
                    retreatFromPlayer(enemy, dx, dy);
                    // 移動後の罠チェック
                    enemyStepOnTrap(enemy);
                }
            } else if (distance == 1) {
                attackPlayer(enemy);
            } else {
                smartEnemyMove(enemy, dx, dy, distance);
                // 移動後の罠チェック
                enemyStepOnTrap(enemy);
            }
        }
    }

    private void smartEnemyMove(Enemy enemy, int dx, int dy, int distance) {
        double hpRatio = (double) enemy.hp / enemy.maxHP;

        if (hpRatio < 0.3) {
            retreatFromPlayer(enemy, dx, dy);
            return;
        }

        int nearbyEnemies = countNearbyEnemies(playerX, playerY);

        if (nearbyEnemies >= 1 && distance <= 3) {
            surroundPlayer(enemy, dx, dy);
        } else {
            advancedPathfinding(enemy, dx, dy);
        }
    }

    private void retreatFromPlayer(Enemy enemy, int dx, int dy) {
        int nx = enemy.x;
        int ny = enemy.y;

        if (Math.abs(dx) > Math.abs(dy)) {
            nx -= (dx > 0) ? 1 : -1;
        } else if (Math.abs(dy) > 0) {
            ny -= (dy > 0) ? 1 : -1;
        }

        if (!canMoveTo(nx, ny) || isEnemyAt(nx, ny)) {
            if (Math.abs(dy) > 0) {
                ny = enemy.y - ((dy > 0) ? 1 : -1);
            } else if (Math.abs(dx) > 0) {
                nx = enemy.x - ((dx > 0) ? 1 : -1);
            }
        }

        if (canMoveTo(nx, ny) && !isEnemyAt(nx, ny)) {
            int oldX = enemy.x;
            int oldY = enemy.y;
            enemy.x = nx;
            enemy.y = ny;

            // 向きを更新
            if (enemy.x < oldX) {
                enemy.direction = Direction.LEFT;
            } else if (enemy.x > oldX) {
                enemy.direction = Direction.RIGHT;
            } else if (enemy.y < oldY) {
                enemy.direction = Direction.UP;
            } else if (enemy.y > oldY) {
                enemy.direction = Direction.DOWN;
            }
        }
    }

    private int countNearbyEnemies(int x, int y) {
        int count = 0;
        for (Enemy e : enemies) {
            int dist = Math.abs(e.x - x) + Math.abs(e.y - y);
            if (dist <= 2)
                count++;
        }
        return count;
    }

    private void surroundPlayer(Enemy enemy, int dx, int dy) {
        int[][] positions = {
                { playerX - 1, playerY }, { playerX + 1, playerY },
                { playerX, playerY - 1 }, { playerX, playerY + 1 }
        };

        int bestX = enemy.x;
        int bestY = enemy.y;
        int minDist = Integer.MAX_VALUE;

        for (int[] pos : positions) {
            if (canMoveTo(pos[0], pos[1]) && !isEnemyAt(pos[0], pos[1])) {
                int dist = Math.abs(pos[0] - enemy.x) + Math.abs(pos[1] - enemy.y);
                if (dist < minDist) {
                    minDist = dist;
                    bestX = pos[0];
                    bestY = pos[1];
                }
            }
        }

        if (bestX != enemy.x || bestY != enemy.y) {
            int stepX = enemy.x;
            int stepY = enemy.y;

            if (Math.abs(bestX - enemy.x) > Math.abs(bestY - enemy.y)) {
                stepX += (bestX > enemy.x) ? 1 : -1;
            } else if (bestY != enemy.y) {
                stepY += (bestY > enemy.y) ? 1 : -1;
            }

            if (canMoveTo(stepX, stepY) && !isEnemyAt(stepX, stepY)) {
                int oldX = enemy.x;
                int oldY = enemy.y;
                enemy.x = stepX;
                enemy.y = stepY;

                // 向きを更新
                if (enemy.x < oldX) {
                    enemy.direction = Direction.LEFT;
                } else if (enemy.x > oldX) {
                    enemy.direction = Direction.RIGHT;
                } else if (enemy.y < oldY) {
                    enemy.direction = Direction.UP;
                } else if (enemy.y > oldY) {
                    enemy.direction = Direction.DOWN;
                }
                return;
            }
        }

        advancedPathfinding(enemy, dx, dy);
    }

    private void advancedPathfinding(Enemy enemy, int dx, int dy) {
        int[][] directions = new int[4][2];
        int[] scores = new int[4];

        directions[0] = new int[] { enemy.x - 1, enemy.y };
        directions[1] = new int[] { enemy.x + 1, enemy.y };
        directions[2] = new int[] { enemy.x, enemy.y - 1 };
        directions[3] = new int[] { enemy.x, enemy.y + 1 };

        for (int i = 0; i < 4; i++) {
            int nx = directions[i][0];
            int ny = directions[i][1];

            if (!canMoveTo(nx, ny) || isEnemyAt(nx, ny)) {
                scores[i] = -1000;
                continue;
            }

            int newDx = playerX - nx;
            int newDy = playerY - ny;
            int newDist = Math.abs(newDx) + Math.abs(newDy);
            int currentDist = Math.abs(dx) + Math.abs(dy);

            scores[i] = currentDist - newDist;

            if (nx == 1 || nx == MAP_COLS - 2 || ny == 1 || ny == MAP_ROWS - 2) {
                scores[i] -= 2;
            }

            int nearWalls = 0;
            if (nx > 0 && map[ny][nx - 1] == 1)
                nearWalls++;
            if (nx < MAP_COLS - 1 && map[ny][nx + 1] == 1)
                nearWalls++;
            if (ny > 0 && map[ny - 1][nx] == 1)
                nearWalls++;
            if (ny < MAP_ROWS - 1 && map[ny + 1][nx] == 1)
                nearWalls++;

            if (nearWalls >= 2)
                scores[i] += 1;
        }

        int bestDir = 0;
        for (int i = 1; i < 4; i++) {
            if (scores[i] > scores[bestDir]) {
                bestDir = i;
            }
        }

        if (scores[bestDir] > -1000) {
            int oldX = enemy.x;
            int oldY = enemy.y;
            enemy.x = directions[bestDir][0];
            enemy.y = directions[bestDir][1];

            // 向きを更新
            if (enemy.x < oldX) {
                enemy.direction = Direction.LEFT;
            } else if (enemy.x > oldX) {
                enemy.direction = Direction.RIGHT;
            } else if (enemy.y < oldY) {
                enemy.direction = Direction.UP;
            } else if (enemy.y > oldY) {
                enemy.direction = Direction.DOWN;
            }
        }
    }

    private void moveEnemyTowardPlayer(Enemy enemy, int dx, int dy) {
        advancedPathfinding(enemy, dx, dy);
    }

    private void attackPlayer(Enemy enemy) {
        int damage = Math.max(1, enemy.attack - playerDefense);

        // ボスの場合は特殊攻撃
        if (enemy instanceof Boss) {
            Boss boss = (Boss) enemy;

            if (boss.shouldUseSpecialAttack()) {
                if (!boss.isCharging) {
                    // 溜め開始
                    boss.isCharging = true;
                    boss.chargeCounter = 1;
                    bossCharging = true;
                    addLog(boss.name + "が力を溜めている...");
                    JOptionPane.showMessageDialog(this, boss.name + "が力を溜めている！\n次のターンで強力な攻撃が来る！");
                    return; // この溜めターンは攻撃しない
                } else {
                    // 溜め完了後の特殊攻撃
                    damage = (int) (damage * 2.0); // ダメージ2倍
                    addLog(boss.name + "の特殊攻撃！(" + damage + "ダメージ)");
                    JOptionPane.showMessageDialog(this, boss.name + "の特殊攻撃！");
                    boss.isCharging = false;
                    boss.chargeCounter = 0;
                    bossCharging = false;
                }
            }

            // 瀕死時は攻撃力上昇
            if (boss.isEnraged()) {
                damage = (int) (damage * 1.3);
            }
        }

        playerHP -= damage;
        addLog("敵の攻撃！" + damage + "ダメージ！");

        // ボス敵は20%の確率で混乱、強敵は15%で毒を与える（毒は5階層以降のみ）
        if (playerStatus == StatusEffect.NONE) {
            if (enemy.type == EnemyType.BOSS_LEVEL && random.nextDouble() < 0.2) {
                playerStatus = StatusEffect.CONFUSION;
                playerStatusTurns = 4;
                addLog("混乱状態になった！");
                JOptionPane.showMessageDialog(this, "混乱させられた！");
            } else if (enemy.type == EnemyType.STRONG && currentFloor >= 5 && random.nextDouble() < 0.15) {
                playerStatus = StatusEffect.POISON;
                playerStatusTurns = 5;
                JOptionPane.showMessageDialog(this, "毒を受けた！");
            }
        }

        if (playerHP <= 0) {
            playerHP = 0;
            JOptionPane.showMessageDialog(this, "ゲームオーバー！\n到達階層: " + currentFloor + "F\nターン数: " + turnCount);
            resetGame();
        }
    }

    private void attackEnemy(int ex, int ey) {
        for (Iterator<Enemy> iter = enemies.iterator(); iter.hasNext();) {
            Enemy enemy = iter.next();
            if (enemy.x == ex && enemy.y == ey) {
                int damage = Math.max(1, playerAttack - enemy.defense);
                enemy.hp -= damage;
                addLog("敵に" + damage + "ダメージ！");

                if (enemy.hp <= 0) {
                    // ボス撃破時
                    if (enemy instanceof Boss) {
                        Boss boss = (Boss) enemy;
                        addLog("★" + boss.name + "を撃破！");

                        // ボスは経験値とお金が多い
                        int goldGain = enemy.expValue * 2;
                        playerGold += goldGain;
                        addLog("経験値+" + enemy.expValue + " ゴールド+" + goldGain);
                        gainExp(enemy.expValue);

                        // ボス撃破報酬（ランダムアイテム）
                        playerMaxHP += 20;
                        playerHP += 20;
                        playerAttack += 3;
                        playerDefense += 2;
                        addLog("全ステータス大幅UP！");
                        addLog("HP+20 攻撃+3 防御+2");
                        addLog("階段が現れた！");

                        currentBoss = null;
                        bossCharging = false;
                    } else if (enemy.type == EnemyType.EXP_BONUS) {
                        // 経験値ボーナスモンスター撃破
                        addLog("★EXPボーナス撃破！(+" + enemy.expValue + "EXP)");
                        gainExp(enemy.expValue);
                        int goldGain = 20 + enemy.expValue;
                        playerGold += goldGain;
                        addLog("ゴールド+" + goldGain);
                    } else {
                        // 通常敵撃破
                        addLog("敵を倒した！(+" + enemy.expValue + "EXP)");
                        int goldGain = 10 + enemy.expValue;
                        playerGold += goldGain;
                        gainExp(enemy.expValue);
                    }

                    iter.remove();

                    if (enemies.isEmpty()) {
                        addLog("全ての敵を倒した！階段を探そう");
                    }
                }
                break;
            }
        }
    }

    private void gainExp(int exp) {
        playerExp += exp;
        while (playerExp >= expToNextLevel) {
            levelUp();
        }
    }

    private void levelUp() {
        playerLevel++;
        playerExp -= expToNextLevel;
        expToNextLevel = (int) (expToNextLevel * 1.5);

        int oldMaxHP = playerMaxHP;
        int oldAttack = playerAttack;
        int oldDefense = playerDefense;

        playerMaxHP += 5;
        playerHP += 5;
        playerAttack += 2;
        if (playerLevel % 2 == 0) {
            playerDefense += 1;
        }

        addLog("★レベルアップ！Lv" + playerLevel);

        // レベルアップメッセージを設定（任意のキーで消去可能）
        levelUpMessage = "レベルアップ！ Lv" + playerLevel + "\n" +
                "最大HP: " + oldMaxHP + " → " + playerMaxHP + "\n" +
                "攻撃力: " + oldAttack + " → " + playerAttack + "\n" +
                "防御力: " + oldDefense + " → " + playerDefense + "\n\n" +
                "[任意のキーを押してください]";
        repaint();
    }

    private void pickUpItem(int x, int y) {
        for (Iterator<Item> iter = items.iterator(); iter.hasNext();) {
            Item item = iter.next();
            if (item.x == x && item.y == y) {
                addLog(item.getName() + "を取得！");

                switch (item.type) {
                    case POWER_UP: // パワーアップ
                        playerAttack += 2;
                        addLog("攻撃力+2！");
                        break;
                    case MAX_HP_INCREASE: // 最大HP増加
                        playerMaxHP += 10;
                        playerHP += 10;
                        addLog("最大HP+10！");
                        break;
                    case TREASURE_CHEST: // 宝箱（防御力アップに変更）
                        playerDefense += 2;
                        addLog("防御力+2！");
                        break;
                    case MAP: // マップ
                        hasMap = true;
                        addLog("マップを取得！Mキーで表示");
                        break;
                    case STONE:
                    case ARROW: // 投擲アイテム（石、矢）
                        // 既に持っている場合は数を増やす
                        boolean found = false;
                        for (ThrowableItem t : throwables) {
                            if (t.type == item.type) {
                                t.count += random.nextInt(3) + 2; // 2-4個追加
                                addLog(t.getName() + "を" + (random.nextInt(3) + 2) + "個取得！");
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            int count = random.nextInt(3) + 3; // 3-5個
                            throwables.add(new ThrowableItem(item.type, count));
                            addLog(new ThrowableItem(item.type, count).getName() + "を" + count + "個取得！");
                        }
                        break;
                    case HEALING_POTION:
                    case RICE_BALL:
                        // インベントリに追加
                        inventory.add(new Item(item.type));
                        addLog("Iキーでインベントリ確認");
                        break;
                }
                iter.remove();
                break;
            }
        }
    }

    private void useItem(int index) {
        if (index < 0 || index >= inventory.size())
            return;

        Item item = inventory.get(index);

        switch (item.type) {
            case HEALING_POTION: // 回復薬
                int healAmount = 15;
                playerHP = Math.min(playerHP + healAmount, playerMaxHP);
                JOptionPane.showMessageDialog(this, "回復薬を使った！ HP+" + healAmount);
                break;
            case RICE_BALL: // おにぎり
                hunger = Math.min(hunger + 30, maxHunger);
                playerHP = Math.min(playerHP + 5, playerMaxHP);
                JOptionPane.showMessageDialog(this, "おにぎりを食べた！ 満腹度+30、HP+5");
                break;
            case POWER_UP:
            case MAX_HP_INCREASE:
            case TREASURE_CHEST:
            case MAP:
            case STONE:
            case ARROW:
                // これらは使用不可（拾った時に処理済み）
                JOptionPane.showMessageDialog(this, "このアイテムは使用できません");
                return;
        }

        inventory.remove(index);

        // アイテム使用は1ターン消費
        turnCount++;
        decreaseHunger();
        enemyTurn();
        repaint();
    }

    private void stepOnTrap(int x, int y) {
        for (Trap trap : traps) {
            if (trap.x == x && trap.y == y) {
                int damage = 5 + currentFloor;
                playerHP -= damage;
                addLog("罠だ！" + damage + "ダメージを受けた！");
                traps.remove(trap);
                if (playerHP <= 0) {
                    playerHP = 0;
                    JOptionPane.showMessageDialog(this, "ゲームオーバー！");
                    resetGame();
                }
                break;
            }
        }
    }

    // 敵が罠を踏んだ時の処理
    private void enemyStepOnTrap(Enemy enemy) {
        for (Iterator<Trap> iter = traps.iterator(); iter.hasNext();) {
            Trap trap = iter.next();
            if (trap.x == enemy.x && trap.y == enemy.y) {
                int damage = 3 + currentFloor / 2;
                enemy.hp -= damage;
                addLog("敵が罠を踏んだ！(" + damage + "ダメージ)");
                iter.remove();

                // 敵が倍れたら削除
                if (enemy.hp <= 0) {
                    addLog("敵が罠で倍れた！");
                    enemies.remove(enemy);
                }
                break;
            }
        }
    }

    private void decreaseHunger() {
        moveCount++;
        if (moveCount >= 4) {
            moveCount = 0;
            hunger--;
            if (hunger <= 0) {
                hunger = 0;
                playerHP -= 2;
                addLog("飢餓ダメージ！2HP減少");
                if (playerHP <= 0) {
                    playerHP = 0;
                    addLog("飢え死にした...");
                    JOptionPane.showMessageDialog(this, "飢え死にした...");
                    resetGame();
                }
            }
        }
    }

    private void processPlayerStatus() {
        if (playerStatus == StatusEffect.NONE)
            return;

        playerStatusTurns--;

        switch (playerStatus) {
            case POISON:
                // 毒ダメージは最大HPの3%（最低1）
                int poisonDamage = Math.max(1, (int) (playerMaxHP * 0.03));
                playerHP -= poisonDamage;
                addLog("毒ダメージ" + poisonDamage);
                if (playerHP <= 0) {
                    playerHP = 0;
                    addLog("毒で力尽きた...");
                    JOptionPane.showMessageDialog(this, "毒で力尽きた...");
                    resetGame();
                    return;
                }
                break;
            case SLEEP:
                // 睡眠中は何もできない（ターン処理でスキップ）
                break;
            case CONFUSION:
                // 混乱は特に処理なし（移動が反転するのは別の場所で処理）
                break;
            case NONE:
                // 何もしない
                break;
        }

        if (playerStatusTurns <= 0) {
            addLog(playerStatus.name + "状態が治った！");
            playerStatus = StatusEffect.NONE;
        }
    }

    private void processEnemyStatus() {
        for (Enemy enemy : enemies) {
            if (enemy.status == StatusEffect.NONE)
                continue;

            enemy.statusTurns--;

            switch (enemy.status) {
                case POISON:
                    enemy.hp -= 2;
                    if (enemy.hp <= 0) {
                        enemy.hp = 0;
                    }
                    break;
                case SLEEP:
                    // 睡眠中は行動不能（移動処理でチェック）
                    break;
                case CONFUSION:
                    // 混乱は移動がランダムになる（移動処理でチェック）
                    break;
                case NONE:
                    // 何もしない
                    break;
            }

            if (enemy.statusTurns <= 0) {
                enemy.status = StatusEffect.NONE;
            }
        }

        // HP0の敵を削除
        enemies.removeIf(e -> e.hp <= 0);
    }

    private void throwItem(int dx, int dy) {
        if (throwables.isEmpty()) {
            JOptionPane.showMessageDialog(this, "投げるものがない！");
            return;
        }

        // 最初の投擲アイテムを使用
        ThrowableItem throwable = throwables.get(0);
        int range = throwable.getRange();
        int damage = throwable.getDamage();

        // 投擲方向に飛ばす
        int tx = playerX;
        int ty = playerY;
        boolean hit = false;
        String hitMessage = "";

        for (int i = 0; i < range; i++) {
            tx += dx;
            ty += dy;

            // マップ外or壁に当たったら停止
            if (tx < 0 || tx >= MAP_COLS || ty < 0 || ty >= MAP_ROWS || map[ty][tx] == 1) {
                break;
            }

            // 敵に当たったかチェック
            for (Enemy enemy : enemies) {
                if (enemy.x == tx && enemy.y == ty) {
                    enemy.hp -= damage;

                    // 矢は毒効果を付与することがある（30%、5階層以降のみ）
                    if (throwable.type == ItemType.ARROW && currentFloor >= 5 && random.nextDouble() < 0.3) {
                        enemy.status = StatusEffect.POISON;
                        enemy.statusTurns = 5;
                        hitMessage = throwable.getName() + "が命中！ " + damage + "ダメージ！\n敵に毒を与えた！";
                    } else {
                        hitMessage = throwable.getName() + "が命中！ " + damage + "ダメージ！";
                    }

                    if (enemy.hp <= 0) {
                        int goldGain = 10 + enemy.expValue;
                        playerGold += goldGain;
                        gainExp(enemy.expValue);
                        enemies.remove(enemy);
                    }

                    hit = true;
                    break;
                }
            }

            if (hit)
                break;

            // 罠に当たったかチェック
            for (Iterator<Trap> trapIter = traps.iterator(); trapIter.hasNext();) {
                Trap trap = trapIter.next();
                if (trap.x == tx && trap.y == ty) {
                    trapIter.remove();
                    hit = true;
                    hitMessage = throwable.getName() + "で罠を破壊した！";
                    break;
                }
            }

            if (hit)
                break;
        }

        // 使用した投擲アイテムを減らす
        throwable.count--;
        if (throwable.count <= 0) {
            throwables.remove(0);
        }

        if (hit) {
            JOptionPane.showMessageDialog(this, hitMessage);
        } else {
            JOptionPane.showMessageDialog(this, throwable.getName() + "は外れた...");
        }

        throwMode = false;
    }

    private void goToNextFloor() {
        currentFloor++;
        addLog("=== " + currentFloor + "階に到達！ ===");

        // 階段を上ると空腹回復
        hunger = Math.min(maxHunger, hunger + 15);
        addLog("空腹が15回復した！");

        playerX = 2;
        playerY = 2;
        turnCount = 0;
        monsterHouseTriggered = false;
        currentBoss = null;
        isBossFloor = false;
        bossCharging = false;

        // ボス部屋フラグをクリア
        for (int r = 0; r < MAP_ROWS; r++) {
            for (int c = 0; c < MAP_COLS; c++) {
                isBossRoom[r][c] = false;
            }
        }

        generateFloor();

        if (isBossFloor) {
            addLog("★" + currentFloor + "階に到達！【ボスフロア】");
            addLog("強敵が待ち構えている...");
        } else {
            addLog("★" + currentFloor + "階に到達！");
        }

        repaint();
    }

    private boolean isEnemyAt(int x, int y) {
        for (Enemy enemy : enemies) {
            if (enemy.x == x && enemy.y == y) {
                return true;
            }
        }
        return false;
    }

    private Enemy getEnemyAt(int x, int y) {
        for (Enemy enemy : enemies) {
            if (enemy.x == x && enemy.y == y) {
                return enemy;
            }
        }
        return null;
    }

    // 行動ログを追加するメソッド
    private void addLog(String message) {
        actionLog.add(message);
        repaint();
    }

    private void resetGame() {
        playerX = 2;
        playerY = 2;
        playerHP = 20;
        playerMaxHP = 20;
        playerAttack = 3;
        playerDefense = 1;
        playerLevel = 1;
        playerExp = 0;
        expToNextLevel = 10;
        hunger = 100;
        viewRange = 5;
        playerGold = 0;
        currentFloor = 1;
        hasMap = false;
        showFullMap = false;
        playerDirection = Direction.DOWN;
        showInventory = false;
        moveCount = 0;
        turnCount = 0;
        monsterHouseTriggered = false;
        playerStatus = StatusEffect.NONE;
        playerStatusTurns = 0;
        throwMode = false;
        currentBoss = null;
        isBossFloor = false;
        bossCharging = false;
        enemies.clear();
        items.clear();
        inventory.clear();
        traps.clear();
        shopItems.clear();
        throwables.clear();
        torches.clear();
        actionLog.clear(); // ログをクリア
        addLog("新しい冒険を始めた！");

        // ボス部屋フラグをクリア
        for (int r = 0; r < MAP_ROWS; r++) {
            for (int c = 0; c < MAP_COLS; c++) {
                isBossRoom[r][c] = false;
            }
        }

        generateFloor();
        repaint();
    }

    private void saveGame() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SAVE_FILE))) {
            // プレイヤー情報
            writer.write(playerX + "," + playerY + "," + playerHP + "," + playerMaxHP + ",");
            writer.write(playerAttack + "," + playerDefense + "," + playerLevel + ",");
            writer.write(playerExp + "," + expToNextLevel + "," + hunger + "," + currentFloor + ",");
            writer.write(turnCount + "," + (hasMap ? 1 : 0) + "," + playerDirection.value + "," + moveCount + ","
                    + playerGold
                    + "\n");

            // 階段位置
            writer.write(stairsX + "," + stairsY + "\n");

            // マップ情報
            for (int r = 0; r < MAP_ROWS; r++) {
                for (int c = 0; c < MAP_COLS; c++) {
                    writer.write(map[r][c] + ",");
                }
                writer.write("\n");
            }

            // 探索情報
            for (int r = 0; r < MAP_ROWS; r++) {
                for (int c = 0; c < MAP_COLS; c++) {
                    writer.write((explored[r][c] ? 1 : 0) + ",");
                }
                writer.write("\n");
            }

            // 敵情報
            writer.write(enemies.size() + "\n");
            for (Enemy e : enemies) {
                writer.write(e.x + "," + e.y + "," + e.hp + "," + e.maxHP + ",");
                writer.write(e.attack + "," + e.defense + "," + e.intelligence + ",");
                writer.write(e.expValue + "," + e.type.ordinal() + "\n");
            }

            // アイテム情報
            writer.write(items.size() + "\n");
            for (Item i : items) {
                writer.write(i.x + "," + i.y + "," + i.type.ordinal() + "\n");
            }

            // 罠情報
            writer.write(traps.size() + "\n");
            for (Trap t : traps) {
                writer.write(t.x + "," + t.y + "," + (t.visible ? 1 : 0) + "\n");
            }

            // インベントリ情報
            writer.write(inventory.size() + "\n");
            for (Item i : inventory) {
                writer.write(i.type.ordinal() + "\n");
            }

            // ショップ情報
            writer.write(shopItems.size() + "\n");
            for (ShopItem si : shopItems) {
                writer.write(si.x + "," + si.y + "," + si.type.ordinal() + "," + si.price + "\n");
            }

            // ショップエリア情報
            for (int r = 0; r < MAP_ROWS; r++) {
                for (int c = 0; c < MAP_COLS; c++) {
                    writer.write((isShop[r][c] ? 1 : 0) + ",");
                }
                writer.write("\n");
            }

            // モンスターハウス情報
            writer.write((monsterHouseTriggered ? 1 : 0) + "\n");
            for (int r = 0; r < MAP_ROWS; r++) {
                for (int c = 0; c < MAP_COLS; c++) {
                    writer.write((isMonsterHouse[r][c] ? 1 : 0) + ",");
                }
                writer.write("\n");
            }

            JOptionPane.showMessageDialog(this, "セーブしました！");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "セーブに失敗しました: " + e.getMessage());
        }
    }

    private void loadGame() {
        File saveFile = new File(SAVE_FILE);
        if (!saveFile.exists()) {
            JOptionPane.showMessageDialog(this, "セーブデータが見つかりません");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(SAVE_FILE))) {
            // プレイヤー情報
            String[] playerData = reader.readLine().split(",");
            playerX = Integer.parseInt(playerData[0]);
            playerY = Integer.parseInt(playerData[1]);
            playerHP = Integer.parseInt(playerData[2]);
            playerMaxHP = Integer.parseInt(playerData[3]);
            playerAttack = Integer.parseInt(playerData[4]);
            playerDefense = Integer.parseInt(playerData[5]);
            playerLevel = Integer.parseInt(playerData[6]);
            playerExp = Integer.parseInt(playerData[7]);
            expToNextLevel = Integer.parseInt(playerData[8]);
            hunger = Integer.parseInt(playerData[9]);
            currentFloor = Integer.parseInt(playerData[10]);
            turnCount = Integer.parseInt(playerData[11]);
            hasMap = Integer.parseInt(playerData[12]) == 1;
            if (playerData.length > 13) {
                playerDirection = Direction.fromValue(Integer.parseInt(playerData[13]));
            } else {
                playerDirection = Direction.DOWN;
            }
            if (playerData.length > 14) {
                moveCount = Integer.parseInt(playerData[14]);
            } else {
                moveCount = 0;
            }
            if (playerData.length > 15) {
                playerGold = Integer.parseInt(playerData[15]);
            } else {
                playerGold = 0;
            }
            showFullMap = false;

            // 階段位置
            String[] stairsData = reader.readLine().split(",");
            stairsX = Integer.parseInt(stairsData[0]);
            stairsY = Integer.parseInt(stairsData[1]);

            // マップ情報
            for (int r = 0; r < MAP_ROWS; r++) {
                String[] rowData = reader.readLine().split(",");
                for (int c = 0; c < MAP_COLS; c++) {
                    map[r][c] = Integer.parseInt(rowData[c]);
                }
            }

            // 探索情報
            for (int r = 0; r < MAP_ROWS; r++) {
                String[] rowData = reader.readLine().split(",");
                for (int c = 0; c < MAP_COLS; c++) {
                    explored[r][c] = Integer.parseInt(rowData[c]) == 1;
                }
            }

            // 敵情報
            enemies.clear();
            int enemyCount = Integer.parseInt(reader.readLine());
            for (int i = 0; i < enemyCount; i++) {
                String[] enemyData = reader.readLine().split(",");
                Enemy e = new Enemy(0, 0, 1, EnemyType.NORMAL); // ダミー作成
                e.x = Integer.parseInt(enemyData[0]);
                e.y = Integer.parseInt(enemyData[1]);
                e.hp = Integer.parseInt(enemyData[2]);
                e.maxHP = Integer.parseInt(enemyData[3]);
                e.attack = Integer.parseInt(enemyData[4]);
                e.defense = Integer.parseInt(enemyData[5]);
                e.intelligence = Integer.parseInt(enemyData[6]);
                e.expValue = Integer.parseInt(enemyData[7]);
                e.type = intToEnemyType(Integer.parseInt(enemyData[8]));
                enemies.add(e);
            }

            // アイテム情報
            items.clear();
            int itemCount = Integer.parseInt(reader.readLine());
            for (int i = 0; i < itemCount; i++) {
                String[] itemData = reader.readLine().split(",");
                items.add(new Item(
                        Integer.parseInt(itemData[0]),
                        Integer.parseInt(itemData[1]),
                        intToItemType(Integer.parseInt(itemData[2]))));
            }

            // 罠情報
            traps.clear();
            int trapCount = Integer.parseInt(reader.readLine());
            for (int i = 0; i < trapCount; i++) {
                String[] trapData = reader.readLine().split(",");
                Trap t = new Trap(
                        Integer.parseInt(trapData[0]),
                        Integer.parseInt(trapData[1]));
                t.visible = Integer.parseInt(trapData[2]) == 1;
                traps.add(t);
            }

            // インベントリ情報
            inventory.clear();
            String invLine = reader.readLine();
            if (invLine != null) {
                int invCount = Integer.parseInt(invLine);
                for (int i = 0; i < invCount; i++) {
                    int itemType = Integer.parseInt(reader.readLine());
                    inventory.add(new Item(intToItemType(itemType)));
                }
            }

            // ショップ情報（新規追加データ - なければスキップ）
            shopItems.clear();
            String shopLine = reader.readLine();
            if (shopLine != null && !shopLine.isEmpty()) {
                int shopCount = Integer.parseInt(shopLine);
                for (int i = 0; i < shopCount; i++) {
                    String[] shopData = reader.readLine().split(",");
                    shopItems.add(new ShopItem(
                            Integer.parseInt(shopData[0]),
                            Integer.parseInt(shopData[1]),
                            intToItemType(Integer.parseInt(shopData[2])),
                            Integer.parseInt(shopData[3])));
                }

                // ショップエリア情報
                for (int r = 0; r < MAP_ROWS; r++) {
                    String[] rowData = reader.readLine().split(",");
                    for (int c = 0; c < MAP_COLS; c++) {
                        isShop[r][c] = Integer.parseInt(rowData[c]) == 1;
                    }
                }

                // モンスターハウス情報
                String mhLine = reader.readLine();
                if (mhLine != null) {
                    monsterHouseTriggered = Integer.parseInt(mhLine) == 1;
                    for (int r = 0; r < MAP_ROWS; r++) {
                        String[] rowData = reader.readLine().split(",");
                        for (int c = 0; c < MAP_COLS; c++) {
                            isMonsterHouse[r][c] = Integer.parseInt(rowData[c]) == 1;
                        }
                    }
                }
            }

            gameState = GameState.PLAYING;
            repaint();
            JOptionPane.showMessageDialog(this, "ロードしました！ 階層: " + currentFloor + "F");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "ロードに失敗しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        double scale = Math.min(getWidth() / (double) BASE_WIDTH, getHeight() / (double) BASE_HEIGHT);
        Graphics2D scaledGraphics = (Graphics2D) g.create();
        scaledGraphics.translate((getWidth() - BASE_WIDTH * scale) / 2,
                (getHeight() - BASE_HEIGHT * scale) / 2);
        scaledGraphics.scale(scale, scale);
        g = scaledGraphics;

        if (gameState == GameState.TITLE) {
            // タイトル画面を描画
            GameScreens.drawTitle(g, BASE_WIDTH, TITLE_SCREEN_HEIGHT, menuCursor, SAVE_FILE);
            scaledGraphics.dispose();
            return;
        }

        if (gameState == GameState.HELP) {
            // 操作説明画面を描画
            GameScreens.drawHelp(g, BASE_WIDTH, BASE_HEIGHT);
            scaledGraphics.dispose();
            return;
        }

        g.translate(0, (BASE_HEIGHT - GAME_AREA_HEIGHT) / 2);

        // ビューポートの計算
        int viewX = Math.max(0, Math.min(playerX - VIEWPORT_COLS / 2, MAP_COLS - VIEWPORT_COLS));
        int viewY = Math.max(0, Math.min(playerY - VIEWPORT_ROWS / 2, MAP_ROWS - VIEWPORT_ROWS));

        // マップとアイテムの描画
        for (int r = 0; r < VIEWPORT_ROWS; r++) {
            for (int c = 0; c < VIEWPORT_COLS; c++) {
                int mapX = viewX + c;
                int mapY = viewY + r;

                if (mapX >= MAP_COLS || mapY >= MAP_ROWS)
                    continue;

                // 探索済みかつ視界内かチェック
                boolean inView = isInView(mapX, mapY);
                boolean isExplored = explored[mapY][mapX];

                if (isExplored || hasMap) {
                    // マップタイル
                    if (map[mapY][mapX] == 1) {
                        // 壁
                        if (wallImage != null) {
                            g.drawImage(wallImage, c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE, this);
                            if (!inView) {
                                // 未探索でマップのみの場合はさらに暗く
                                int darkness = isExplored ? 150 : 200;
                                g.setColor(new Color(0, 0, 0, darkness));
                                g.fillRect(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                            }
                        } else {
                            g.setColor(inView ? new Color(60, 60, 60) : new Color(30, 30, 30));
                            g.fillRect(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        }
                    } else if (isBossRoom[mapY][mapX]) {
                        // ボス部屋エリア
                        g.setColor(inView ? new Color(180, 140, 0) : new Color(90, 70, 0));
                        g.fillRect(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    } else if (isShop[mapY][mapX]) {
                        // ショップエリア
                        g.setColor(inView ? new Color(100, 100, 200) : new Color(50, 50, 100));
                        g.fillRect(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    } else if (isMonsterHouse[mapY][mapX]) {
                        // モンスターハウスエリア
                        g.setColor(inView ? new Color(150, 0, 0) : new Color(75, 0, 0));
                        g.fillRect(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    } else {
                        // 床
                        if (floorImage != null) {
                            g.drawImage(floorImage, c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE, this);
                            if (!inView) {
                                // 未探索でマップのみの場合はさらに暗く
                                int darkness = isExplored ? 150 : 200;
                                g.setColor(new Color(0, 0, 0, darkness));
                                g.fillRect(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                            }
                        } else {
                            g.setColor(inView ? new Color(80, 160, 80) : new Color(40, 80, 40));
                            g.fillRect(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        }
                    }

                    // 階段（探索済みで視界内のみ）
                    if (isExplored && inView && mapX == stairsX && mapY == stairsY) {
                        int sx = c * TILE_SIZE;
                        int sy = r * TILE_SIZE;
                        if (stairsImage != null) {
                            // 階段画像を床の上に重ねて描画
                            g.drawImage(stairsImage, sx, sy, TILE_SIZE, TILE_SIZE, this);
                        } else {
                            // 画像がない場合は従来の三角形
                            g.setColor(new Color(255, 215, 0));
                            int[] xPoints = { sx + TILE_SIZE / 2, sx + 5, sx + TILE_SIZE - 5 };
                            int[] yPoints = { sy + 5, sy + TILE_SIZE - 5, sy + TILE_SIZE - 5 };
                            g.fillPolygon(xPoints, yPoints, 3);
                        }
                    }

                    // アイテム（探索済みで視界内のみ）
                    if (isExplored && inView) {
                        for (Item item : items) {
                            if (item.x == mapX && item.y == mapY) {
                                drawItem(g, c * TILE_SIZE, r * TILE_SIZE, item.type);
                            }
                        }

                        // ショップアイテム
                        for (ShopItem shopItem : shopItems) {
                            if (shopItem.x == mapX && shopItem.y == mapY) {
                                drawItem(g, c * TILE_SIZE, r * TILE_SIZE, shopItem.type);
                                // 価格表示
                                g.setColor(Color.YELLOW);
                                g.setFont(new Font("MS Gothic", Font.BOLD, 10));
                                g.drawString(shopItem.price + "G", c * TILE_SIZE + 2, r * TILE_SIZE + 30);
                            }
                        }
                    }

                    // 罠（探索済みで可視化されていて視界内）
                    if (isExplored && inView) {
                        for (Trap trap : traps) {
                            if (trap.x == mapX && trap.y == mapY && trap.visible) {
                                g.setColor(new Color(180, 0, 0));
                                int tx = c * TILE_SIZE;
                                int ty = r * TILE_SIZE;
                                g.fillRect(tx + 12, ty + 12, 8, 8);
                            }
                        }
                    }

                    // 燭台（探索済みで視界内のみ）
                    if (isExplored && inView) {
                        for (Point torch : torches) {
                            if (torch.x == mapX && torch.y == mapY) {
                                if (torchImage != null) {
                                    g.drawImage(torchImage, c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE, this);
                                } else {
                                    // 画像がない場合は簡易的に炎を描画
                                    g.setColor(new Color(255, 100, 0));
                                    g.fillOval(c * TILE_SIZE + 8, r * TILE_SIZE + 4, 16, 16);
                                    g.setColor(new Color(139, 69, 19));
                                    g.fillRect(c * TILE_SIZE + 12, r * TILE_SIZE + 16, 8, 12);
                                }
                            }
                        }
                    }
                } else {
                    // 未探索エリア（マップなし）は真っ黒
                    g.setColor(Color.BLACK);
                    g.fillRect(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }

                // タイル間のライン（太め）
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(0, 0, 0, 80));
                g2d.setStroke(new java.awt.BasicStroke(2));
                g2d.drawRect(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE - 1, TILE_SIZE - 1);
            }
        }

        // 敵の描画（視界内のみ）
        for (Enemy enemy : enemies) {
            if (isInView(enemy.x, enemy.y)) {
                int screenX = (enemy.x - viewX) * TILE_SIZE;
                int screenY = (enemy.y - viewY) * TILE_SIZE;

                // ボスは大きく描画
                if (enemy instanceof Boss) {
                    Boss boss = (Boss) enemy;

                    // ボスのオーラ（瀕死時は赤く、溜め中は黄色く）
                    if (boss.isCharging) {
                        g.setColor(new Color(255, 255, 0, 120));
                    } else if (boss.isEnraged()) {
                        g.setColor(new Color(255, 0, 0, 100));
                    } else {
                        g.setColor(new Color(180, 0, 180, 100));
                    }
                    g.fillOval(screenX - 4, screenY - 4, TILE_SIZE + 8, TILE_SIZE + 8);

                    // ボス画像（向きに応じて選択）
                    Image bossImage = null;
                    switch (boss.direction) {
                        case DOWN:
                            bossImage = bossImageDown;
                            break;
                        case UP:
                            bossImage = bossImageUp;
                            break;
                        case LEFT:
                            bossImage = bossImageLeft;
                            break;
                        case RIGHT:
                            bossImage = bossImageRight;
                            break;
                    }

                    if (bossImage != null) {
                        g.drawImage(bossImage, screenX, screenY, TILE_SIZE, TILE_SIZE, null);
                    } else {
                        g.setColor(new Color(180, 0, 180));
                        g.fillOval(screenX + 2, screenY + 2, TILE_SIZE - 4, TILE_SIZE - 4);
                    }

                    // ボス名表示
                    g.setColor(Color.YELLOW);
                    g.setFont(new Font("MS Gothic", Font.BOLD, 11));
                    int nameWidth = g.getFontMetrics().stringWidth(boss.name);
                    g.drawString(boss.name, screenX + (TILE_SIZE - nameWidth) / 2, screenY - 10);

                    // 溜め中の表示
                    if (boss.isCharging) {
                        g.setColor(Color.YELLOW);
                        g.setFont(new Font("MS Gothic", Font.BOLD, 10));
                        String chargeMsg = "溜中";
                        int msgWidth = g.getFontMetrics().stringWidth(chargeMsg);
                        g.drawString(chargeMsg, screenX + (TILE_SIZE - msgWidth) / 2, screenY + TILE_SIZE + 12);
                    }
                } else {
                    // 通常敵、強敵、経験値ボーナスモンスター
                    if (enemy.type == EnemyType.EXP_BONUS) { // 経験値ボーナスモンスター
                        // 金色に光るエフェクト
                        g.setColor(new Color(255, 215, 0, 150));
                        g.fillOval(screenX - 2, screenY - 2, TILE_SIZE + 4, TILE_SIZE + 4);

                        // 金色のスライム
                        g.setColor(new Color(255, 200, 0));
                        g.fillOval(screenX + 4, screenY + 4, TILE_SIZE - 8, TILE_SIZE - 8);

                        // キラキラマーク
                        g.setColor(Color.WHITE);
                        g.fillRect(screenX + TILE_SIZE / 2 - 1, screenY + 6, 2, 6);
                        g.fillRect(screenX + TILE_SIZE / 2 - 3, screenY + 8, 6, 2);

                        // 経験値ボーナス表示
                        g.setColor(Color.YELLOW);
                        g.setFont(new Font("MS Gothic", Font.BOLD, 9));
                        g.drawString("EXP", screenX + 4, screenY - 2);
                    } else if (enemy.type == EnemyType.STRONG) { // 強敵（ドラゴン）
                        if (dragonImage != null) {
                            g.drawImage(dragonImage, screenX, screenY, TILE_SIZE, TILE_SIZE, null);
                        } else {
                            g.setColor(new Color(220, 0, 0));
                            g.fillOval(screenX + 4, screenY + 4, TILE_SIZE - 8, TILE_SIZE - 8);
                        }
                    } else { // 通常敵（向きに応じて画像を選択）
                        Image enemyImage = null;
                        switch (enemy.direction) {
                            case DOWN:
                                enemyImage = enemyImageDown;
                                break;
                            case UP:
                                enemyImage = enemyImageUp;
                                break;
                            case LEFT:
                                enemyImage = enemyImageLeft;
                                break;
                            case RIGHT:
                                enemyImage = enemyImageRight;
                                break;
                        }

                        if (enemyImage != null) {
                            g.drawImage(enemyImage, screenX, screenY, TILE_SIZE, TILE_SIZE, null);
                        } else {
                            // 画像がない場合はデフォルトの青い円
                            g.setColor(new Color(70, 70, 220));
                            g.fillOval(screenX + 4, screenY + 4, TILE_SIZE - 8, TILE_SIZE - 8);
                        }
                    }
                }

                // HPバー
                g.setColor(Color.BLACK);
                g.fillRect(screenX + 2, screenY - 6, TILE_SIZE - 4, 4);
                g.setColor(Color.RED);
                int hpWidth = (int) ((TILE_SIZE - 4) * ((double) enemy.hp / enemy.maxHP));
                g.fillRect(screenX + 2, screenY - 6, hpWidth, 4);

                // 敵の状態異常表示
                if (enemy.status != StatusEffect.NONE) {
                    g.setColor(enemy.status.color);
                    g.fillOval(screenX + TILE_SIZE - 10, screenY + 2, 8, 8);
                }
            }
        }

        // プレイヤーの描画
        int playerScreenX = (playerX - viewX) * TILE_SIZE;
        int playerScreenY = (playerY - viewY) * TILE_SIZE;
        drawPlayer(g, playerScreenX, playerScreenY);

        // プレイヤーの状態異常表示
        if (playerStatus != StatusEffect.NONE) {
            g.setColor(playerStatus.color);
            g.fillOval(playerScreenX + TILE_SIZE - 10, playerScreenY + 2, 8, 8);
        }

        // ステータスバー
        int statusY = VIEWPORT_ROWS * TILE_SIZE + 5;
        g.setColor(Color.WHITE);
        g.setFont(new Font("MS Gothic", Font.BOLD, 12));

        // 状態異常表示
        String statusText = "";
        if (playerStatus != StatusEffect.NONE) {
            statusText = " [" + playerStatus.name + ":" + playerStatusTurns + "T]";
        }

        g.drawString("Lv:" + playerLevel + " HP:" + playerHP + "/" + playerMaxHP +
                " 攻:" + playerAttack + " 防:" + playerDefense + " 満腹度:" + hunger + " 所持金:" + playerGold + "G"
                + statusText, 10, statusY + 10);

        // 階層表示（ボスフロアの場合は強調）
        String floorText = "Exp:" + playerExp + "/" + expToNextLevel + " 階層:" + currentFloor + "F";
        if (isBossFloor) {
            floorText += " 【BOSS】";
        }
        floorText += " 敵:" + enemies.size();

        if (isBossFloor) {
            g.setColor(new Color(255, 215, 0)); // ボスフロアは金色
        }
        g.drawString(floorText, 10, statusY + 25);
        g.setColor(Color.WHITE);

        // HPバー
        g.setColor(Color.BLACK);
        g.fillRect(10, statusY + 30, 200, 8);
        g.setColor(Color.GREEN);
        int playerHpWidth = (int) (200 * ((double) playerHP / playerMaxHP));
        g.fillRect(10, statusY + 30, playerHpWidth, 8);

        // 満腹度バー
        g.setColor(Color.BLACK);
        g.fillRect(220, statusY + 30, 100, 8);
        g.setColor(new Color(255, 200, 0));
        int hungerWidth = (int) (100 * ((double) hunger / maxHunger));
        g.fillRect(220, statusY + 30, hungerWidth, 8);

        // 投擲アイテム残弾表示（ステータスバー右側）
        if (!throwables.isEmpty()) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("MS Gothic", Font.BOLD, 12));
            int throwX = 330;
            for (ThrowableItem t : throwables) {
                g.setColor(new Color(255, 200, 100));
                g.drawString(t.getName() + ":" + t.count, throwX, statusY + 10);
                throwX += 60;
            }
        }

        // 操作ガイド（画面下部）
        g.setColor(new Color(200, 200, 200));
        g.setFont(new Font("MS Gothic", Font.PLAIN, 11));
        g.drawString("[S]セーブ [I]アイテム [M]マップ [T]投擲モード", 10, statusY + 50);

        // 行動ログの描画（右側に表示）
        int logX = VIEWPORT_COLS * TILE_SIZE + 10;
        int logY = 10;
        g.setColor(new Color(40, 40, 60));
        g.fillRect(logX - 5, 5, LOG_WIDTH - 5, VIEWPORT_ROWS * TILE_SIZE + 50);

        g.setColor(new Color(255, 215, 0));
        g.setFont(new Font("MS Gothic", Font.BOLD, 14));
        g.drawString("行動ログ", logX, logY + 15);

        g.setColor(Color.WHITE);
        g.setFont(new Font("MS Gothic", Font.PLAIN, 11));
        int lineHeight = 18;
        for (int i = 0; i < actionLog.size(); i++) {
            String log = actionLog.get(i);
            // ログが長すぎる場合は省略
            if (log.length() > 28) {
                log = log.substring(0, 25) + "...";
            }
            g.drawString(log, logX, logY + 35 + i * lineHeight);
        }

        // ミニマップ（常時表示、Mキーで全体/探索済み切替）
        if (showFullMap) {
            drawMiniMap(g, true);
        } else {
            drawMiniMap(g, false);
        }

        // インベントリ表示
        if (showInventory) {
            drawInventory(g);
        }

        // 投擲モード表示
        if (throwMode) {
            g.setColor(new Color(255, 100, 100));
            g.setFont(new Font("MS Gothic", Font.BOLD, 16));
            g.drawString("【投擲モード】方向キーで投擲", 10, 20);

            if (!throwables.isEmpty()) {
                ThrowableItem t = throwables.get(0);
                g.drawString(t.getName() + " x" + t.count + " (ダメージ:" + t.getDamage() + " 射程:" + t.getRange() + ")", 10,
                        40);
            }
        }

        // レベルアップメッセージ表示
        if (levelUpMessage != null) {
            int msgWidth = 350;
            int msgHeight = 180;
            int msgX = (VIEWPORT_COLS * TILE_SIZE - msgWidth) / 2;
            int msgY = (VIEWPORT_ROWS * TILE_SIZE - msgHeight) / 2;

            // 背景
            g.setColor(new Color(0, 0, 0, 230));
            g.fillRect(msgX, msgY, msgWidth, msgHeight);
            g.setColor(new Color(255, 215, 0));
            g.fillRect(msgX, msgY, msgWidth, 3);
            g.fillRect(msgX, msgY + msgHeight - 3, msgWidth, 3);
            g.drawRect(msgX, msgY, msgWidth, msgHeight);

            // メッセージ
            g.setColor(Color.WHITE);
            g.setFont(new Font("MS Gothic", Font.BOLD, 16));
            String[] lines = levelUpMessage.split("\\n");
            int y = msgY + 30;
            for (String line : lines) {
                int textWidth = g.getFontMetrics().stringWidth(line);
                g.drawString(line, msgX + (msgWidth - textWidth) / 2, y);
                y += 25;
            }
        }
        scaledGraphics.dispose();
    }

    private void drawInventory(Graphics g) {
        int invWidth = 300;
        int invHeight = 250;
        int invX = (VIEWPORT_COLS * TILE_SIZE - invWidth) / 2;
        int invY = (VIEWPORT_ROWS * TILE_SIZE - invHeight) / 2;

        // 背景
        g.setColor(new Color(0, 0, 0, 220));
        g.fillRect(invX, invY, invWidth, invHeight);
        g.setColor(Color.WHITE);
        g.drawRect(invX, invY, invWidth, invHeight);

        // タイトル
        g.setFont(new Font("MS Gothic", Font.BOLD, 16));
        g.drawString("インベントリ", invX + 10, invY + 25);

        // アイテムリスト
        g.setFont(new Font("MS Gothic", Font.PLAIN, 14));
        if (inventory.isEmpty()) {
            g.drawString("アイテムがありません", invX + 20, invY + 60);
        } else {
            for (int i = 0; i < inventory.size(); i++) {
                Item item = inventory.get(i);

                // カーソル表示（矢印）
                if (i == inventoryCursor) {
                    g.setColor(Color.YELLOW);
                    g.drawString("→", invX + 5, invY + 60 + i * 25);
                    g.setColor(Color.WHITE);
                }

                String itemText = item.getName();
                g.drawString(itemText, invX + 30, invY + 60 + i * 25);
            }
        }

        // 説明
        g.setFont(new Font("MS Gothic", Font.PLAIN, 12));
        g.setColor(new Color(180, 180, 180));
        g.drawString("I/ESCキーで閉じる", invX + 10, invY + invHeight - 10);
    }

    private void drawMiniMap(Graphics g, boolean showAll) {
        int miniMapSize = 4; // 1マスあたりのピクセル数
        int miniMapWidth = MAP_COLS * miniMapSize;
        int miniMapHeight = MAP_ROWS * miniMapSize;
        int miniMapX = VIEWPORT_COLS * TILE_SIZE - miniMapWidth - 10;
        int miniMapY = 10;

        // 背景
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(miniMapX - 5, miniMapY - 5, miniMapWidth + 10, miniMapHeight + 10);
        g.setColor(Color.WHITE);
        g.drawRect(miniMapX - 5, miniMapY - 5, miniMapWidth + 10, miniMapHeight + 10);

        // マップ描画
        for (int r = 0; r < MAP_ROWS; r++) {
            for (int c = 0; c < MAP_COLS; c++) {
                int x = miniMapX + c * miniMapSize;
                int y = miniMapY + r * miniMapSize;

                // showAllがtrueまたは探索済みの場合に表示
                if (showAll || explored[r][c]) {
                    if (map[r][c] == 1) {
                        g.setColor(new Color(100, 100, 100));
                    } else {
                        g.setColor(new Color(50, 100, 50));
                    }
                    g.fillRect(x, y, miniMapSize, miniMapSize);
                } else {
                    g.setColor(new Color(20, 20, 20));
                    g.fillRect(x, y, miniMapSize, miniMapSize);
                }
            }
        }

        // 階段
        if (showAll || explored[stairsY][stairsX]) {
            int sx = miniMapX + stairsX * miniMapSize;
            int sy = miniMapY + stairsY * miniMapSize;
            g.setColor(new Color(255, 215, 0));
            g.fillRect(sx, sy, miniMapSize, miniMapSize);
        }

        // アイテム
        for (Item item : items) {
            if (showAll || explored[item.y][item.x]) {
                int ix = miniMapX + item.x * miniMapSize;
                int iy = miniMapY + item.y * miniMapSize;
                g.setColor(new Color(255, 100, 255));
                g.fillRect(ix, iy, miniMapSize, miniMapSize);
            }
        }

        // 敵
        for (Enemy enemy : enemies) {
            if (showAll || explored[enemy.y][enemy.x]) {
                int ex = miniMapX + enemy.x * miniMapSize;
                int ey = miniMapY + enemy.y * miniMapSize;
                if (enemy.type == EnemyType.BOSS_LEVEL) {
                    g.setColor(new Color(180, 0, 180));
                } else if (enemy.type == EnemyType.STRONG) {
                    g.setColor(new Color(220, 0, 0));
                } else {
                    g.setColor(new Color(70, 70, 220));
                }
                g.fillRect(ex, ey, miniMapSize, miniMapSize);
            }
        }

        // プレイヤー
        int px = miniMapX + playerX * miniMapSize;
        int py = miniMapY + playerY * miniMapSize;
        g.setColor(new Color(220, 70, 70));
        g.fillRect(px, py, miniMapSize, miniMapSize);

        // 説明文
        g.setColor(Color.WHITE);
        g.setFont(new Font("MS Gothic", Font.BOLD, 10));
        String mapLabel = showAll ? "マップ" : "マップ";
        g.drawString(mapLabel, miniMapX, miniMapY + miniMapHeight + 15);
    }

    private boolean isInView(int x, int y) {
        int dx = x - playerX;
        int dy = y - playerY;
        return Math.sqrt(dx * dx + dy * dy) <= viewRange;
    }

    private void drawPlayer(Graphics g, int x, int y) {
        Image playerImage = playerImageDown;

        switch (playerDirection) {
            case DOWN:
                playerImage = playerImageDown;
                break;
            case UP:
                playerImage = playerImageUp;
                break;
            case LEFT:
                playerImage = playerImageLeft;
                break;
            case RIGHT:
                playerImage = playerImageRight;
                break;
        }

        if (playerImage != null) {
            g.drawImage(playerImage, x, y, TILE_SIZE, TILE_SIZE, null);
        } else {
            g.setColor(new Color(220, 70, 70));
            g.fillOval(x + 4, y + 4, TILE_SIZE - 8, TILE_SIZE - 8);
        }
    }

    private void drawItem(Graphics g, int x, int y, ItemType type) {
        switch (type) {
            case HEALING_POTION: // 回復薬
                g.setColor(new Color(255, 100, 255));
                g.fillOval(x + 8, y + 8, TILE_SIZE - 16, TILE_SIZE - 16);
                g.setColor(Color.WHITE);
                g.fillRect(x + 14, y + 10, 4, 12);
                g.fillRect(x + 10, y + 14, 12, 4);
                break;
            case POWER_UP: // パワーアップ
                g.setColor(new Color(255, 0, 0));
                g.fillOval(x + 8, y + 8, TILE_SIZE - 16, TILE_SIZE - 16);
                g.setColor(Color.YELLOW);
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.drawString("P", x + 12, y + 20);
                break;
            case MAX_HP_INCREASE: // 最大HP増加
                g.setColor(new Color(0, 255, 0));
                g.fillOval(x + 8, y + 8, TILE_SIZE - 16, TILE_SIZE - 16);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.drawString("H", x + 12, y + 20);
                break;
            case RICE_BALL: // おにぎり
                g.setColor(Color.WHITE);
                int[] xPoints = { x + TILE_SIZE / 2, x + 8, x + 10, x + 22, x + 24 };
                int[] yPoints = { y + 10, y + 12, y + 22, y + 22, y + 12 };
                g.fillPolygon(xPoints, yPoints, 5);
                g.setColor(Color.BLACK);
                g.fillRect(x + 13, y + 15, 6, 3);
                break;
            case TREASURE_CHEST: // 宝箱
                g.setColor(new Color(139, 69, 19));
                g.fillRect(x + 8, y + 12, TILE_SIZE - 16, TILE_SIZE - 16);
                g.setColor(new Color(255, 215, 0));
                g.fillRect(x + 14, y + 14, 4, 6);
                break;
            case MAP: // マップ
                g.setColor(new Color(255, 255, 200));
                g.fillRect(x + 8, y + 10, TILE_SIZE - 16, TILE_SIZE - 18);
                g.setColor(new Color(100, 100, 100));
                g.drawRect(x + 8, y + 10, TILE_SIZE - 16, TILE_SIZE - 18);
                g.setColor(Color.BLACK);
                g.setFont(new Font("Arial", Font.BOLD, 10));
                g.drawString("M", x + 13, y + 21);
                break;
            case STONE: // 石
                g.setColor(new Color(150, 150, 150));
                g.fillOval(x + 10, y + 10, 12, 12);
                g.setColor(new Color(100, 100, 100));
                g.fillOval(x + 12, y + 11, 3, 3);
                break;
            case ARROW: // 矢
                g.setColor(new Color(139, 69, 19));
                g.fillRect(x + 10, y + 15, 12, 2);
                g.setColor(new Color(200, 200, 200));
                int[] arrowX = { x + 22, x + 26, x + 22 };
                int[] arrowY = { y + 12, y + 16, y + 20 };
                g.fillPolygon(arrowX, arrowY, 3);
                break;
        }
    }

    private boolean canMoveTo(int nx, int ny) {
        if (nx < 0 || ny < 0 || nx >= MAP_COLS || ny >= MAP_ROWS)
            return false;
        return map[ny][nx] == 0;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // タイトル画面での処理
        if (gameState == GameState.TITLE) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP: // カーソル上移動
                    menuCursor--;
                    if (menuCursor < 0)
                        menuCursor = 2;
                    repaint();
                    break;
                case KeyEvent.VK_DOWN: // カーソル下移動
                    menuCursor++;
                    if (menuCursor > 2)
                        menuCursor = 0;
                    repaint();
                    break;
                case KeyEvent.VK_ENTER: // 決定
                    if (menuCursor == 0) {
                        // 新規ゲーム
                        gameState = GameState.PLAYING;
                        resetGame();
                    } else if (menuCursor == 1) {
                        // ロード
                        File saveFile = new File(SAVE_FILE);
                        if (saveFile.exists()) {
                            loadGame();
                        }
                    } else if (menuCursor == 2) {
                        // 操作説明
                        gameState = GameState.HELP;
                        repaint();
                    }
                    break;
            }
            return;
        }

        // 操作説明画面での処理
        if (gameState == GameState.HELP) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                gameState = GameState.TITLE;
                repaint();
            }
            return;
        }

        // レベルアップメッセージ表示中は任意のキーで消去
        if (levelUpMessage != null) {
            levelUpMessage = null;
            repaint();
            return;
        }

        // ゲーム中の処理
        // Sキーでセーブ
        if (e.getKeyCode() == KeyEvent.VK_S) {
            saveGame();
            return;
        }

        // Mキーでマップ表示切替
        if (e.getKeyCode() == KeyEvent.VK_M) {
            if (hasMap) {
                showFullMap = !showFullMap;
                repaint();
            }
            return;
        }

        // Iキーでインベントリ表示切替
        if (e.getKeyCode() == KeyEvent.VK_I) {
            showInventory = !showInventory;
            if (showInventory) {
                inventoryCursor = 0; // カーソルをリセット
            }
            repaint();
            return;
        }

        // インベントリ表示中の操作
        if (showInventory) {
            if (e.getKeyCode() == KeyEvent.VK_UP) {
                if (inventoryCursor > 0) {
                    inventoryCursor--;
                    repaint();
                }
                return;
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                if (inventoryCursor < inventory.size() - 1) {
                    inventoryCursor++;
                    repaint();
                }
                return;
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (!inventory.isEmpty() && inventoryCursor < inventory.size()) {
                    useItem(inventoryCursor);
                    showInventory = false;
                    if (inventoryCursor >= inventory.size()) {
                        inventoryCursor = Math.max(0, inventory.size() - 1);
                    }
                }
                return;
            } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                showInventory = false;
                repaint();
                return;
            }
            return; // インベントリ表示中は他の操作を受け付けない
        }

        // Tキーで投擲モード切替
        if (e.getKeyCode() == KeyEvent.VK_T) {
            if (!throwables.isEmpty()) {
                throwMode = !throwMode;
                repaint();
            } else {
                JOptionPane.showMessageDialog(this, "投げるものがない！");
            }
            return;
        }

        // 睡眠中は行動不能
        if (playerStatus == StatusEffect.SLEEP) {
            JOptionPane.showMessageDialog(this, "眠っていて動けない...");
            turnCount++;
            processPlayerStatus();
            enemyTurn();
            repaint();
            return;
        }

        int nx = playerX;
        int ny = playerY;
        int dx = 0, dy = 0;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                nx--;
                dx = -1;
                playerDirection = Direction.LEFT;
                break;
            case KeyEvent.VK_RIGHT:
                nx++;
                dx = 1;
                playerDirection = Direction.RIGHT;
                break;
            case KeyEvent.VK_UP:
                ny--;
                dy = -1;
                playerDirection = Direction.UP;
                break;
            case KeyEvent.VK_DOWN:
                ny++;
                dy = 1;
                playerDirection = Direction.DOWN;
                break;
            default:
                return;
        }

        // 投擲モード時
        if (throwMode) {
            throwItem(dx, dy);
            turnCount++;
            processPlayerStatus();
            enemyTurn();
            repaint();
            return;
        }

        // 混乱時はランダム方向に移動
        if (playerStatus == StatusEffect.CONFUSION) {
            nx = playerX + (random.nextInt(3) - 1);
            ny = playerY + (random.nextInt(3) - 1);
        }

        if (canMoveTo(nx, ny)) {
            if (isEnemyAt(nx, ny)) {
                attackEnemy(nx, ny);
            } else {
                playerX = nx;
                playerY = ny;

                // 視界更新
                updateVisibility();

                // ショップアイテムチェック
                boolean onShopItem = false;
                for (ShopItem shopItem : shopItems) {
                    if (shopItem.x == nx && shopItem.y == ny) {
                        onShopItem = true;
                        int response = JOptionPane.showConfirmDialog(this,
                                shopItem.getName() + " (" + shopItem.price + "G)\n購入しますか？",
                                "ショップ", JOptionPane.YES_NO_OPTION);
                        if (response == JOptionPane.YES_OPTION) {
                            buyShopItem(nx, ny);
                        }
                        break;
                    }
                }

                // 通常アイテムチェック（ショップアイテムでない場合）
                if (!onShopItem && isItemAt(nx, ny)) {
                    pickUpItem(nx, ny);
                }

                // モンスターハウスチェック
                if (isMonsterHouse[ny][nx] && !monsterHouseTriggered) {
                    triggerMonsterHouse();
                }

                // 罠チェック
                if (isTrapAt(nx, ny)) {
                    stepOnTrap(nx, ny);
                }

                // 階段チェック
                if (nx == stairsX && ny == stairsY) {
                    goToNextFloor();
                    return;
                }
            }

            turnCount++;
            processPlayerStatus();
            decreaseHunger();
            enemyTurn();
            repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ダンジョン探索ゲーム");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            MoveGame_enemy game = new MoveGame_enemy();
            frame.setContentPane(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
