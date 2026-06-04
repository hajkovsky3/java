package com.example.skuska2;

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

        public CellState(int value) {
            this.value = value;
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
        int h = lines.toArray().length;
        int w = lines.getFirst().split("\\s+").length;

        //System.out.println(h + " " + w);
        GameState s = new GameState(h, w);

        for (int r = 0; r < h; r++){
            for  (int c = 0; c < w; c++){
                int value = Integer.parseInt(lines.get(r).trim().split("\\s+")[c]);
                System.out.println(r + " " + c + " -> " + value);
                CellState cs = new CellState(value);
                s.board[r][c] = cs;
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
    public boolean tryMove(int selectedBlockID, int dr, int dc) {
        if (canMove(selectedBlockID, dr, dc)){
            System.out.println("canMove " +  selectedBlockID + " " + dr + " " + dc);
            move(selectedBlockID, dr, dc);
            moveCount++;
            return true;
        } else {
            if (canRemove(selectedBlockID, dr, dc)) {
                System.out.println("canRemove " +  selectedBlockID + " " + dr + " " + dc);
                remove(selectedBlockID);
                moveCount++;
                return true;
            }
        }
        System.out.println("cannot move nor remove " +  selectedBlockID + " " + dr + " " + dc);
        return false;
    }

    public void move(int selectedBlockID, int dr, int dc){
        CellState[][] newBoard = new CellState[rows][cols];
        // kopirovanie okrajov
        for (int r = 0; r < rows; r++) {
            newBoard[r][0] =  new CellState(board[r][0].value);
            newBoard[r][cols-1] =  new CellState(board[r][cols-1].value);
        }
        for (int c = 0; c < cols; c++) {
            newBoard[0][c] =  new CellState(board[0][c].value);
            newBoard[rows-1][c] =  new CellState(board[rows-1][c].value);
        }
        // bez momentalneho blockID
        for (int r = 1; r < rows - 1; r++){
            for (int c = 1; c < cols - 1; c++){
                if (board[r][c].value == selectedBlockID){
                    newBoard[r][c] = new CellState(0);
                } else {
                    newBoard[r][c] = new CellState(board[r][c].value);
                }
            }
        }
        // blockID na novom mieste
        for (int r = 1; r < rows - 1; r++){
            for (int c = 1; c < cols - 1; c++){
                if (board[r][c].value == selectedBlockID){
                    newBoard[r+dr][c+dc].value = selectedBlockID;
                }
            }
        }

        board = newBoard;
    }

    public void remove(int selectedBlockID) {
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                if  (board[r][c].value == selectedBlockID) {
                    board[r][c].value = 0;
                }
            }
        }
    }

    public boolean canMove(int selectedBlockID, int dr, int dc) {
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                if (board[r][c].value == selectedBlockID) {
                    if (dr == 1) {
                        if (r == rows - 2) {return false;}
                        if (board[r+1][c].value != selectedBlockID && board[r+1][c].value != 0) {return false;}
                    }
                    else if (dr == -1) {
                        if (r == 1) {return false;}
                        if (board[r-1][c].value != selectedBlockID && board[r-1][c].value != 0) {return false;}
                    }
                    else if (dc == 1) {
                        if (c == cols - 2) {return false;}
                        if (board[r][c+1].value != selectedBlockID && board[r][c+1].value != 0) {return false;}
                    }
                    else if (dc == -1) {
                        if (c == 1) {return false;}
                        if (board[r][c-1].value != selectedBlockID && board[r][c-1].value != 0) {return false;}
                    }
                }
            }
        }
        return true;
    }

    public boolean canRemove(int selectedBlockID, int dr, int dc){
        int minInt = 1000, maxInt = -1000;

        if (dr == 1) {
            for (int c = 0; c < cols; c++) {
                if (board[rows-1][c].value == selectedBlockID) {
                    if (c < minInt) {minInt = c;}
                    if (c > maxInt) {maxInt = c;};
                }
            }
            if (minInt <= maxInt) {
                for (int r = 1; r < rows-1; r++) {
                    for (int c = 1; c < cols-1; c++) {
                        if (board[r][c].value == selectedBlockID && (c < minInt || c > maxInt)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        } else if (dr == -1) {
            for (int c = 0; c < cols; c++) {
                if (board[0][c].value == selectedBlockID) {
                    if (c < minInt) {minInt = c;}
                    if (c > maxInt) {maxInt = c;}
                }
            }
            if (minInt <= maxInt) {
                for (int r = 1; r < rows-1; r++) {
                    for (int c = 1; c < cols-1; c++) {
                        if (board[r][c].value == selectedBlockID && (c < minInt || c > maxInt)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        } else if (dc == 1) {
            for (int r = 0; r < rows; r++) {
                if (board[r][cols - 1].value == selectedBlockID) {
                    if (r < minInt) {minInt = r;}
                    if (r > maxInt) {maxInt = r;}
                }
            }
            if (minInt <= maxInt) {
                for (int r = 1; r < rows-1; r++) {
                    for (int c = 1; c < cols-1; c++) {
                        if (board[r][c].value == selectedBlockID && (r < minInt || r > maxInt)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        } else if (dc == -1) {
            for (int r = 0; r < rows; r++) {
                if (board[r][0].value == selectedBlockID) {
                    if (r < minInt) {minInt = r;}
                    if (r > maxInt) {maxInt = r;}
                }
            }
            if (minInt <= maxInt) {
                for (int r = 1; r < rows-1; r++) {
                    for (int c = 1; c < cols-1; c++) {
                        if (board[r][c].value == selectedBlockID && (r < minInt || r > maxInt)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }


    /** Ci je hra dohrana - podmienka podla zadania. */
    public boolean isFinished() {
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                if (board[r][c].value != 0){
                    return false;
                }
            }
        }
        return true;
    }
}
