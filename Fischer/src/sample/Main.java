package sample;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

public class Main extends Application {

    public static final int TILE_SIZE = 80;
    public static final int SQUARE_NUMBER = 8;
    public static final int PIECE_SIZE = 80;
    public static final int PROMOTION_BUTTON_SIZE = TILE_SIZE / 2;

    public Tile [][] board = new Tile [SQUARE_NUMBER][SQUARE_NUMBER];

    private Group tiles = new Group();
    private Group pieces = new Group();

    Army whiteAlivePieces = new Army();
    Army darkAlivePieces = new Army();

    Clock whiteClock = null;
    Clock darkClock = null;

    boolean doubleCheck = false;
    boolean check = false;
    int enPassantXPossibility;

    Square graveyard = null;

    Piece whiteKing = null;
    Piece darkKing = null;

    TurnIndicator onMove = null;
    GameSupervisor gameSupervisor = null;
    StageManager stageManager = null;

    private Pane right = null;
    private HBox promotionBox = null;

    public static final int xKingCastleKingSide = 6;
    public static final int xRookCastleKingSide = 5;

    public static final int xKingCastleQueenSide = 2;
    public static final int xRookCastleQueenSide = 3;

    public static final int yWhite = 7;
    public static final int yDark = 0;

    public static TimeControl timeControl = TimeControl.NONE;

    public static Color darkTileColour = Color.TOMATO;
    public static Color lightTileColour = Color.WHITESMOKE;
    public static String graphicFolder = "setOne";

