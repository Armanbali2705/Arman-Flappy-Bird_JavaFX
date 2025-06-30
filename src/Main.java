// Flappy Bird Game - JavaFX | Rewritten with Structured States (Splash → Play → Game Over)

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.Group;

import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class Main extends Application {

    private enum GameState { SPLASH, PLAY, GAME_OVER }

    private final int WIDTH = 432, HEIGHT = 768;
    private final double GRAVITY = 0.45, JUMP = -8.5, PIPE_SPEED = 2.5;
    private final int PIPE_WIDTH = 52, PIPE_GAP = 160, BIRD_SIZE = 34;

    private Image bg, base, msg, pipeImg, pipeFlipImg;
    private Image[] birdFrames;

    private double birdY = HEIGHT / 2;
    private double velocity = 0;
    private double groundX = 0;
    private int score = 0, highScore = 0, frameCount = 0;
    private int birdFrame = 0;
    private GameState gameState = GameState.SPLASH;

    private ArrayList<Pipe> pipes = new ArrayList<>();
    private Random rand = new Random();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Scene scene = new Scene(new Group(canvas));

        loadAssets();
        loadHighScore();

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.UP) {
                switch (gameState) {
                    case SPLASH:
                        gameState = GameState.PLAY;
                        break;
                    case PLAY:
                        velocity = JUMP;
                        playSound("assets/audio/wing.wav");
                        break;
                    case GAME_OVER:
                        if (birdY + BIRD_SIZE >= HEIGHT - 112) {
                            restartGame();
                            gameState = GameState.SPLASH;
                        }
                        break;
                }
            }
        });

        stage.setScene(scene);
        stage.setTitle("Flappy Bird JavaFX");
        stage.setResizable(false);
        stage.show();

        new AnimationTimer() {
            public void handle(long now) {
                switch (gameState) {
                    case SPLASH -> splash(gc);
                    case PLAY -> {
                        update();
                        draw(gc);
                    }
                    case GAME_OVER -> {
                        crash();
                        draw(gc);
                    }
                }
            }
        }.start();
    }

    private void splash(GraphicsContext gc) {
        draw(gc);
        animateBird();
    }

    private void update() {
        velocity += GRAVITY;
        birdY += velocity;
        frameCount++;

        groundX -= PIPE_SPEED;
        if (groundX <= -WIDTH) groundX = 0;

        if (frameCount % 100 == 0) addPipe();

        for (Pipe p : pipes) {
            p.x -= PIPE_SPEED;
            if (!p.scored && p.x + PIPE_WIDTH < 100) {
                score++;
                p.scored = true;
            }
            if (100 + BIRD_SIZE > p.x && 100 < p.x + PIPE_WIDTH) {
                if (birdY < p.gapY || birdY + BIRD_SIZE > p.gapY + PIPE_GAP) {
                    gameState = GameState.GAME_OVER;
                    playSound("assets/audio/hit.wav");
                }
            }
        }

        pipes.removeIf(p -> p.x + PIPE_WIDTH < 0);

        if (birdY + BIRD_SIZE >= HEIGHT - 112 || birdY < 0) {
            gameState = GameState.GAME_OVER;
            playSound("assets/audio/hit.wav");
        }
    }

    private void draw(GraphicsContext gc) {
        gc.drawImage(bg, 0, 0, WIDTH, HEIGHT);

        for (Pipe p : pipes) {
            gc.drawImage(pipeFlipImg, p.x, p.gapY - 320, PIPE_WIDTH, 320);
            gc.drawImage(pipeImg, p.x, p.gapY + PIPE_GAP, PIPE_WIDTH, 320);
        }

        gc.drawImage(birdFrames[birdFrame], 100, birdY, BIRD_SIZE, BIRD_SIZE);

        gc.drawImage(base, groundX, HEIGHT - 112, WIDTH * 2, 112);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 28));
        gc.fillText("Score: " + score, 15, 40);
        gc.setFont(Font.font("Arial", 20));
        gc.fillText("High: " + highScore, 15, 70);

        if (gameState == GameState.SPLASH) {
            gc.drawImage(msg, (WIDTH - 184) / 2.0, 200);
        } else if (gameState == GameState.GAME_OVER) {
            gc.setFont(Font.font("Arial", 26));
            gc.fillText("Game Over! Press SPACE", WIDTH / 2.0 - 130, HEIGHT / 2.0);
        }
    }

    private void animateBird() {
        birdFrame = (birdFrame + 1) % birdFrames.length;
        birdY = HEIGHT / 2 + Math.sin(frameCount * 0.15) * 8;
        frameCount++;
    }

    private void crash() {
        if (birdY + BIRD_SIZE < HEIGHT - 112) {
            velocity += GRAVITY;
            birdY += velocity;
        }
    }

    private void restartGame() {
        birdY = HEIGHT / 2;
        velocity = 0;
        score = 0;
        frameCount = 0;
        pipes.clear();
    }

    private void addPipe() {
        int gapY = 100 + rand.nextInt(HEIGHT - 300);
        pipes.add(new Pipe(WIDTH, gapY));
    }

    private void loadAssets() {
        bg = new Image("file:assets/sprites/background-day.png");
        base = new Image("file:assets/sprites/base.png");
        msg = new Image("file:assets/sprites/message.png");
        pipeImg = new Image("file:assets/sprites/pipe-green.png");
        pipeFlipImg = new Image("file:assets/sprites/pipe-green.png", PIPE_WIDTH, 320, false, true);
        birdFrames = new Image[]{
            new Image("file:assets/sprites/bluebird-upflap.png"),
            new Image("file:assets/sprites/bluebird-midflap.png"),
            new Image("file:assets/sprites/bluebird-downflap.png")
        };
    }

    private void loadHighScore() {
        try (BufferedReader r = new BufferedReader(new FileReader("highscore.txt"))) {
            String line = r.readLine();
            if (line != null) highScore = Integer.parseInt(line);
        } catch (IOException | NumberFormatException ignored) {}
    }

    private void playSound(String path) {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File(path));
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("Sound failed: " + e.getMessage());
        }
    }

    private static class Pipe {
        double x, gapY;
        boolean scored = false;

        Pipe(double x, double gapY) {
            this.x = x;
            this.gapY = gapY;
        }
    }
}
