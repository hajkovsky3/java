package com.example.skuska3;

import javafx.scene.image.ImageView;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

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
        boolean isHeader;
        boolean isSolved;

        public CellState(int value, boolean header) {
            this.value = value;
            this.isHeader = header;
        }

        public void changeValue(int val){
            if (val == value){
                value = 0;
            } else {
                value = val;
            }
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
                board[r][c] = new CellState(0, false);
    }


    // =======================================================================
    //  NACITANIE KONFIGURACIE ZO SUBORU
    //  (format zavisi od zadania - tu nacitaj rozmery a obsah policok)
    // =======================================================================
    public static GameState loadConfig(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path));
        int n = Integer.parseInt(lines.getFirst().trim());
        String[] rowsHeader = lines.get(1).split(",");
        String[] colsHeader = lines.get(2).split(",");

        GameState s = new GameState(n+1, n+1);

        for (int i = 0; i < n; i++){
            //System.out.println(rowsHeader[i] + " " + colsHeader[i]);
            int a;
            int b;
            if (rowsHeader[i].equals("")){
                a = -1;
            } else {
                a = Integer.parseInt(rowsHeader[i]);
            }
            if (colsHeader[i].equals("")){
                b = -1;
            } else {
                b = Integer.parseInt(colsHeader[i]);
            }
            CellState cs1 = new CellState(a, true);
            CellState cs2 = new CellState(b, true);
            s.board[0][i+1] = cs1;
            s.board[i+1][0] = cs2;
        }

        return s;
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
                s.board[r][c] = new CellState(board[r][c].value, board[r][c].isHeader);

        return s;
    }

    /** Pokus o tah - pravidla (hranice, kolizie, povoleny smer) podla zadania. */
    public boolean tryMove(int row, int col, int dr, int dc) {
        // over, ci sa objekt na (row,col) moze posunut o (dr,dc); ak ano, vykonaj
        // posun, zvys moveCount a vrat true; inak vrat false
        return false;
    }

    public void recalculateHeaders(){

        for (int i = 1; i < rows; i++){
            board[i][0].isSolved = checkRow(i);
        }
        for (int i = 1; i < cols; i++){
            board[0][i].isSolved = checkCol(i);
        }
    }

    public boolean checkRow(int n){
        int target = board[n][0].value;
        int squareCount = 0;
        int hamCount = 0;
        boolean count = false;

        for (int i = 1; i < cols; i++){
            CellState cs = board[n][i];
            if (cs.value == 1){
                squareCount++;
                count = !count;
            } else if (cs.value == 2) {
                if (count){hamCount++;}
            }
        }

        if (target == -1) {
            return (squareCount == 2);
        } else {
            return (squareCount == 2) && (hamCount == target);
        }
    }

    public boolean checkCol(int n){
        int target = board[0][n].value;
        int squareCount = 0;
        int hamCount = 0;
        boolean count = false;

        for (int i = 1; i < rows; i++){
            CellState cs = board[i][n];
            if (cs.value == 1){
                squareCount++;
                count = !count;
            } else if (cs.value == 2) {
                if (count){hamCount++;}
            }
        }

        if (target == -1) {
            return (squareCount == 2);
        } else {
            return (squareCount == 2) && (hamCount == target);
        }
    }

    /** Ci je hra dohrana - podmienka podla zadania. */
    public boolean isFinished() {
        for (int i = 1; i < rows; i++){
            if (!board[i][0].isSolved) {return false;}
        }
        for (int i = 1; i < cols; i++){
            if (!board[0][i].isSolved) {return false;}
        }
        return true;
    }
}
