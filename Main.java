import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            @SuppressWarnings("unused")
            GameFrame frame = new GameFrame();
        });
    }
}

class GameFrame extends JFrame {
    public GameFrame() {
        this.setTitle("AI Shooter Challenge");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        
        GamePanel panel = new GamePanel();
        this.add(panel);
        this.pack(); 
        
        this.setLocationRelativeTo(null); 
        this.setVisible(true);
        
        panel.startGame(); 
    }
}

class GamePanel extends JPanel implements Runnable {
    public static final int GAME_WIDTH = 800;
    public static final int GAME_HEIGHT = 600;
    private Thread gameThread;
    private boolean running = false;

    private Player player;
    private List<Bullet> bullets;
    private List<Enemy> enemies;
    private List<Rock> rocks; // 新增：碎石障礙物列表
    private int score = 0;
    
    private boolean isGameOver = false;
    private int shootDelay = 0; 

    public GamePanel() {
        this.setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);

        initGame(); 

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                
                if (isGameOver) {
                    if (key == KeyEvent.VK_R) initGame();
                    return; 
                }

                if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) player.up = true;
                if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) player.down = true;
                if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) player.left = true;
                if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) player.right = true;
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) player.up = false;
                if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) player.down = false;
                if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) player.left = false;
                if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) player.right = false;
            }
        });
    }

    private void initGame() {
        player = new Player(GAME_WIDTH / 2, GAME_HEIGHT - 100);
        bullets = new ArrayList<>();
        enemies = new ArrayList<>();
        rocks = new ArrayList<>();
        score = 0;
        isGameOver = false;
        shootDelay = 0;

        // 初始化障礙物 (配合網格，將碎石大小設為 40x40，並放置在網格點上)
        // 左上角障礙
        rocks.add(new Rock(200, 200));
        rocks.add(new Rock(240, 200));
        rocks.add(new Rock(200, 240));
        // 右側障礙
        rocks.add(new Rock(560, 320));
        rocks.add(new Rock(560, 360));
        rocks.add(new Rock(600, 360));
        // 中間橫向障礙
        rocks.add(new Rock(320, 440));
        rocks.add(new Rock(360, 440));
        rocks.add(new Rock(400, 440));
        rocks.add(new Rock(440, 440));
    }

    public void startGame() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;

            if (delta >= 1) {
                update();
                repaint(); 
                delta--;
            }
        }
    }

    private void update() {
        if (isGameOver) return; 

        // 傳入 rocks 列表，讓玩家判斷能不能移動
        player.update(rocks);

        // 1. 自動發射子彈
        shootDelay++;
        if (shootDelay >= 15) {
            bullets.add(new Bullet(player.getX() + 12, player.getY()));
            shootDelay = 0;
        }

        // 2. 更新子彈
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            b.update();
            
            // 偵測子彈是否打中石頭 (打中石頭子彈就消失)
            if (b.isActive()) {
                for (Rock r : rocks) {
                    if (checkCollision(b.getX(), b.getY(), b.getWidth(), b.getHeight(), r.getX(), r.getY(), r.getSize(), r.getSize())) {
                        b.setActive(false);
                        break;
                    }
                }
            }

            if (!b.isActive()) {
                bullets.remove(i);
                i--;
            }
        }

        // 3. 生成敵人
        if (Math.random() < 0.03) {
            int randomX = (int)(Math.random() * (GAME_WIDTH - 30));
            enemies.add(new Enemy(randomX, -30)); 
        }

        // 4. 更新敵人
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            e.update();

            // 偵測敵人是否撞到石頭 (先設定撞到石頭就墜毀)
            for (Rock r : rocks) {
                if (e.isActive() && checkCollision(e.getX(), e.getY(), e.getWidth(), e.getHeight(), r.getX(), r.getY(), r.getSize(), r.getSize())) {
                    e.setActive(false);
                }
            }

            // 偵測子彈打中敵人
            for (Bullet b : bullets) {
                if (b.isActive() && e.isActive() && checkCollision(b.getX(), b.getY(), b.getWidth(), b.getHeight(), e.getX(), e.getY(), e.getWidth(), e.getHeight())) {
                    b.setActive(false);
                    e.setActive(false);
                    score += 100;
                }
            }

            // 偵測敵人撞擊玩家
            if (e.isActive() && checkCollision(player.getX(), player.getY(), player.getWidth(), player.getHeight(), e.getX(), e.getY(), e.getWidth(), e.getHeight())) {
                e.setActive(false); 
                player.reduceHp();  
                if (player.getHp() <= 0) isGameOver = true;
            }

            if (!e.isActive() || e.getY() > GAME_HEIGHT) {
                enemies.remove(i);
                i--;
            }
        }
    }

    // 將 AABB 碰撞偵測改為 public static，讓其他類別也能共用
    public static boolean checkCollision(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        g.setColor(Color.DARK_GRAY);
        for(int i = 0; i < GAME_WIDTH; i += 40) g.drawLine(i, 0, i, GAME_HEIGHT);
        for(int i = 0; i < GAME_HEIGHT; i += 40) g.drawLine(0, i, GAME_WIDTH, i);

        // 繪製障礙物
        for (Rock r : rocks) {
            r.draw(g);
        }

        player.draw(g);

        for (Bullet b : bullets) {
            b.draw(g);
        }

        for (Enemy e : enemies) {
            e.draw(g);
        }

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("SCORE: " + score, 10, 20);

        if (isGameOver) {
            g.setColor(new Color(0, 0, 0, 150)); 
            g.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
            
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("GAME OVER", GAME_WIDTH / 2 - 150, GAME_HEIGHT / 2);
            
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Press 'R' to Restart", GAME_WIDTH / 2 - 80, GAME_HEIGHT / 2 + 40);
        }
    }
}

