import java.util.*;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

class LightEmAllSauhard extends World {

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

  LightEmAllSauhard(int width, int height) {
    this.width = width * GamePiece.CELL_LENGTH;
    this.height = height * GamePiece.CELL_LENGTH;
    this.board = makeBoard();
  }

  // Makes the scene
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(width, height);
    scene.placeImageXY(this.drawBoard(), width / 2, height / 2);
    return scene;
  }

  // Creates the game board
  // Currently written with a manual generation that always makes the same board
  public ArrayList<ArrayList<GamePiece>> makeBoard() {
    ArrayList<ArrayList<GamePiece>> boardResult = new ArrayList<ArrayList<GamePiece>>();

    for (int i = 0; i < this.height / GamePiece.CELL_LENGTH; i++) {
      ArrayList<GamePiece> rowResult = new ArrayList<GamePiece>();

      for (int j = 0; j < this.width / GamePiece.CELL_LENGTH; j++) {
        if (i == 2 && j == 2) {
          rowResult.add(new GamePiece(i, j, true, true, true, true, true));
          this.powerCol = 2;
          this.powerRow = 2;
        }
        else if (i == 2) {
          rowResult.add(new GamePiece(i, j, true, true, true, true, false));
        }
        else {
          rowResult.add(new GamePiece(i, j, true, true, false, false, false));
        }
      }
      boardResult.add(rowResult);
    }
    return boardResult;
  }

  // Draws the board
  public WorldImage drawBoard() {
    WorldImage boardImg = new EmptyImage();

    for (ArrayList<GamePiece> row : this.board) {
      WorldImage rowImg = new EmptyImage();

      for (GamePiece cell : row) {
        rowImg = new BesideImage(rowImg, cell.drawPiece());
      }
      boardImg = new AboveImage(boardImg, rowImg);
    }

    return boardImg;
  }

  // Handles all clicking
  public void onMouseClicked(Posn mousePos, String button) {
    int posX = mousePos.x / GamePiece.CELL_LENGTH;
    int posY = mousePos.y / GamePiece.CELL_LENGTH;
    GamePiece gp = this.board.get(posY).get(posX);
    if (button.equals("LeftButton")
        && (posY <= this.height && 0 <= posY && posX <= this.width && 0 <= posX)) {
      gp.rotate();
    }
  }

  // Determines if two neighbors are connected
  boolean piecesConnected(GamePiece target, GamePiece other) {
    // target is ABOVE other
    if (target.row + 1 == other.row && target.col == other.col) {
      return target.bottom && other.top;
    }
    // target is BELOW other
    else if (target.row - 1 == other.row && target.col == other.col) {
      return target.top && other.bottom;
    } // target is on LEFT of other
    else if (target.col + 1 == other.col && target.row == other.row) {
      return target.right && other.left;
    } // target is on RIGHT of other
    else if (target.col - 1 == other.col && target.row == other.row) {
      return target.left && other.right;
    }
    return false;
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

  GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom,
      boolean powerStation) {
    this.row = row;
    this.col = col;
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
  LightEmAllSauhard test;

  void initData() {
    test = new LightEmAllSauhard(5, 5);
  }

  void testMain(Tester t) {
    initData();
    test.bigBang(test.width, test.height, .003);
  }
}
