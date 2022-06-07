package sample;

import javafx.util.Pair;

import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;


public class GameFile {

    static class IntPair{
        final int x;
        final int y;
        IntPair(int x, int y){this.x = x; this.y = y;}
    }

    private GameSupervisor supervisor = null;

    private Square graveyard = new Square(-1,-1);

    private Piece whiteKing = null;
    private Piece blackKing = null;
    private Army whitePieces = null;
    private Army blackPieces = null;

    GameFile(GameSupervisor supervisor, Piece whiteKing, Piece blackKing, Army whitePieces, Army blackPieces){
        this.supervisor = supervisor;
        this.whiteKing = whiteKing;
        this.blackKing = blackKing;
        this.whitePieces = whitePieces;
        this.blackPieces = blackPieces;
    }

    private IntPair squareDecryptor(char oldX, char oldY){
        int x = oldX - 97;
        int y = 8 - (oldY - 48);
        return new IntPair(x,y);
    }

    private int intDecryptor(char oldX){
        return oldX - 97;
    }

    private PieceKind kindDecryptor(char c){
        PieceKind k = PieceKind.PAWN;
        switch (c){
            case 'Q' -> k = PieceKind.QUEEN;
            case 'N' -> k = PieceKind.KNIGHT;
            case 'R' -> k = PieceKind.ROOK;
            case 'B' -> k = PieceKind.BISHOP;
        }
        return k;
    }

    private void movePawn(char[] ch, PieceKind promotion, boolean bitka){
        IntPair pair = squareDecryptor(ch[ch.length-2], ch[ch.length-1]);
        Note note2 = supervisor.board[pair.x][pair.y].getPiece() == null ? null : new Note(supervisor.board[pair.x][pair.y].getPiece(), pair.x, pair.y, -1,-1);
        int fromY = supervisor.counter % 2 == 0 ? pair.y +1 : pair.y-1;
        int fromX;
        if(bitka){
            fromX = ch[0] < ch[ch.length-2] ? pair.x-1 : pair.x+1;
            if(supervisor.board[pair.x][pair.y].getPiece() == null){ //en passant
                Army arm = supervisor.counter % 2 == 0 ? blackPieces : whitePieces;
                note2 = new Note(supervisor.board[pair.x][fromY].getPiece(), pair.x, pair.y, -1,-1);
                arm.kill(supervisor.board[pair.x][fromY].getPiece());
                supervisor.board[pair.x][fromY].setPiece(null);
            }
        } else{
            fromX = pair.x;
            fromY = supervisor.board[pair.x][fromY].getPiece() != null ? fromY : fromY + fromY - pair.y;
        }
        Note note = new Note(supervisor.board[fromX][fromY].getPiece(),fromX,fromY, pair.x, pair.y);
        Pair<Note,Note> annotation = new Pair(note, note2);
        if(note2 != null){
            if(supervisor.board[pair.x][pair.y].getPiece().getColour() == PieceColour.BLACK){
                blackPieces.kill(note2.getPiece());
            }else{
                whitePieces.kill(note2.getPiece());
            }
        }
        Piece piece = supervisor.board[fromX][fromY].getPiece();
        piece.move(pair.x, pair.y);
        if(promotion != PieceKind.PAWN) {
            piece.promote(promotion);
            piece.setPromotionMoveNumber(supervisor.realSize() + 1);
        }
        supervisor.board[pair.x][pair.y].setPiece(supervisor.board[fromX][fromY].getPiece());
        supervisor.board[fromX][fromY].setPiece(null);
        supervisor.add(annotation,false,0, false);
    }

