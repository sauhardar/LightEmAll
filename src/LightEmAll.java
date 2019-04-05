import java.util.ArrayList;
import java.util.Arrays;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

class LightEmAll extends World {

  // a list of columns of GamePieces
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  // the current location of the power station, as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;

  LightEmAll(int width, int height) {
    this.width = width * GamePiece.CELL_LENGTH;
    this.height = height * GamePiece.CELL_LENGTH;
    this.board = makeBoard();
  }

  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(width, height);
    scene.placeImageXY(this.drawBoard(), width / 2, height / 2);
    return scene;
  }

  public ArrayList<ArrayList<GamePiece>> makeBoard() {
    ArrayList<ArrayList<GamePiece>> boardResult = new ArrayList<ArrayList<GamePiece>>();
    ArrayList<GamePiece> rowResult = new ArrayList<GamePiece>();

    for (int i = 0; i < height / GamePiece.CELL_LENGTH; i++) {
      for (int j = 0; j < width / GamePiece.CELL_LENGTH; j++) {
        rowResult.add(new GamePiece(true, false, true, true, true));
      }
      boardResult.add(rowResult);
    }

    return boardResult;
  }

  public WorldImage drawBoard() {
    WorldImage boardImg = new EmptyImage();
    WorldImage rowImg = new EmptyImage();

    for (ArrayList<GamePiece> column : this.board) {
      for (GamePiece cell : column) {
        rowImg = new BesideImage(cell.drawPiece(), rowImg);
      }
      boardImg = new AboveImage(rowImg, boardImg);
      rowImg = new EmptyImage();
    }
    return boardImg;
  }

  // Handles all clicking
  public void onMouseClicked(Posn mousePos, String button) {
    int posX = mousePos.x / GamePiece.CELL_LENGTH;
    int posY = mousePos.y / GamePiece.CELL_LENGTH;
    GamePiece gp = this.board.get(posX).get(posY);
    if (button.equals("LeftButton")
        && (posY <= this.height && 0 <= posY && posX <= this.width && 0 <= posX)) {
      System.out.println(mousePos);
      gp.rotate();
    }
  }
}

class GamePiece {
  static final int CELL_LENGTH = 40;

  int row;
  int col;
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;
  boolean powerStation;

  GamePiece() {
  }

  GamePiece(boolean left, boolean right, boolean top, boolean bottom, boolean powerStation) {
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
  }

  WorldImage drawPiece() {
    WorldImage outline = new RectangleImage(CELL_LENGTH, CELL_LENGTH, OutlineMode.OUTLINE,
        Color.BLACK);
    WorldImage result = new OverlayImage(
        new RectangleImage(CELL_LENGTH, CELL_LENGTH, OutlineMode.SOLID, Color.DARK_GRAY),
        new EmptyImage());

    LineImage vertLine = new LineImage(new Posn(0, CELL_LENGTH / 2), Color.ORANGE);
    LineImage horLine = new LineImage(new Posn(CELL_LENGTH / 2, 0), Color.ORANGE);

    // Connected to the left
    if (this.left) {
      result = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.PINHOLE, horLine, 0, 0, result);
    }

    if (this.right) {
      result = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.PINHOLE, horLine, 0, 0, result);
    }

    if (this.top) {
      result = new OverlayOffsetAlign(AlignModeX.PINHOLE, AlignModeY.TOP, vertLine, 0, 0, result);

    }

    if (this.bottom) {
      result = new OverlayOffsetAlign(AlignModeX.PINHOLE, AlignModeY.BOTTOM, vertLine, 0, 0,
          result);
    }

    if (this.powerStation) {
      WorldImage star = new StarImage(15, OutlineMode.SOLID, Color.CYAN);
      result = new OverlayImage(star, result);
    }
    return new OverlayImage(outline, result);
  }
  
  // When clicked, the GamePiece is rotated clockwise (90º)
  void rotate() {
    boolean prevLeft = this.left;
    boolean prevTop = this.top;
    boolean prevBot = this.bottom;
    boolean prevRight = this.right;
    this.top = prevLeft;
    this.right = prevTop;
    this.bottom = prevRight;
    this.left = prevBot;
  }  
}

class Edge {
  GamePiece fromNode;
  GamePiece toNode;
  int weight;
}

class ExamplesGame {
  LightEmAll test;

  void initData() {
    test = new LightEmAll(10, 10);
  }

  void testMain(Tester t) {
    initData();
    test.bigBang(test.width, test.height, .003);
  }
}
