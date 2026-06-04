package com.example.skuska2;

import com.example.skuska2.GameState.CellState;

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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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

    static final String CONFIG_PREFIX = "files/blocks"; // meno suborov: config1.txt, config2.txt, ...
    static final int    CONFIG_COUNT  = 6;         // pocet konfiguracii pre Prev/Next
    static final String SAVE_FILE     = "savedGame.dat";

    // paleta farieb policok podla value (index 0 = prazdne)
    static final Color[] PALETTE = {
        Color.WHITE,
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
        Color.CYAN, Color.PURPLE, Color.MAGENTA, Color.ORANGE,
        Color.BROWN, Color.PINK, Color.GOLD, Color.SILVER,
    };

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
    int selectedRow = -1, selectedCol = -1;         // vybrane policko (-1 = nic)
    int currentConfig = 1;                          // aktualna konfiguracia (Prev/Next)
    boolean finishedHandled = false;                // aby koniec hry zbehol len raz


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
                selectedRow = row;
                selectedCol = col;
                playground.paint();
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
            Color color = (cell.value >= 0 && cell.value < PALETTE.length)
                          ? PALETTE[cell.value] : Color.GRAY;
            bg.setFill(color);
            bg.setStroke(Color.web("#00000022"));
            getChildren().add(bg);

            // obrazok policka, ak ho hra pouziva
            if (cell.image != null) {
                cell.image.setFitWidth(w);
                cell.image.setFitHeight(h);
                getChildren().add(cell.image);
            }

            // zvyraznenie vybraneho policka
            if (row == selectedRow && col == selectedCol) {
                Rectangle sel = new Rectangle(2, 2, w - 4, h - 4);
                sel.setFill(Color.TRANSPARENT);
                sel.setStroke(Color.BLACK);
                sel.setStrokeWidth(3);
                getChildren().add(sel);
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
                /*
                if (c == 0 || c == C - 1){
                    cc.setPercentWidth(20.0 / C);
                } else {
                    cc.setPercentWidth(100.0 / C);
                }
                */
                getColumnConstraints().add(cc);
            }
            for (int r = 0; r < R; r++) {
                RowConstraints rc = new RowConstraints();
                rc.setPercentHeight(100.0 / R);
                /*
                if (r == 0 || r == R - 1){
                    cc.setPercentWidth(20.0 / R);
                } else {
                    cc.setPercentWidth(100.0 / R);
                }
                */
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
                selectedRow = selectedCol = -1;
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
        scene.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            int dr = 0, dc = 0;
            if (ev.getCode() == KeyCode.UP)         dr = -1;
            else if (ev.getCode() == KeyCode.DOWN)  dr =  1;
            else if (ev.getCode() == KeyCode.LEFT)  dc = -1;
            else if (ev.getCode() == KeyCode.RIGHT) dc =  1;
            else if (ev.getCode() == KeyCode.Q) { Platform.exit(); return; }
            else return;
            ev.consume();

            if (selectedRow < 0) return;
            pushHistory();
            boolean moved = state.tryMove(selectedRow, selectedCol, dr, dc);
            if (moved) {
                selectedRow += dr;
                selectedCol += dc;
            } else {
                history.pop();   // tah sa nepodaril -> zahod ulozenu kopiu
            }
            refresh();
            checkFinished();
        });

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
        lbInfo.setText("Hra " + currentConfig);
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
        selectedRow = selectedCol = -1;
        finishedHandled = false;
        rebuildPlayground();
        refresh();
    }

    /** Prev/Next - prepni na inu konfiguraciu. */
    void switchConfig(int idx) {
        currentConfig = idx;
        state = loadConfigOrEmpty(idx);
        history.clear();
        selectedRow = selectedCol = -1;
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
        finishedHandled = true;
        lbInfo.setText("Vyriešené!");
        PauseTransition wait = new PauseTransition(Duration.seconds(10));
        wait.setOnFinished(ev -> {
            if (currentConfig < CONFIG_COUNT) switchConfig(currentConfig + 1);
        });
        wait.play();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
