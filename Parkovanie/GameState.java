package com.example.skuska;

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

    int carIndex = -1;
    int reqPosition = -1;

    // sem si doplnis vlastne stavove polia podla zadania


    // =======================================================================
    //  VNORENA TRIEDA: stav jedneho policka
    // =======================================================================
    public static class CellState implements Serializable {
        private static final long serialVersionUID = 2L;

        int value;                  // obsah policka (vyznam podla zadania)
        boolean isTarget;
        boolean isFinish;

        public CellState(int value) {
            this.value = value;
            this.isTarget = false;
            this.isFinish = false;
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
    public static GameState loadConfig(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path));
        int n = Integer.parseInt(lines.getFirst().trim());
        lines.removeFirst();

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


        boolean found = false;
        for (int r = 0; r < n; r++){
            for (int c = 0; c < n; c++){
                CellState cs = s.board[r][c];
                if (cs.isTarget){
                    if (r < n - 1 && s.board[r+1][c].isTarget){
                        int y = r;
                        while (s.board[y][c].isTarget){
                            s.board[s.reqPosition + r - y][c].isFinish = true;
                            y++;
                        }
                    } else {
                        int x = c;
                        while (s.board[r][x].isTarget){
                            s.board[r][s.reqPosition + c - x].isFinish = true;
                            x++;
                        }
                    }
                    found = true;
                }
                if (found) {
                    break;
                }
            }
            if (found) {
                break;
            }
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
        s.carIndex    = carIndex;
        s.reqPosition = reqPosition;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                CellState cs = new CellState(board[r][c].value);
                cs.isTarget = board[r][c].isTarget;
                cs.isFinish = board[r][c].isFinish;
                s.board[r][c] = cs;
            }
        }
        return s;
    }

    /** Pokus o tah - pravidla (hranice, kolizie, povoleny smer) podla zadania. */
    public boolean tryMove(int carID, int dr, int dc) {

        if (dr != 0 && !carR(carID)){
            return false;
        }
        if (dc != 0 && !carC(carID)){
            return false;
        }

        if (dr == 1 && canMoveRplus(carID)){
            moveCarDown(carID);
            return true;
        }

        if (dr == -1 && canMoveRminus(carID)){
            moveCarUp(carID);
            return true;
        }

        if (dc == 1 && canMoveCplus(carID)){
            moveCarRight(carID);
            return true;
        }

        if (dc == -1 && canMoveCminus(carID)){
            moveCarLeft(carID);
            return true;
        }

        return false;
    }

    public void moveCarDown(int carID) {
        for (int r = rows - 1; r > 0; r--) {
            for (int c = 0; c < cols; c++) {
                if (board[r-1][c].value == carID) {
                    board[r][c].value = carID;
                    board[r][c].isTarget = board[r-1][c].isTarget;
                } else {
                    if (board[r][c].value == carID) {
                        board[r][c].value = 0;
                        board[r][c].isTarget = false;
                    }
                }
            }
        }
        for (int c = 0; c < cols; c++) {
            if (board[0][c].value == carID) {
                board[0][c].value = 0;
                board[0][c].isTarget = false;
            }
        }
    }

    public void moveCarUp(int carID) {
        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r+1][c].value == carID) {
                    board[r][c].value = carID;
                    board[r][c].isTarget = board[r+1][c].isTarget;
                } else {
                    if (board[r][c].value == carID) {
                        board[r][c].value = 0;
                        board[r][c].isTarget = false;
                    }
                }
            }
        }
        for (int c = 0; c < cols; c++) {
            if (board[rows-1][c].value == carID) {
                board[rows-1][c].value = 0;
                board[rows-1][c].isTarget = false;
            }
        }
    }

    public void moveCarRight(int carID) {
        for (int r = 0; r < rows; r++) {
            for (int c = cols - 1; c > 0; c--) {
                if (board[r][c-1].value == carID) {
                    board[r][c].value = carID;
                    board[r][c].isTarget = board[r][c-1].isTarget;
                } else {
                    if (board[r][c].value == carID) {
                        board[r][c].value = 0;
                        board[r][c].isTarget = false;
                    }
                }
            }
        }
        for (int r = 0; r < rows; r++) {
            if (board[r][0].value == carID) {
                board[r][0].value = 0;
                board[r][0].isTarget = false;
            }
        }
    }

    public void moveCarLeft(int carID) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols - 1; c++) {
                if (board[r][c+1].value == carID) {
                    board[r][c].value = carID;
                    board[r][c].isTarget = board[r][c+1].isTarget;
                } else {
                    if (board[r][c].value == carID) {
                        board[r][c].value = 0;
                        board[r][c].isTarget = false;
                    }
                }
            }
        }
        for (int r = 0; r < rows; r++) {
            if (board[r][cols-1].value == carID) {
                board[r][cols-1].value = 0;
                board[r][cols-1].isTarget = false;
            }
        }
    }

    public boolean canMoveRplus(int carID){
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (board[r][c].value == carID){
                    if (r == rows - 1){
                        return false;
                    } else {
                        if (!(board[r+1][c].value == carID || board[r+1][c].value == 0)){
                            return false;
                        }
                    }
                }
        return true;
    }

    public boolean canMoveRminus(int carID){
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (board[r][c].value == carID){
                    if (r == 0){
                        return false;
                    } else {
                        if (!(board[r-1][c].value == carID || board[r-1][c].value == 0)){
                            return false;
                        }
                    }
                }
        return true;
    }

    public boolean canMoveCplus(int carID){
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (board[r][c].value == carID){
                    if (c == cols - 1){
                        return false;
                    } else {
                        if (!(board[r][c+1].value == carID || board[r][c+1].value == 0)){
                            return false;
                        }
                    }
                }
        return true;
    }

    public boolean canMoveCminus(int carID){
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (board[r][c].value == carID){
                    if (c == 0){
                        return false;
                    } else {
                        if (!(board[r][c-1].value == carID || board[r][c-1].value == 0)){
                            return false;
                        }
                    }
                }
        return true;
    }

    public boolean carR(int carID) {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (board[r][c].value == carID){
                    if (r < rows - 1 && board[r+1][c].value == carID){
                        return true;
                    } else {
                        return false;
                    }
                }
        return false;
    }

    public boolean carC(int carID) {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (board[r][c].value == carID){
                    if (c < cols - 1 && board[r][c+1].value == carID){
                        return true;
                    } else {
                        return false;
                    }
                }
        return false;
    }

    /** Ci je hra dohrana - podmienka podla zadania. */
    public boolean isFinished() {
        for  (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c].isTarget != board[r][c].isFinish) {
                    return false;
                }
            }
        }
        return true;
    }
}
