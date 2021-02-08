/*

 */
package aimtrainer;

import java.util.List;
import java.util.Random;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 *
 * @author kubaj
 */
public class RandomMap extends Thread {
    private Stage stage = AimTrainer.getPrimaryStage();
    private Pane root;
    private int approachTime;
    private int time;
    private double speed;
    private double size;
    private List<Color> colors;
    private MapSettings settings;
    private Rectangle2D screen = Screen.getPrimary().getBounds();
    private double minX = screen.getMinX() + 80;
    private double minY = screen.getMinY() + 80;
    private double maxX = screen.getMaxX() - 80;
    private double maxY = screen.getMaxY() - 80;
    private int hit;
    private int missed;
    private double accuracy;
    private final IntegerProperty score = new SimpleIntegerProperty();
    private double multiplier;
    private int highestCombo;
    private final IntegerProperty combo = new SimpleIntegerProperty();
    

    public RandomMap(MapSettings settings) {
        this.settings = settings;
        this.approachTime = settings.getApproachTime();
        this.time = settings.getTime();
        this.speed = settings.getSpeed();
        this.size = settings.getSize();
        this.colors = settings.getColors();
        multiplier = (speed/5) + (50/size);
        root = new Pane();
        Label scoreLabel = new Label();
        scoreLabel.setPadding(new Insets(20));
        scoreLabel.setFont(new Font("Technic", 30));
        scoreLabel.textProperty().bind(score.asString());
        
        Label comboLabel = new Label();
        comboLabel.setLayoutY(maxY + 15);
        comboLabel.setPadding(new Insets(20));
        comboLabel.setFont(new Font("Technic", 30));
        comboLabel.textProperty().bind(combo.asString().concat("x"));
        
        //debug
        Circle c1 = new Circle(minX, minY, 5);
        Circle c2 = new Circle(maxX, minY, 5);
        Circle c3 = new Circle(minX, maxY, 5);
        Circle c4 = new Circle(maxX, maxY, 5);
        root.getChildren().addAll(c1, c2, c3, c4);
        
        root.getChildren().addAll(scoreLabel, comboLabel);
        stage.getScene().setRoot(root);
    }
    
    public void generateRandom(){
        Random rand = new Random();
        double x = (rand.nextDouble() * (maxX-minX)) + minX;
        double y = (rand.nextDouble() * (maxY-minY)) + minY;
        Color color = colors.get(rand.nextInt(colors.size()));
        Circle c = new Circle(x, y, size);
        c.fillProperty().set(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.5));
        c.setStroke(color);
        c.setStrokeType(StrokeType.CENTERED);
        c.setStrokeWidth(size/15.0);
        
        
        ApproachCircleGen acg = new ApproachCircleGen(x, y, size, approachTime, color);
        root.getChildren().add(acg.getCircle());
        
        PauseTransition pt = new PauseTransition(Duration.millis(approachTime));
        pt.setOnFinished(event -> {
           if(root.getChildren().contains(c)){
               acg.stopAnimation();
               root.getChildren().remove(c);
               if(combo.get() > highestCombo){
                   highestCombo = combo.get();
               }
               combo.set(0);
               missed++;
           }
        });

        pt.play();
        acg.startAnimation();
        long startTime = System.currentTimeMillis();
        c.fillProperty().set(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.65));
        
        c.setOnMouseClicked((MouseEvent event) -> {
            acg.stopAnimation();
            long stopTime = System.currentTimeMillis();
            long reaction = stopTime - startTime;
            if(reaction < approachTime){
                combo.set(combo.get() + 1);
                int i = score.get() + (int) ((approachTime - reaction) * multiplier * combo.get());
                score.set(i);
                hit++;
                System.out.println("\nReaction: " + reaction + "\nMultiplier: " + multiplier + "\ncombo: " + combo.get());
                System.out.println("Score: " + score.get() + " - " + i);
            }
            root.getChildren().remove(c);
        });
        root.getChildren().add(c);
    }
    
    public void centerText(Text t){
        double x = t.getX();
        double y = t.getY();
        double width = t.getBoundsInLocal().getWidth();
        double height = t.getBoundsInLocal().getHeight();
        t.relocate(x - (width / 2), y - (height / 2));
    }

    @Override
    public void run() {
        try {
            missed = 0;
            score.set(0);
            combo.set(0);
            Text t = new Text();
            t.setFont(new Font("Technic", 110));
            t.setX(screen.getMaxX()/2);
            t.setY(screen.getMaxY()/2 - 80);
            Text t2 = new Text("Klikni na terče co nejrychleji");
            t2.setFont(new Font("Technic", 30));
            t2.setX(t.getX());
            t2.setY(t.getY() + 60);
            centerText(t2);
            FadeTransition ft2 = new FadeTransition(Duration.millis(3000), t2);
            ft2.setFromValue(1.5);
            ft2.setToValue(0.0);
            ft2.play();
            Platform.runLater(() -> root.getChildren().add(t));
            Platform.runLater(() -> root.getChildren().add(t2));
            for(int i = 3; i > 0; i--){
                t.setText(String.valueOf(i));
                centerText(t);
                FadeTransition ft = new FadeTransition(Duration.millis(900), t);
                ft.setFromValue(1.0);
                ft.setToValue(0.0);
                ft.play();
                Thread.sleep(1000);
            }
            Platform.runLater(() -> root.getChildren().remove(t));
            
        } catch (InterruptedException ex) {
            System.out.println("Thread has been interrupted");
        }
        int interval = (int) (1000 - Math.pow(speed, 2.9));
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(interval), event -> {
                generateRandom();
                
            })
        );
        int cycleCount = (time * 1000) / interval;
        timeline.setCycleCount(cycleCount);
        timeline.play();
        timeline.setOnFinished(event -> {
            if(combo.get() > highestCombo){
                highestCombo = combo.get();
            }
            Result res = new Result(hit, missed, score.get(), highestCombo, System.currentTimeMillis());
            MapSettings settings = new MapSettings(approachTime, time, speed, size, colors);
            ResultScreen resScreen = new ResultScreen(res, settings);
            stage.getScene().setRoot(resScreen.getRoot());
        });
    }
}