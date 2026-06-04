package com.example.skuska3;

import com.example.skuska3.GameState.CellState;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ============================================================================
 *  MainGame  -  HLAVNA TRIEDA (GUI + ovladanie)
 * ----------------------------------------------------------------------------
 *  Struktura:
 *    - CellPane       extends Pane      ... jedno policko (kresli sa v paint())
 *    - PlaygroundPane extends GridPane  ... cela hracia plocha (mriezka policok)
 *
 *  Layout (BorderPane):  TOP = info, CENTER = plocha, BOTTOM = ovladanie.
 *  Ovladanie: klik = vyber policka, sipky = pohyb vybraneho objektu.
 * ============================================================================
 */
public class MainGame extends Application {

    static final String CONFIG_PREFIX = "subory/hamasando"; // meno suborov: config1.txt, config2.txt, ...
    static final int    CONFIG_COUNT  = 3;         // pocet konfiguracii pre Prev/Next
    static final String SAVE_FILE     = "savedGame.dat";


    // ---- GUI komponenty ----
    BorderPane root;
    PlaygroundPane playground;
    Label lbInfo  = new Label();
    Label lbTime  = new Label();
    Label lbMoves = new Label();
    Button btnSave = new Button("Save");
    Button btnLoad = new Button("Load");
    Button btnUndo = new Button("Undo");
    Button btnPrev = new Button("Prev");
    Button btnNext = new Button("Next");

    // ---- stav a pomocne premenne ----
    GameState state;
    Timeline clock;
    Deque<GameState> history = new ArrayDeque<>();  // zasobnik kopii pre Undo
    int currentConfig = 1;                          // aktualna konfiguracia (Prev/Next)
    boolean finishedHandled = false;                // aby koniec hry zbehol len raz
    boolean solved = false;


    // =======================================================================
    //  CellPane extends Pane  -  jedno policko
    // =======================================================================
    public class CellPane extends Pane {
        int row, col;

        public CellPane(int row, int col) {
            this.row = row;
            this.col = col;

            // pri zmene velkosti policko prekreslime (responzivita)
            widthProperty().addListener((o, a, b) -> paint());
            heightProperty().addListener((o, a, b) -> paint());

            // klik mysou = vyber policka
            setOnMousePressed(e -> {
                if (!solved) {
                    CellState cs = state.get(row, col);
                    if (!cs.isHeader) {
                        pushHistory();

                        if (e.getButton() == MouseButton.PRIMARY) {
                            cs.changeValue(1);
                        } else if (e.getButton() == MouseButton.SECONDARY) {
                            cs.changeValue(2);
                        }

                        state.recalculateHeaders();
                        state.moveCount++;
                        refresh();
                        checkFinished();
                    }
                }
            });
        }

        /** Vykresli policko podla jeho stavu. */
        public void paint() {
            double w = getWidth(), h = getHeight();
            getChildren().clear();
            CellState cell = state.get(row, col);
            if (cell == null) return;

            // pozadie policka (farba podla value)
            Rectangle bg = new Rectangle(0, 0, w, h);
            Color color;
            if (cell.isHeader){
                if (cell.isSolved){
                    color = Color.LIGHTBLUE;
                } else {
                    color = Color.LIGHTGRAY;
                }
            } else {
                color = Color.WHITE;
            }
            bg.setFill(color);
            bg.setStroke(Color.web("#00000022"));
            getChildren().add(bg);

            if (cell.isHeader){
                if (cell.value != -1) {
                    Text t = new Text(w/2, h/2, "" + cell.value);
                    t.setFont(new Font(20));
                    getChildren().add(t);
                }
            } else {
                if (cell.value == 1){
                    Rectangle r = new Rectangle(w/6, h/6, 4*w/6, 4*h/6);
                    r.setFill(Color.LIGHTGREEN);
                    r.setStroke(Color.BLACK);
                    r.setStrokeWidth(3);
                    getChildren().add(r);
                } else if (cell.value == 2){
                    Ellipse e = new Ellipse(w/2, h/2, w/3, h/3);
                    e.setFill(Color.LIGHTPINK);
                    e.setStroke(Color.BLACK);
                    e.setStrokeWidth(3);
                    getChildren().add(e);
                }
            }
        }
    }


    // =======================================================================
    //  PlaygroundPane extends GridPane  -  cela plocha
    // =======================================================================
    public class PlaygroundPane extends GridPane {
        CellPane[][] cells;