// ==========================================
// 遊戲實體類別
// ==========================================

class Player {
    private int x, y;
    private final int width = 30;
    private final int height = 30;
    private final int speed = 5;
    private int hp = 3;

    public boolean up, down, left, right;

    public Player(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }

    // 更新：傳入石頭列表，進行「移動預判」
    public void update(List<Rock> rocks) {
        int nextX = x;
        int nextY = y;

        if (up && y > 0) nextY -= speed;
        if (down && y < GamePanel.GAME_HEIGHT - height) nextY += speed;
        if (left && x > 0) nextX -= speed;
        if (right && x < GamePanel.GAME_WIDTH - width) nextX += speed;

        // 檢查預測的下一步是否會撞到石頭
        boolean collisionWithRock = false;
        for (Rock r : rocks) {
            if (GamePanel.checkCollision(nextX, nextY, width, height, r.getX(), r.getY(), r.getSize(), r.getSize())) {
                collisionWithRock = true;
                break;
            }
        }

        // 如果沒有撞到石頭，才真正移動飛機
        if (!collisionWithRock) {
            x = nextX;
            y = nextY;
        }
    }

    public void draw(Graphics g) {
        if (hp == 1) g.setColor(Color.ORANGE);
        else g.setColor(Color.BLUE); 
        
        g.fillRect(x, y, width, height);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("HP: " + hp, x, y - 5);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public void reduceHp() { hp--; }
    public int getHp() { return hp; }
}

class Bullet {
    private int x, y;
    private final int width = 6;
    private final int height = 15;
    private final int speed = 10;
    private boolean active = true;

    public Bullet(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }

    public void update() {
        y -= speed; 
        if (y < 0) active = false; 
    }

    public void draw(Graphics g) {
        g.setColor(Color.YELLOW);
        g.fillRect(x, y, width, height);
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}

class Enemy {
    private int x, y;
    private final int width = 30;
    private final int height = 30;
    private final int speed = 2;
    private boolean active = true;

    public Enemy(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }

    public void update() {
        y += speed; 
    }

    public void draw(Graphics g) {
        g.setColor(Color.RED);
        g.fillRect(x, y, width, height);
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}

// 新增：碎石(障礙物)類別
class Rock {
    private int x, y;
    private final int size = 40; // 設定為 40，剛好佔滿一格網格

    public Rock(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics g) {
        g.setColor(Color.GRAY);
        // 畫一個灰色的方形代表石頭
        g.fillRect(x, y, size, size);
        
        // 幫石頭畫上邊框增加立體感
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(x, y, size, size);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getSize() { return size; }
}