    private void movePiece(char[] ch, int conflict, PieceKind kind){
        IntPair pair = squareDecryptor(ch[ch.length-2], ch[ch.length-1]);
        Note note2 = supervisor.board[pair.x][pair.y].getPiece() == null ? null : new Note(supervisor.board[pair.x][pair.y].getPiece(), pair.x, pair.y, -1,-1);
        Piece piece = new Piece(supervisor.counter % 2 == 1 ? PieceColour.WHITE : PieceColour.BLACK,
                kind, pair.x, pair.y, supervisor.board, null);
        piece.setOurKing(supervisor.counter % 2 == 1 ? whiteKing : blackKing);
        piece.setEnemyArmy(supervisor.counter % 2 == 1 ? blackPieces : whitePieces);
        piece.findPossibleMoves(0,false, true);
        boolean theOne = false;
        for(Move move : piece.getPossibleMoves()){
            Piece temp = supervisor.board[move.getX()][move.getY()].getPiece();
            if(move.type == MoveType.KILL && temp.getKind() == kind){
                if (conflict == 0){
                    theOne = true;
                }else if((conflict == 1 && intDecryptor(ch[1]) == move.getX()) || (conflict == 2 && (int)ch[1] - 40 == move.getY())){
                    theOne = true;
                }
            }
            if(theOne) {
                int fromX = move.getX();
                int fromY = move.getY();
                piece = null;
                Piece piece1 = supervisor.board[fromX][fromY].getPiece();
                Note note = new Note(piece1, fromX, fromY, pair.x, pair.y);
                piece1.move(pair.x, pair.y);
                Pair<Note, Note> p = new Pair(note, note2);
                supervisor.add(p, false, 0, false);
                if(note2 != null){
                    if(piece1.getColour() == PieceColour.BLACK){
                        whitePieces.kill(note2.getPiece());
                    }else{
                        blackPieces.kill(note2.getPiece());
                    }
                }
                supervisor.board[pair.x][pair.y].setPiece(piece1);
                supervisor.board[fromX][fromY].setPiece(null);
                break;
            }
        }
    }

    private void moveKing(char[] ch){
        IntPair pair = squareDecryptor(ch[ch.length-2], ch[ch.length-1]);
        Piece piece =  supervisor.counter % 2 == 0 ? whiteKing : blackKing;
        Square from = piece.getCoordinates();
        int fromX = from.getX();
        int fromY = from.getY();
        Note note2 = supervisor.board[pair.x][pair.y].getPiece() == null ? null : new Note(supervisor.board[pair.x][pair.y].getPiece(), pair.x, pair.y, -1,-1);
        Note note = new Note(piece, fromX, fromY, pair.x, pair.y);
        Pair<Note, Note> p = new Pair(note, note2);
        if(note2 != null){
            if(piece.getColour() != PieceColour.BLACK){
                blackPieces.kill(note2.getPiece());
            }else{
                whitePieces.kill(note2.getPiece());
            }
        }
        piece.move(pair.x, pair.y);
        supervisor.add(p,false,0, false);
        supervisor.board[pair.x][pair.y].setPiece(piece);
        supervisor.board[fromX][fromY].setPiece(null);
    }

    private void castle (char[] ch){
        boolean sup = supervisor.counter % 2 == 0;
        if(sup){
            if(ch.length == 3){

            }
        }
    }

    private void resolveMove(String move){
        move = move.replace("+", "").replace("=", "");
        char[] ch = move.toCharArray();
        int conflict = 0;
        if(!(move.contains("x")) && move.length() >3){
            conflict = Character.isLowerCase(ch[1]) ? 1 : 2;
        }else if(move.length() > 4){
            conflict = Character.isLowerCase(ch[1]) ? 1 : 2;
        }
        if(Character.isLowerCase(ch[0])){ //pawn
            PieceKind prom = kindDecryptor(move.charAt(move.length() - 1));
            if(Character.isUpperCase(ch[ch.length-1])) {
                move = move.substring(0, move.length() - 1);
                ch = move.toCharArray();
            }
           movePawn(ch, prom,move.contains("x"));
        }else{
            switch(ch[0]){
                case 'N' -> movePiece(ch,conflict, PieceKind.KNIGHT);
                case 'Q' -> movePiece(ch,conflict, PieceKind.QUEEN);
                case 'K' -> moveKing(ch);
                case 'R' -> movePiece(ch,conflict, PieceKind.ROOK);
                case 'B' -> movePiece(ch,conflict, PieceKind.BISHOP);
                case '0' -> castle(ch);
                case 'O' -> castle(ch);
            }
        }
    }

    public void readMoves(String filePath){
        try(FileReader reader = new FileReader(filePath)){
            int sup = 100;
            Scanner sc = new Scanner(reader);
            for(int i = 0; sc.hasNext(); i++){
                String line = sc.next();
                if(i%3 != 0) {
                    System.out.println(supervisor.counter + " " + line);
                    resolveMove(line);
                    if(supervisor.counter == sup){
                        System.out.println("CO JEST KURWA");
                    }
                }
                sup = supervisor.counter;
            }
            supervisor.counter--;
        }catch(IOException err){
            System.err.format("IOException: %s%n", err);
        }
    }

}