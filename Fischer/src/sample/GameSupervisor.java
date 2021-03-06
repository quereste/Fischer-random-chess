package sample;

import javafx.util.Pair;

import java.util.ArrayList;

public class GameSupervisor {
    ArrayList<Pair<Note, Note>> game;
    int counter = 0;
    Tile [] [] board;

    String pgnNotation = "";

    GameSupervisor(Tile [][] board) {
        this.board = board;
        game = new ArrayList<>();
    }

    private char getShort(PieceKind k){
        char ch = '\0';
        switch (k){
            case KING -> ch = 'K';
            case ROOK -> ch = 'R';
            case QUEEN -> ch = 'Q';
            case BISHOP -> ch = 'B';
            case KNIGHT -> ch = 'N';
        }
        return ch;
    }

    private void pgnSaver(Pair<Note, Note> note, int pieceDuplication, Move result){
        if(game.size() % 2 == 1){
            pgnNotation += (game.size()/2 + 1 + ". ");
        }
        if(result.type == MoveType.CASTLE_KINGSIDE){
            pgnNotation += "0-0 ";
            return;
        }
        if(result.type == MoveType.CASTLE_QUEENSIDE){
            pgnNotation += "0-0-0 ";
            return;
        }
        if(note.getKey().piece.getKind() !=  PieceKind.PAWN){
            char c = getShort(note.getKey().piece.getKind());
            pgnNotation += c;
            if (pieceDuplication == 1){
                pgnNotation += (Main.SQUARE_NUMBER-note.getKey().from.getY());
            }
            if (pieceDuplication == 2){
                pgnNotation += note.getKey().from.getXasChar();
            }
        }
        if(result.type == MoveType.KILL || result.type == MoveType.KILL_PROMOTION){
            if(note.getKey().piece.getKind() ==  PieceKind.PAWN) {
                pgnNotation += note.getKey().from.getXasChar();
            }
            pgnNotation += "x";
        }
        pgnNotation += note.getKey().to.getXasChar();
        pgnNotation += (Main.SQUARE_NUMBER-note.getKey().to.getY());
        if(result.type == MoveType.KILL_PROMOTION || result.type == MoveType.SIMPLE_PROMOTION){
            pgnNotation += "=" + getShort(note.getKey().piece.getKindAfterPromotion());
        }
        pgnNotation += " ";
    }
    public void add(Pair<Note, Note> note, int pieceDuplication, Move result) {
        game.add(note);
        if(result != null) {
            pgnSaver(note, pieceDuplication, result);
        }
        counter++;
    }

    public String toString() {
        int limit = game.size();
        String result = "";
        for (int i = 0; i < limit; i++) {
            result += ((i+1) + ". " + game.get(i).getKey().from.getX() + game.get(i).getKey().from.getY()) + " ";
        }
        return result;
    }


    public int realSize() {
        return game.size() - 1;
    }

    public boolean anyMovesMade() {
        return game.size() != 0;
    }
    public void setCounter() {
        counter = game.size() - 1;
    }

    public void retraceMove() {
        if (counter >= 0) {
            Note noteOne = game.get(counter).getKey();
            Note noteTwo = game.get(counter).getValue();
            if (noteOne != null) {
                noteOne.piece.move(noteOne.from.getX(), noteOne.from.getY());
                board[noteOne.from.getX()][noteOne.from.getY()].setPiece(noteOne.piece);
                if (noteOne.to.getX() != -1) {
                    board[noteOne.to.getX()][noteOne.to.getY()].setPiece(null);
                }
            }
            if (noteTwo != null) {
                noteTwo.piece.move(noteTwo.from.getX(), noteTwo.from.getY());
                board[noteTwo.from.getX()][noteTwo.from.getY()].setPiece(noteTwo.piece);
            }
            if (counter == noteOne.piece.getPromotionMoveNumber()) {

                noteOne.piece.setKind(PieceKind.PAWN);
            }
            counter--;
        }
    }

    public boolean repeatMove() {
        if (counter + 1 <= realSize()) {
            counter++;
            Note noteOne = game.get(counter).getKey();
            Note noteTwo = game.get(counter).getValue();
            if (noteTwo != null) {
                noteTwo.piece.move(noteTwo.to.getX(), noteTwo.to.getY());
                board[noteTwo.from.getX()][noteTwo.from.getY()].setPiece(null);
                if (noteTwo.to.getX() != -1) {
                    board[noteTwo.to.getX()][noteTwo.to.getY()].setPiece(noteTwo.piece);

                }
            }
            noteOne.piece.move(noteOne.to.getX(), noteOne.to.getY());
            board[noteOne.to.getX()][noteOne.to.getY()].setPiece(noteOne.piece);
            if (noteOne.from.getX() != -1) {
                board[noteOne.from.getX()][noteOne.from.getY()].setPiece(null);
            }
            if (counter == noteOne.piece.getPromotionMoveNumber()) {
                noteOne.piece.promote(noteOne.piece.getKindAfterPromotion());
            }

        }
        return counter == realSize();
    }

}