package com.example.skuska2;

import javafx.scene.image.ImageView;

import java.io.*;

/**
 * ============================================================================
 *  GameState  -  MODEL HRY (data + stav, ziadna grafika)
 * ----------------------------------------------------------------------------
 *  implements Serializable -> cely stav sa da ulozit/nacitat jednym
 *  writeObject(this) / readObject(). Vsetky polia musia byt serializovatelne;
 *  JavaFX uzly (ImageView) preto drzime ako 'transient' a po nacitani obnovime.
 * ============================================================================
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    int rows;              // rozmery hracej plochy
    int cols;
    CellState[][] board;   // mriezka stavov policok
    int elapsedTime = 0;   // uplynuly cas v sekundach
    int moveCount   = 0;   // pocet tahov

    // sem si doplnis vlastne stavove polia podla zadania


    // =======================================================================
    //  VNORENA TRIEDA: stav jedneho policka
    // =======================================================================
    public static class CellState implements Serializable {
        private static final long serialVersionUID = 2L;

        int value;                  // obsah policka (vyznam podla zadania)
        transient ImageView image;  // obrazok policka, ak ho hra pouziva; transient = neserializuje sa

        public CellState(int value) {
            this.value = value;
            loadImage();
        }

        /** Nacitanie obrazka podla value (len ak ho hra pouziva, inak necht prazdne). */
        public void loadImage() {
            // image = new ImageView(new Image(getClass().getResourceAsStream(...)));
        }

        /** Po load() treba obrazok obnovit (bol transient -> null). */
        public void reloadImage() {
            loadImage();
        }
    }


    // =======================================================================
    //  KONSTRUKTOR
    // =======================================================================
    public GameState(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.board = new CellState[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                board[r][c] = new CellState(0);
    }


    // =======================================================================
    //  NACITANIE KONFIGURACIE ZO SUBORU
    //  (format zavisi od zadania - tu nacitaj rozmery a obsah policok)
    // =======================================================================
    public static GameState loadConfig(File file) throws IOException {
        return null;

/*
        List<String> lines = Files.readAllLines(Paths.get(path));
        int n = Integer.parseInt(lines.getFirst().trim());
        lines.removeFirst();

        int w = lines.getFirst().split("\\s+").length;

        GameState s = new GameState(n, n);
        s.carIndex = Integer.parseInt(lines.getFirst().trim().split(" ")[0]);
        s.reqPosition = Integer.parseInt(lines.getFirst().trim().split(" ")[1]);
        lines.removeFirst();

        for (int r = 0; r < n; r++){
            String[] row = lines.get(r).trim().split(" ");
            for (int c = 0; c < n; c++){
                int value = Integer.parseInt(row[c].trim());
                CellState cs = new CellState(value);
                cs.isTarget = (value == s.carIndex);
                s.board[r][c] = cs;
            }
        }
        return s;*/
    }


    // =======================================================================
    //  SERIALIZACIA - SAVE / LOAD rozohranej hry
    // =======================================================================
    public void save(String path) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path))) {
            out.writeObject(this);
        } catch (IOException e) {
            System.out.println("Save zlyhal: " + e.getMessage());
        }
    }

    public static GameState load(String path) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(path))) {
            GameState s = (GameState) in.readObject();
            // obnov obrazky (boli transient)
            for (int r = 0; r < s.rows; r++)
                for (int c = 0; c < s.cols; c++)
                    if (s.board[r][c] != null) s.board[r][c].reloadImage();
            return s;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Load zlyhal: " + e.getMessage());
            return null;
        }
    }


    // =======================================================================
    //  POMOCNE METODY
    // =======================================================================

    /** Bezpecny pristup k policku (null ak je mimo plochy). */
    public CellState get(int r, int c) {
        if (0 <= r && r < rows && 0 <= c && c < cols) return board[r][c];
        return null;
    }

    /** Hlboka kopia stavu - pre Undo (kopiruje sa aj obsah policok). */
    public GameState copy() {
        GameState s = new GameState(rows, cols);
        s.elapsedTime = elapsedTime;
        s.moveCount   = moveCount;

        //skopirovat vsetky hodnoty tiez

        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                s.board[r][c] = new CellState(board[r][c].value);
                //CellState cs = new CellState(board[r][c].value);
                //cs.isTarget = board[r][c].isTarget;
                //cs.isFinish = board[r][c].isFinish;
                //s.board[r][c] = cs; 


        return s;
    }

    /** Pokus o tah - pravidla (hranice, kolizie, povoleny smer) podla zadania. */
    public boolean tryMove(int row, int col, int dr, int dc) {
        // over, ci sa objekt na (row,col) moze posunut o (dr,dc); ak ano, vykonaj
        // posun, zvys moveCount a vrat true; inak vrat false
        return false;
    }

    /** Ci je hra dohrana - podmienka podla zadania. */
    public boolean isFinished() {
        return false;
    }
}