        public PlaygroundPane() {
            int R = state.rows, C = state.cols;
            cells = new CellPane[R][C];

            // rovnaky podiel sirky/vysky pre kazdy stlpec/riadok -> grid sa
            // sam roztiahne na velkost okna (responzivita)
            for (int c = 0; c < C; c++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setPercentWidth(100.0 / C);
                getColumnConstraints().add(cc);
            }
            for (int r = 0; r < R; r++) {
                RowConstraints rc = new RowConstraints();
                rc.setPercentHeight(100.0 / R);
                getRowConstraints().add(rc);
            }

            // vytvor policka (POZOR: add(uzol, STLPEC, RIADOK))
            for (int r = 0; r < R; r++) {
                for (int c = 0; c < C; c++) {
                    CellPane cell = new CellPane(r, c);
                    cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                    cells[r][c] = cell;
                    this.add(cell, c, r);
                }
            }
        }

        /** Prekresli vsetky policka + aktualizuj info. */
        public void paint() {
            updateInfo();
            for (int r = 0; r < cells.length; r++)
                for (int c = 0; c < cells[r].length; c++)
                    cells[r][c].paint();
        }
    }


    // =======================================================================
    //  START
    // =======================================================================
    @Override
    public void start(Stage stage) {
        state = loadConfigOrEmpty(currentConfig);
        playground = new PlaygroundPane();

        // TOP info panel
        HBox top = new HBox(30, lbInfo, lbTime, lbMoves);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(10));

        // BOTTOM ovladaci panel
        HBox bottom = new HBox(10, btnPrev, btnUndo, btnSave, btnLoad, btnNext);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(10));

        root = new BorderPane();
        root.setTop(top);
        root.setCenter(playground);
        root.setBottom(bottom);

        // pocitanie casu
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            state.elapsedTime++;
            updateInfo();
        }));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // tlacidla
        btnSave.setOnAction(e -> state.save(SAVE_FILE));
        btnLoad.setOnAction(e -> {
            GameState s = GameState.load(SAVE_FILE);
            if (s != null) {
                state = s;
                history.clear();
                finishedHandled = false;
                rebuildPlayground();
                refresh();
            }
        });
        btnUndo.setOnAction(e -> undo());
        btnPrev.setOnAction(e -> { if (currentConfig > 1) switchConfig(currentConfig - 1); });
        btnNext.setOnAction(e -> { if (currentConfig < CONFIG_COUNT) switchConfig(currentConfig + 1); });

        // scene + ovladanie klavesnicou (sipky = pohyb vybraneho objektu)
        Scene scene = new Scene(root, 600, 680);

        stage.setTitle("Hra");
        stage.setScene(scene);
        stage.show();
        refresh();
    }


    // =======================================================================
    //  POMOCNE METODY
    // =======================================================================

    void refresh() {
        updateInfo();
        if (playground != null) playground.paint();
    }

    void updateInfo() {
        if (!solved) {lbInfo.setText("Hra " + currentConfig);}
        lbTime.setText("Čas: " + state.elapsedTime + " s");
        lbMoves.setText("Ťahy: " + state.moveCount);
    }

    /** Po vymene stavu (Load/Prev/Next) postav GridPane nanovo. */
    void rebuildPlayground() {
        playground = new PlaygroundPane();
        root.setCenter(playground);
    }

    void pushHistory() {
        history.push(state.copy());
    }

    /** Undo - vrat hru o krok spat (opakovane az po start). */
    void undo() {
        if (history.isEmpty()) return;
        int t = state.elapsedTime;     // cas necht bezat dalej
        state = history.pop();
        state.elapsedTime = t;
        finishedHandled = false;
        rebuildPlayground();
        refresh();
    }

    /** Prev/Next - prepni na inu konfiguraciu. */
    void switchConfig(int idx) {
        currentConfig = idx;
        state = loadConfigOrEmpty(idx);
        history.clear();
        finishedHandled = false;
        rebuildPlayground();
        refresh();
    }

    /**
     * Nacitanie konfiguracie: subor v pracovnom adresari, meno = prefix + cislo + ".txt".
     * (alternativa: FileChooser). Pri chybe vrati prazdnu plochu.
     */
    GameState loadConfigOrEmpty(int idx) {
        String path = CONFIG_PREFIX + idx + ".txt";
        try {
            GameState s = GameState.loadConfig(path);
            if (s == null) {
                throw new Exception("loadConfig vratil null");
            }
            return s;
        } catch (Exception ex) {
            System.out.println("Nepodarilo sa nacitat " + path + ": " + ex.getMessage());
            return new GameState(6, 6);
        }
    }

    /** Koniec hry: gratulacia + prepnutie na dalsiu po 10 s. */
    void checkFinished() {
        if (!state.isFinished() || finishedHandled) return;
        solved = true;
        finishedHandled = true;
        lbInfo.setText("Vyriešené!");
        PauseTransition wait = new PauseTransition(Duration.seconds(10));
        wait.setOnFinished(ev -> {
            if (currentConfig < CONFIG_COUNT) switchConfig(currentConfig + 1);
            solved = false;
        });
        wait.play();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