    public Parent createContent(boolean FischerOnes, boolean gameFromFile, Stage stage) {
        clearData();
        BorderPane root = new BorderPane();
        Pane center = new Pane();
        Pane right = new Pane();
        root.setRight(right);
        root.setCenter(center);

        if (graphicFolder.equals("setTwo")) {
            darkTileColour = Color.LIGHTGREEN;
        } else {
            darkTileColour = Color.TOMATO;
        }

        this.right = right;

        center.setPrefSize(TILE_SIZE * SQUARE_NUMBER, TILE_SIZE * SQUARE_NUMBER);
        center.getChildren().addAll(tiles, pieces);

        right.setPrefSize(TILE_SIZE * 3, TILE_SIZE * SQUARE_NUMBER);

        if (gameFromFile) {
            Button goBack = new Button("Go back to Menu");
            goBack.setPrefSize(2 * PIECE_SIZE, PIECE_SIZE / 2);
            goBack.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    stageManager.OpenMainMenu();
                }
            });
            goBack.relocate((TILE_SIZE * 3 - 2 * PIECE_SIZE) / 2, TILE_SIZE * 3);
            right.getChildren().add(goBack);
        } else {

            Button draw = new Button("Take draw");
            Button resign = new Button("Resign");

            draw.setPrefSize(PIECE_SIZE, PIECE_SIZE / 2);
            resign.setPrefSize(PIECE_SIZE, PIECE_SIZE / 2);
            draw.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    stageManager.OpenEndingScene(Result.DRAW, gameSupervisor);
                }
            });

            resign.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    stageManager.OpenEndingScene(onMove.getPieceColour() == PieceColour.WHITE ? Result.BLACK : Result.WHITE, gameSupervisor);
                }
            });

            HBox manualEndings = new HBox(draw, resign);
            manualEndings.relocate((TILE_SIZE * 3 - 2 * PIECE_SIZE) / 2, TILE_SIZE * 4);
            right.getChildren().add(manualEndings);
        }
        Button backwardReview = new Button("Back");
        Button forwardReview = new Button("Forward");

        backwardReview.setPrefSize(PIECE_SIZE, PIECE_SIZE);
        forwardReview.setPrefSize(PIECE_SIZE, PIECE_SIZE);

        backwardReview.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (gameSupervisor.anyMovesMade()) {
                    if (onMove.getGameMode()) {
                        onMove.switchMode();
                        gameSupervisor.setCounter();
                    }
                    gameSupervisor.retraceMove();
                }
            }
        });

        forwardReview.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (onMove.getGameMode()) {

                } else {
                    if(gameSupervisor.repeatMove()) {
                        onMove.switchMode();
                    }
                }
            }
        });

        HBox gameReview = new HBox(backwardReview, forwardReview);

        gameReview.relocate((TILE_SIZE * 3 - 2 * PIECE_SIZE) / 2, TILE_SIZE * 5);
        right.getChildren().add(gameReview);

        ExCoordinates positionCreator = new ExCoordinates(FischerOnes);

        onMove = new TurnIndicator(true);
        gameSupervisor = new GameSupervisor(board);
        graveyard = new Square(-1, -1);
        enPassantXPossibility = -2;

        for (int y = 0; y < SQUARE_NUMBER; y++) {
            for (int x = 0; x < SQUARE_NUMBER; x++) {
                Tile tile = new Tile((y + x) % 2 != 0, x, y);
                tiles.getChildren().add(tile);
                board [x] [y] = tile;

                if (yOfStartingPiecePosition(y)) {
                    addPieceToBoard(positionCreator, x, y, stage, tile);
                }

            }
        }

        darkKing = board [positionCreator.yKing()] [0].getPiece();
        whiteKing = board [positionCreator.yKing()] [7].getPiece();

        whiteAlivePieces.addOurKing(whiteKing);
        darkAlivePieces.addOurKing(darkKing);

        onMove.findRooksAndKings(board [positionCreator.yLRook()] [yWhite].getPiece(), board [positionCreator.yKing()] [yWhite].getPiece(), board [positionCreator.yRRook()] [yWhite].getPiece(),
                board [positionCreator.yLRook()] [yDark].getPiece(), board [positionCreator.yKing()] [yDark].getPiece(), board [positionCreator.yRRook()] [yDark].getPiece());

        whiteAlivePieces.addEnemyArmy(darkAlivePieces);
        darkAlivePieces.addEnemyArmy(whiteAlivePieces);

        whiteAlivePieces.makeThemReady(enPassantXPossibility);



        if (timeControl != TimeControl.NONE) {
            whiteClock = new Clock(timeControl, gameSupervisor, stage);
            darkClock = new Clock(timeControl, gameSupervisor, stage);

            darkClock.getTimer().relocate((TILE_SIZE * 3 - whiteClock.getWidth()) / 2, TILE_SIZE / 2);
            whiteClock.getTimer().relocate((TILE_SIZE * 3 - darkClock.getWidth()) / 2, TILE_SIZE * SQUARE_NUMBER - 3 * TILE_SIZE / 2);

            Button addTime = new Button("Add 10 sec");
            addTime.setPrefSize(2 * PIECE_SIZE, PIECE_SIZE / 2);
            addTime.relocate((TILE_SIZE * 3 - 2 * PIECE_SIZE) / 2, 3 * TILE_SIZE);
            addTime.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if (onMove.getPieceColour() == PieceColour.BLACK) {
                        darkClock.addTime(10);
                    } else {
                        whiteClock.addTime(10);
                    }
                }
            });

            right.getChildren().addAll(whiteClock.getTimer(), darkClock.getTimer(), addTime);
            whiteClock.play();
        }

        return root;
    }

    public boolean yOfStartingPiecePosition(int y) {
        return y == 0 || y == 1 || y == 6 || y == 7;
    }
    public void addPieceToBoard(ExCoordinates positionCreator, int x, int y, Stage stage, Tile tile) {
        PieceColour colour = (y == 6 || y == 7) ? PieceColour.WHITE : PieceColour.BLACK;
        PieceKind kind = (y == 1 || y == 6) ? PieceKind.PAWN : positionCreator.retrieveWhichPiece(x);
        Army army = (colour == PieceColour.WHITE) ? whiteAlivePieces : darkAlivePieces;
        Piece piece = makePiece(colour, kind, x, y, board, stage);
        tile.setPiece(piece);
        pieces.getChildren().add(piece);
        army.hire(piece);
    }

    private Move tryMove(Piece piece, int newX, int newY) {
        if (isOnMove(piece)) {
            Square moveCandidate = new Square(newX, newY);
            return piece.isMoveLegit(moveCandidate);
        } else {
            return null;
        }
    }

    private int conversionToSquareMiddle(double pixel) {
        return (int)(pixel + TILE_SIZE / 2) / TILE_SIZE;
    }

    private Piece makePiece(PieceColour colour, PieceKind kind, int x, int y, Tile [] [] board, Stage stage) {

        Piece piece = new Piece(colour, kind, x, y, board, onMove);

        piece.setOnMouseReleased(e -> {
            int newX = conversionToSquareMiddle(piece.getLayoutX());
            int newY = conversionToSquareMiddle(piece.getLayoutY());

            int oldX = conversionToSquareMiddle(piece.getOldX());
            int oldY = conversionToSquareMiddle(piece.getOldY());

            int pieceDuplication;

            Move result = tryMove(piece, newX, newY);
            if (result != null) {
                if (result.getType() == MoveType.CASTLE_KINGSIDE) {
                    newX = xKingCastleKingSide;
                } else if (result.getType() == MoveType.CASTLE_QUEENSIDE) {
                    newX = xKingCastleQueenSide;
                }
                boolean take = board[newX][newY].getPiece() != null;
                boolean promotion = false;
                pieceDuplication = 0;
                for(int i = 0; i < 8; ++i){
                    if (i != oldY && board[oldX][i].getPiece() != null &&
                            (board[oldX][i].getPiece().getKind() == piece.getKind() && board[oldX][i].getPiece().getColour() == piece.getColour())){
                        pieceDuplication = 1;
                    }
                    if ( i != oldX && board[i][oldY].getPiece() != null &&
                            board[i][oldY].getPiece().getKind() == piece.getKind() && board[i][oldY].getPiece().getColour() == piece.getColour()){
                        pieceDuplication = 2;
                    }
                }
                Note firstMoved = new Note(piece, oldX, oldY, newX, newY);
                Note secondMoved = null;
                if (result.type == MoveType.PAWN_DOUBLE_MOVE) {
                    enPassantXPossibility = newX;
                } else {
                    enPassantXPossibility = -2;
                }

                int xOfCastlingRook = 0;

                if (result.type == MoveType.KILL) {
                    Army whichArmy = (onMove.getPieceColour() == PieceColour.WHITE) ? darkAlivePieces : whiteAlivePieces;
                    secondMoved = new Note(board [newX] [newY].getPiece(), newX, newY, graveyard.getX(), graveyard.getY());
                    board [newX] [newY].getPiece().controlCastlePossibilities();
                    whichArmy.kill(board [newX] [newY].getPiece());

                } else if (result.type == MoveType.EN_PASSANT) {
                    Army whichArmy = (onMove.getPieceColour() == PieceColour.WHITE) ? darkAlivePieces : whiteAlivePieces;
                    int yOfVictim = (onMove.getPieceColour() == PieceColour.WHITE) ? newY + 1 : newY - 1;
                    secondMoved = new Note(board [newX] [yOfVictim].getPiece(), newX, newY, graveyard.getX(), graveyard.getY());
                    board [newX] [yOfVictim].getPiece().controlCastlePossibilities();
                    whichArmy.kill(board [newX] [yOfVictim].getPiece());
                } else if (result.type == MoveType.SIMPLE_PROMOTION) {
                    Pair<Note, Note> annotation = new Pair(firstMoved, secondMoved);
                    promotionBox = promotionHBox(piece, stage, annotation, pieceDuplication, result);

                    right.getChildren().add(promotionBox);
                    promotionBox.relocate(TILE_SIZE / 2, 2 * TILE_SIZE);
                    piece.setPromotionMoveNumber(gameSupervisor.realSize() + 1);

                    onMove.switchMode();
                    promotion = true;
                } else if (result.type == MoveType.KILL_PROMOTION) {
                    Army whichArmy = (onMove.getPieceColour() == PieceColour.WHITE) ? darkAlivePieces : whiteAlivePieces;
                    secondMoved = new Note(board [newX] [newY].getPiece(), newX, newY, graveyard.getX(), graveyard.getY());
                    board [newX] [newY].getPiece().controlCastlePossibilities();
                    whichArmy.kill(board [newX] [newY].getPiece());
                    Pair<Note, Note> annotation = new Pair(firstMoved, secondMoved);
                    promotionBox = promotionHBox(piece, stage, annotation, pieceDuplication, result);
                    right.getChildren().add(promotionBox);
                    promotionBox.relocate(TILE_SIZE / 2, 2 * TILE_SIZE);
                    piece.setPromotionMoveNumber(gameSupervisor.realSize() + 1);

                    onMove.switchMode();
                    promotion = true;
                } else if (result.getType() == MoveType.CASTLE_KINGSIDE) {
                    Piece rook = (piece.getColour() == PieceColour.WHITE) ? onMove.getWhiteRightRook() : onMove.getDarkRightRook();
                    board [rook.getCoordinates().getX()] [rook.getCoordinates().getY()].setPiece(null);
                    secondMoved = new Note(rook, rook.getCoordinates().getX(), rook.getCoordinates().getY(),
                            xRookCastleKingSide, (piece.getColour() == PieceColour.WHITE) ? yWhite : yDark);
                    rook.move(xRookCastleKingSide, (piece.getColour() == PieceColour.WHITE) ? yWhite : yDark);
                    board [rook.getCoordinates().getX()] [rook.getCoordinates().getY()].setPiece(rook);
                    xOfCastlingRook = rook.getCoordinates().getX();
                } else if (result.getType() == MoveType.CASTLE_QUEENSIDE) {
                    Piece rook = (piece.getColour() == PieceColour.WHITE) ? onMove.getWhiteLeftRook() : onMove.getDarkLeftRook();
                    board [rook.getCoordinates().getX()] [rook.getCoordinates().getY()].setPiece(null);
                    secondMoved = new Note(rook, rook.getCoordinates().getX(), rook.getCoordinates().getY(),
                            xRookCastleQueenSide, (piece.getColour() == PieceColour.WHITE) ? yWhite : yDark);
                    rook.move(xRookCastleQueenSide, (piece.getColour() == PieceColour.WHITE) ? yWhite : yDark);
                    board [rook.getCoordinates().getX()] [rook.getCoordinates().getY()].setPiece(rook);
                    xOfCastlingRook = rook.getCoordinates().getX();
                }

                piece.move(newX, newY);
                if (!((result.getType() == MoveType.CASTLE_KINGSIDE || result.getType() == MoveType.CASTLE_QUEENSIDE) && xOfCastlingRook == oldX)) {
                    board[oldX][oldY].setPiece(null);
                }
                board[newX][newY].setPiece(piece);



                piece.controlCastlePossibilities();
                if (result.getType() != MoveType.SIMPLE_PROMOTION && result.getType() != MoveType.KILL_PROMOTION) {
                    Pair<Note, Note> annotation = new Pair(firstMoved, secondMoved);
                    gameSupervisor.add(annotation, pieceDuplication ,result);
                    piece.repaint();
                    postMoveAction(stage);
                }

            } else {
                piece.repaint();
                piece.doNotMove();
            }


        });
        return piece;
    }

    public boolean isOnMove(Piece piece) {
        return onMove.getGameMode() && piece.getColour() == onMove.getPieceColour();
    }


    public void postMoveAction(Stage stage) {
        if (onMove.getPieceColour() == PieceColour.WHITE) {
            boolean isThereACheck = whiteAlivePieces.lookForChecks(-2, darkKing.getCoordinates());


            darkAlivePieces.makeThemReady(enPassantXPossibility);
            if (darkAlivePieces.doTheyHaveAnyMoves()) {
                if (timeControl != TimeControl.NONE) {
                    whiteClock.stop();
                    darkClock.play();
                }
            } else {
                Result finalResult = isThereACheck ? Result.WHITE : Result.DRAW;
                stageManager.OpenEndingScene(finalResult, gameSupervisor);
            }
        } else {
            boolean isThereACheck = darkAlivePieces.lookForChecks(-2, whiteKing.getCoordinates());
            whiteAlivePieces.makeThemReady(enPassantXPossibility);
            if (whiteAlivePieces.doTheyHaveAnyMoves()) {
                if (timeControl != TimeControl.NONE) {
                    darkClock.stop();
                    whiteClock.play();
                }
            } else {
                Result finalResult = isThereACheck ? Result.BLACK : Result.DRAW;
                stageManager.OpenEndingScene(finalResult, gameSupervisor);
            }
        }
        onMove.switchTurn();
    }

    public void clearData(){
        board = new Tile [SQUARE_NUMBER][SQUARE_NUMBER];

        tiles = new Group();
        pieces = new Group();

        whiteAlivePieces = new Army();
        darkAlivePieces = new Army();

        doubleCheck = false;
        check = false;

        enPassantXPossibility = 0;

        graveyard = null;

        whiteKing = null;
        darkKing = null;

        onMove = null;
        gameSupervisor = null;

        whiteClock = null;
        darkClock = null;

        right = null;
        promotionBox = null;
    }

    public class Clock {

        private HBox timer;

        private short tinyTimer;

        private Label tensOfMinutes;
        private Label onesOfMinutes;

        private Label tensOfSeconds;
        private Label onesOfSeconds;

        private Label semicolon;

        private Timeline timeline;
        private long timeLeft;

        private int increment;

        private GameSupervisor gameSupervisor;
        private Stage stage;

        ArrayList<Label> backupList;

        Clock(TimeControl timeControl, GameSupervisor gameSupervisor, Stage stage) {

            this.gameSupervisor = gameSupervisor;
            this.stage = stage;

            setTime(timeControl);

            backupList = new ArrayList<>();

            tensOfSeconds = new Label();
            onesOfSeconds = new Label();
            tensOfMinutes = new Label();
            onesOfMinutes = new Label();

            semicolon = new Label(":");
            semicolon.setPrefSize(Main.TILE_SIZE / 4, Main.TILE_SIZE);
            semicolon.setFont(new Font("Arial", Main.TILE_SIZE));

            backupList.add(tensOfMinutes);
            backupList.add(onesOfMinutes);
            backupList.add(tensOfSeconds);
            backupList.add(onesOfSeconds);

            initialize();
            timer = new HBox(tensOfMinutes, onesOfMinutes, semicolon, tensOfSeconds, onesOfSeconds);

            tinyTimer = 0;
            increment = (timeControl == TimeControl.THREE_PLUS_TWO) ? 2 : 0;

            timeline = new Timeline(new KeyFrame(Duration.millis(10),
                    e -> {
                        tinyTimer++;
                        if (tinyTimer >= 100) {
                            subtractSecond();
                            checkEndGame();
                            writeTimeDown();
                            tinyTimer = 0;
                        }

                    }));
            timeline.setCycleCount(Animation.INDEFINITE);
        }

        public void stop() {
            timeline.stop();
            timeLeft += increment;
            writeTimeDown();
        }


        public void play() {
            timeline.play();
        }

        public HBox getTimer() {
            return timer;
        }

        private void initialize() {

            for (Label label : backupList) {
                label.setPrefSize(Main.TILE_SIZE / 2, Main.TILE_SIZE);
                label.setFont(new Font("Arial", Main.TILE_SIZE));
            }

            writeTimeDown();
        }
        private void writeTimeDown() {
            tensOfMinutes.setText(String.valueOf((timeLeft / 60) / 10));
            onesOfMinutes.setText(String.valueOf((timeLeft / 60) % 10));

            tensOfSeconds.setText(String.valueOf((timeLeft % 60) / 10));
            onesOfSeconds.setText(String.valueOf((timeLeft % 60) % 10));

        }

        private void subtractSecond() {
            timeLeft--;

        }

        public void addTime(int time) {
            timeLeft += time;
            writeTimeDown();
        }

        public double getWidth() {
            return Main.TILE_SIZE * 2.5;
        }

        private void setTime(TimeControl timeControl) {
            if (timeControl == TimeControl.THREE) {
                timeLeft = 180;
            } else if (timeControl == TimeControl.FIVE) {
                timeLeft = 300;
            } else if (timeControl == TimeControl.FIFTEEN) {
                timeLeft = 900;
            } else if (timeControl == TimeControl.THREE_PLUS_TWO) {
                timeLeft = 180;
            }
        }

        private void checkEndGame() {
            if (timeLeft == 0) {
                timeline.stop();
                Result result = (onMove.getPieceColour() == PieceColour.BLACK) ? Result.WHITE : Result.BLACK;
                stageManager.OpenEndingScene(result, gameSupervisor);
            }
        }
    }

    public HBox promotionHBox(Piece piece, Stage stage, Pair<Note, Note> annotation, int pieceDuplication, Move result) {
        Button queen = new Button("Q");
        Button bishop = new Button("B");
        Button knight = new Button("K");
        Button rook = new Button("R");

        queen.setPrefSize(PROMOTION_BUTTON_SIZE, PROMOTION_BUTTON_SIZE);
        bishop.setPrefSize(PROMOTION_BUTTON_SIZE, PROMOTION_BUTTON_SIZE);
        knight.setPrefSize(PROMOTION_BUTTON_SIZE, PROMOTION_BUTTON_SIZE);
        rook.setPrefSize(PROMOTION_BUTTON_SIZE, PROMOTION_BUTTON_SIZE);
        queen.setOnAction(e -> {
            doPromotionButtonAction(PieceKind.QUEEN, piece, stage, annotation, pieceDuplication, result);
        } );
        bishop.setOnAction(e -> {
            doPromotionButtonAction(PieceKind.BISHOP, piece, stage, annotation, pieceDuplication, result);
        } );
        knight.setOnAction(e -> {
            doPromotionButtonAction(PieceKind.KNIGHT, piece, stage, annotation, pieceDuplication, result);
        } );
        rook.setOnAction(e -> {
            doPromotionButtonAction(PieceKind.ROOK, piece, stage, annotation, pieceDuplication, result);
        } );
        return new HBox(queen, bishop, knight, rook);
    }

    public void appendResultToStats(Result result) {
        Integer whiteWins = 0;
        Integer blackWins = 0;
        Integer draws = 0;
        checkIfStatsFileExists();
        try {
            File myObj = new File("stats.txt");
            Scanner myReader = new Scanner(myObj);
            whiteWins = myReader.nextInt();
            blackWins = myReader.nextInt();
            draws = myReader.nextInt();
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        if (result == Result.BLACK) {
            blackWins++;
        } else if (result == Result.WHITE) {
            whiteWins++;
        } else {
            draws++;
        }
        try {
            FileWriter writer = new FileWriter("stats.txt", false);
            writer.write(whiteWins.toString() + " ");;
            writer.write(blackWins.toString() + " ");
            writer.write(draws.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doPromotionButtonAction(PieceKind buttonKind, Piece movingPiece, Stage stage, Pair<Note, Note> annotation, int pieceDuplication, Move result) {
        right.getChildren().remove(promotionBox);
        movingPiece.setKindAfterPromotion(buttonKind);
        gameSupervisor.add(annotation, pieceDuplication, result);
        movingPiece.promote(buttonKind);
        movingPiece.repaint();
        onMove.switchMode();
        postMoveAction(stage);
    }

    public void checkIfStatsFileExists() {
        try {
            File pgnFile = new File("stats.txt");
            if (!pgnFile.isFile()) {
                pgnFile.createNewFile();
                FileWriter writer = new FileWriter("stats.txt", false);
                writer.write("0 0 0");
                writer.close();
            }
        } catch(IOException e){
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stageManager = new StageManager(primaryStage, this);
        primaryStage.setTitle("Fischer Chess");
        primaryStage.getIcons().add(new Image("sample/pawn.png"));
        primaryStage.setResizable(false);

        stageManager.OpenMainMenu();
    }

    public static void main(String[] args) {

        Application.launch(args);
    }

}