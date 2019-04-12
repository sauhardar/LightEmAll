import java.util.*;
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
  HashMap<GamePiece, Integer> graph;
  ArrayList<GamePiece> connectedNodes;

  LightEmAll(int numRows, int numCols, int boardType) {

    this.width = numCols * GamePiece.CELL_LENGTH;
    this.height = numRows * GamePiece.CELL_LENGTH;

    // 0 is manualGeneration, 1 is fractal, 2 is random
    if (boardType == 0) {
      this.board = this.makeBoard();
    }
    else if (boardType == 1) {
      this.board = this.manualBoard();
      this.fractalBoard(numRows, numCols, 0, 0);
      // Moved the initialization of the power station to the constructor.
      this.board.get(0).get(this.width / GamePiece.CELL_LENGTH / 2).powerStation = true;
      this.powerCol = this.width / GamePiece.CELL_LENGTH / 2;
      this.powerRow = 0;
    }

    this.nodes = new ArrayList<GamePiece>();
    this.getNodes();
    this.graph = new HashMap<GamePiece, Integer>();
    this.initHash();
    this.radius = this.calcRadius();
    connectedNodes = new ArrayList<GamePiece>();
    this.getConnected(new ArrayList<GamePiece>());
  }

  void getConnected(ArrayList<GamePiece> nodesConnected) {
    for (GamePiece gp : this.nodes) {
      if (this.piecesConnected(this.board.get(powerRow).get(powerCol), gp)) {
        this.connectedNodes.add(gp);
      }
    }
  }

  // Determines if two GamePieces are connected
  boolean piecesConnected(GamePiece target, GamePiece other) {
    if (target.equals(other)) {
      return true;
    }

    ArrayList<GamePiece> worklist = new ArrayList<GamePiece>();
    worklist.add(this.board.get(target.row).get(target.col));

    ArrayList<GamePiece> soFar = new ArrayList<GamePiece>();
    soFar.add(this.board.get(target.row).get(target.col));

    while (!worklist.isEmpty()) {
      GamePiece listFirst = worklist.get(0);
      int rowVal = listFirst.row;
      int colVal = listFirst.col;

      // Check ABOVE target
      if (rowVal - 1 > -1 && !(soFar.contains(this.board.get(rowVal - 1).get(colVal))
          && this.twoPiecesConnected(listFirst, this.board.get(rowVal - 1).get(colVal)))) {

        if (this.board.get(rowVal - 1).get(colVal).equals(other)) {
          return true;
        }
        worklist.add(this.board.get(rowVal - 1).get(colVal));
        soFar.add(this.board.get(rowVal - 1).get(colVal));
      }

      // Check BELOW target
      if (rowVal + 1 < this.height / GamePiece.CELL_LENGTH
          && !(soFar.contains(this.board.get(rowVal + 1).get(colVal))
              && this.twoPiecesConnected(listFirst, this.board.get(rowVal + 1).get(colVal)))) {
        if (this.board.get(rowVal + 1).get(colVal).equals(other)) {
          return true;
        }
        worklist.add(this.board.get(rowVal + 1).get(colVal));
        soFar.add(this.board.get(rowVal + 1).get(colVal));
      }

      // Check LEFT OF target
      if (colVal - 1 > -1 && !(soFar.contains(this.board.get(rowVal).get(colVal - 1))
          && this.twoPiecesConnected(listFirst, this.board.get(rowVal).get(colVal - 1)))) {

        if (this.board.get(rowVal).get(colVal - 1).equals(other)) {
          return true;
        }

        worklist.add(this.board.get(rowVal).get(colVal - 1));
        soFar.add(this.board.get(rowVal).get(colVal - 1));
      }

      // Check RIGHT OF target
      if (colVal + 1 < this.width / GamePiece.CELL_LENGTH
          && !(soFar.contains(this.board.get(rowVal).get(colVal + 1))
              && this.twoPiecesConnected(listFirst, this.board.get(rowVal).get(colVal + 1)))) {

        if (this.board.get(rowVal).get(colVal + 1).equals(other)) {
          return true;
        }

        worklist.add(this.board.get(rowVal).get(colVal + 1));
        soFar.add(this.board.get(rowVal).get(colVal + 1));
      }

      worklist.remove(0);
    }

    return false;
  }

  boolean twoPiecesConnected(GamePiece target, GamePiece other) {
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

  void getNodes() {
    for (ArrayList<GamePiece> row : this.board) {
      for (GamePiece gp : row) {
        this.nodes.add(gp);
      }
    }
  }

  void initHash() {
    for (GamePiece gp : this.nodes) {
      this.graph.put(gp, -1);
    }
  }

  // Sets every value to the correct distance and returns the last value in BFS
  GamePiece setDepths(int yPos, int xPos) {
    GamePiece lastFound = this.board.get(yPos).get(xPos);

    int depth = 0;

    // Worklist for BFS
    ArrayList<GamePiece> worklist = new ArrayList<GamePiece>();
    worklist.add(this.board.get(yPos).get(xPos));

    // Accumulate all the GamePieces already visited
    ArrayList<GamePiece> soFar = new ArrayList<GamePiece>();
    soFar.add(this.board.get(yPos).get(xPos));

    // Distance from home is zero for the home node
    this.graph.put(this.board.get(yPos).get(xPos), depth);

    while (!worklist.isEmpty()) {
      GamePiece listFirst = worklist.get(0);
      int rowVal = listFirst.row;
      int colVal = listFirst.col;
      boolean depthIncreased = false;

      // Check ABOVE target
      if (rowVal - 1 > -1 && this.piecesConnected(listFirst, this.board.get(rowVal - 1).get(colVal))
          && !(soFar.contains(this.board.get(rowVal - 1).get(colVal)))) {

        worklist.add(this.board.get(rowVal - 1).get(colVal));
        this.graph.put(this.board.get(rowVal - 1).get(colVal), depth + 1);

        if (graph.get(this.board.get(rowVal - 1).get(colVal)) > graph.get(lastFound)) {
          lastFound = this.board.get(rowVal - 1).get(colVal);
          depthIncreased = true;
        }
        soFar.add(this.board.get(rowVal - 1).get(colVal));
      }

      // Check BELOW target
      if (rowVal + 1 < this.height / GamePiece.CELL_LENGTH
          && this.piecesConnected(listFirst, this.board.get(rowVal + 1).get(colVal))
          && !(soFar.contains(this.board.get(rowVal + 1).get(colVal)))) {

        worklist.add(this.board.get(rowVal + 1).get(colVal));
        this.graph.put(this.board.get(rowVal + 1).get(colVal), depth + 1);

        if (graph.get(this.board.get(rowVal + 1).get(colVal)) > graph.get(lastFound)) {
          lastFound = this.board.get(rowVal + 1).get(colVal);
          depthIncreased = true;
        }
        soFar.add(this.board.get(rowVal + 1).get(colVal));
      }

      // Check LEFT OF target
      if (colVal - 1 > -1 && this.piecesConnected(listFirst, this.board.get(rowVal).get(colVal - 1))
          && !(soFar.contains(this.board.get(rowVal).get(colVal - 1)))) {

        worklist.add(this.board.get(rowVal).get(colVal - 1));
        this.graph.put(this.board.get(rowVal).get(colVal - 1), depth + 1);

        if (graph.get(this.board.get(rowVal).get(colVal - 1)) > graph.get(lastFound)) {
          lastFound = this.board.get(rowVal).get(colVal - 1);
          depthIncreased = true;
        }
        soFar.add(this.board.get(rowVal).get(colVal - 1));
      }

      // Check RIGHT OF target
      if (colVal + 1 < this.width / GamePiece.CELL_LENGTH
          && this.piecesConnected(listFirst, this.board.get(rowVal).get(colVal + 1))
          && !(soFar.contains(this.board.get(rowVal).get(colVal + 1)))) {

        worklist.add(this.board.get(rowVal).get(colVal + 1));
        this.graph.put(this.board.get(rowVal).get(colVal + 1), depth + 1);

        if (graph.get(this.board.get(rowVal).get(colVal + 1)) > graph.get(lastFound)) {
          lastFound = this.board.get(rowVal).get(colVal + 1);
          depthIncreased = true;
        }
        soFar.add(this.board.get(rowVal).get(colVal + 1));
      }

      if (depthIncreased) {
        depth++;
      }
      worklist.remove(0);
    }

    return lastFound;
  }

  int calcRadius() {
    GamePiece lastFound = setDepths(this.powerRow, this.powerCol);
    lastFound = setDepths(lastFound.row, lastFound.col);
    return (this.graph.get(lastFound) / 2) + 1;
  }

  // Makes the scene with all the game pieces drawn.
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(this.width, this.height);

    for (ArrayList<GamePiece> row : this.board) {
      for (GamePiece cell : row) {
        scene.placeImageXY(cell.drawPiece(),
            cell.col * GamePiece.CELL_LENGTH + GamePiece.CELL_LENGTH / 2,
            cell.row * GamePiece.CELL_LENGTH + GamePiece.CELL_LENGTH / 2);
      }
    }
    return scene;
  }

  // Not used anymore
  public ArrayList<ArrayList<GamePiece>> makeBoard() {
    ArrayList<ArrayList<GamePiece>> boardResult = new ArrayList<ArrayList<GamePiece>>();
    int midPointH = (this.height / GamePiece.CELL_LENGTH) / 2;
    int midPointW = (this.width / GamePiece.CELL_LENGTH) / 2;

    for (int i = 0; i < this.height / GamePiece.CELL_LENGTH; i++) {
      ArrayList<GamePiece> rowResult = new ArrayList<GamePiece>();
      // Midpoint is used to locate the middle of the board or as close as possible.
      for (int j = 0; j < this.width / GamePiece.CELL_LENGTH; j++) {
        if (i == midPointH && j == midPointW) {
          rowResult.add(new GamePiece(i, j, true, true, true, true, true));
          this.powerCol = midPointW;
          this.powerRow = midPointH;
        }
        else if (i == midPointH) {
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

  // Creates the game board with empty cells
  public ArrayList<ArrayList<GamePiece>> manualBoard() {
    ArrayList<ArrayList<GamePiece>> boardResult = new ArrayList<ArrayList<GamePiece>>();
    for (int i = 0; i < this.height / GamePiece.CELL_LENGTH; i++) {
      ArrayList<GamePiece> rowResult = new ArrayList<GamePiece>();
      for (int j = 0; j < this.width / GamePiece.CELL_LENGTH; j++) {
        rowResult.add(new GamePiece(i, j, false, false, false, false, false));
      }
      boardResult.add(rowResult);
    }
    return boardResult;
  }

  // Creates the game board using a subdivision algorithim for factal-like wiring
  // Creates the game board using a subdivision algorithm for fractal-like wiring
  public void fractalBoard(int numRows, int numCols, int currRow, int currCol) {
    int startRow = currRow;
    int startCol = currCol;

    // At the base case of one row or one column, irrespective
    // of the other dimension, the program should stop. No U should be drawn.
    if (numRows == 1 || numCols == 1) { }
      
    // Initially draw a U-shaped wire formation around the outside of the given grid
    
    else {
      // Top left of U
      this.board.get(startRow).get(startCol).bottom = true;
      // Bottom left
      this.board.get(startRow + numRows - 1).get(startCol).right = true;
      this.board.get(startRow + numRows - 1).get(startCol).top = true;
      // Bottom right
      this.board.get(startRow + numRows - 1).get(startCol + numCols - 1).left = true;
      this.board.get(startRow + numRows - 1).get(startCol + numCols - 1).top = true;
      // Top right
      this.board.get(startRow).get(startCol + numCols - 1).bottom = true;
      // Sides of U
      for (int i = startRow + 1; i < startRow + numRows - 1; i++) {
        this.board.get(i).get(startCol).top = true;
        this.board.get(i).get(startCol).bottom = true;
        this.board.get(i).get(startCol + numCols - 1).top = true;
        this.board.get(i).get(startCol + numCols - 1).bottom = true;
      }
      // Bottom row
      for (int i = startCol + 1; i < startCol + numCols - 1; i++) {
        this.board.get(startRow + numRows - 1).get(i).left = true;
        this.board.get(startRow + numRows - 1).get(i).right = true;
      }
    }

    if (numRows == 1 || numCols == 1 || numCols == 2) { }
    // When there is only one row, all pieces should have the top field be true.
    else if (numRows == 1 && numCols > 2) {
      this.fractalBoard(1, (int) Math.ceil(numCols / 2), currRow, currCol);
      this.fractalBoard(1, numCols / 2, currRow, (int) Math.ceil(currCol / 2));
    }

    else if (numRows == 1 && numCols == 2) {
      this.board.get(startRow).get(startCol).right = true;
      this.board.get(startRow).get(startCol + 1).left = true;

    }
    else if (numRows == 2 && numCols == 1) {
      this.board.get(startRow).get(startCol).bottom = true;
      this.board.get(startRow + 1).get(startCol).top = true;
    }
    else if (numRows == 2) {
      for (int i = startCol + 1; i < startCol + numCols - 1; i++) {
        this.board.get(startRow).get(i).bottom = true;
        this.board.get(startRow + 1).get(i).top = true;
      }
    }
    
    else if (numRows >= 3 || numCols >= 3) {
      // Top left quadrant
      fractalBoard((int) Math.ceil(numRows / 2.0), (int) Math.ceil(numCols / 2.0), currRow,
          currCol);

      // Top right quadrant
      fractalBoard((int) Math.ceil(numRows / 2.0), numCols / 2, currRow,
          currCol + (int) Math.ceil(numCols / 2.0));

      // Bottom left quadrant
      fractalBoard(numRows / 2, (int) Math.ceil(numCols / 2.0),
          currRow + (int) Math.ceil(numRows / 2.0), currCol);

      // Bottom right quadrant
      fractalBoard(numRows / 2, numCols / 2, currRow + (int) Math.ceil(numRows / 2.0),
          currCol + (int) Math.ceil(numCols / 2.0));
    }
    this.board.get(0).get(this.width / GamePiece.CELL_LENGTH / 2).powerStation = true;
    this.powerCol = this.width / GamePiece.CELL_LENGTH / 2;
    this.powerRow = 0;
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

  // Handles all keys clicked (to move the powerstation)
  public void onKeyEvent(String key) {

    if (key.equals("left")) {
      if ((this.powerCol - 1 > -1)
          && piecesConnected(this.board.get(this.powerRow).get(this.powerCol),
              this.board.get(this.powerRow).get(this.powerCol - 1))) {
        this.board.get(this.powerRow).get(this.powerCol).powerStation = false;
        this.board.get(this.powerRow).get(this.powerCol - 1).powerStation = true;
        this.powerCol--;
      }
    }

    else if (key.equals("right")) {
      if ((this.powerCol + 1 < this.width / GamePiece.CELL_LENGTH)
          && piecesConnected(this.board.get(this.powerRow).get(this.powerCol),
              this.board.get(this.powerRow).get(this.powerCol + 1))) {
        this.board.get(this.powerRow).get(this.powerCol).powerStation = false;
        this.board.get(this.powerRow).get(this.powerCol + 1).powerStation = true;
        this.powerCol++;
      }
    }

    else if (key.equals("up")) {
      if ((this.powerRow - 1 >= 0)
          && piecesConnected(this.board.get(this.powerRow).get(this.powerCol),
              this.board.get(this.powerRow - 1).get(this.powerCol))) {
        this.board.get(this.powerRow).get(this.powerCol).powerStation = false;
        this.board.get(this.powerRow - 1).get(this.powerCol).powerStation = true;
        this.powerRow--;
      }
    }

    else if (key.equals("down")) {
      if ((this.powerRow + 1 < (this.height / GamePiece.CELL_LENGTH))
          && piecesConnected(this.board.get(this.powerRow).get(this.powerCol),
              this.board.get(this.powerRow + 1).get(this.powerCol))) {
        this.board.get(this.powerRow).get(this.powerCol).powerStation = false;
        this.board.get(this.powerRow + 1).get(this.powerCol).powerStation = true;
        this.powerRow++;
      }
    }
    System.out.println("Post-key star posn: (" + this.powerCol + ", " + this.powerRow + ")");
  }

  /*
   * NOT DONE - NOT NEEDED FOR PART 2 public WorldEnd worldEnds() { if
   * (allConnected()) { return new WorldEnd(true, this.finalScene()); } else {
   * return new WorldEnd(false, this.makeScene()); } }
   */

  // The end scene that congratulates the user on winning. User can only win or keep playing.
  public WorldScene finalScene() {
    WorldScene ws = this.makeScene();
    ws.placeImageXY(new TextImage("You won", 35, Color.MAGENTA), this.width / 2, this.height / 2);
    return ws;
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
  boolean isPowered;

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

    LineImage vertLine = new LineImage(new Posn(0, CELL_LENGTH / 2), Color.GRAY);
    LineImage horLine = new LineImage(new Posn(CELL_LENGTH / 2, 0), Color.GRAY);

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

//All the examples and tests.
class ExamplesGame {
  LightEmAll test;
  LightEmAll threex3;
  LightEmAll fourx4;
  LightEmAll fivex5;

  void initData() {
    // To use with bigbang
    test = new LightEmAll(6, 4, 1);
    // To test a 3x3 grid
    threex3 = new LightEmAll(3, 3, 0);
    // To test a 4x4 grid
    fourx4 = new LightEmAll(4, 4, 0);
    // To test a 5x5 grid
    fivex5 = new LightEmAll(5, 5, 0);
  }

// Runs the program with a predetermined, easy-to-solve pattern.
  void testMain(Tester t) {
    initData();
    test.bigBang(test.width, test.height, .003);
    // threex3.bigBang(this.threex3.width, this.threex3.height, .003);
  }

  // Testing the makeScene() method
  void testMakeScene(Tester t) {
    initData();
    // testing 3x3
    WorldScene testImage1 = new WorldScene(this.threex3.width, this.threex3.width);
    for (ArrayList<GamePiece> row : this.threex3.board) {
      for (GamePiece cell : row) {
        testImage1.placeImageXY(cell.drawPiece(),
            cell.col * GamePiece.CELL_LENGTH + GamePiece.CELL_LENGTH / 2,
            cell.row * GamePiece.CELL_LENGTH + GamePiece.CELL_LENGTH / 2);
      }
    }
    // testing 4x4
    WorldScene testImage = new WorldScene(this.fourx4.width, this.fourx4.width);
    for (ArrayList<GamePiece> row : this.fourx4.board) {
      for (GamePiece cell : row) {
        testImage.placeImageXY(cell.drawPiece(),
            cell.col * GamePiece.CELL_LENGTH + GamePiece.CELL_LENGTH / 2,
            cell.row * GamePiece.CELL_LENGTH + GamePiece.CELL_LENGTH / 2);
      }
    } // testing 5x5
    WorldScene testImage2 = new WorldScene(this.fivex5.width, this.fivex5.width);
    for (ArrayList<GamePiece> row : this.fivex5.board) {
      for (GamePiece cell : row) {
        testImage2.placeImageXY(cell.drawPiece(),
            cell.col * GamePiece.CELL_LENGTH + GamePiece.CELL_LENGTH / 2,
            cell.row * GamePiece.CELL_LENGTH + GamePiece.CELL_LENGTH / 2);
      }
    }
    t.checkExpect(this.fourx4.makeScene(), testImage);
    t.checkExpect(this.threex3.makeScene(), testImage1);
    t.checkExpect(this.fivex5.makeScene(), testImage2);
  }

  // Testing the method makeBoard()
  void testMakeBoard(Tester t) {

    initData();
    // Testing a 3x3 board that is manually created.
    ArrayList<ArrayList<GamePiece>> answer = new ArrayList<ArrayList<GamePiece>>();
    ArrayList<GamePiece> row1 = new ArrayList<GamePiece>(
        Arrays.asList(new GamePiece(0, 0, true, true, false, false, false),
            new GamePiece(0, 1, true, true, false, false, false),
            new GamePiece(0, 2, true, true, false, false, false)));
    ArrayList<GamePiece> row2 = new ArrayList<GamePiece>(
        Arrays.asList(new GamePiece(1, 0, true, true, true, true, false),
            new GamePiece(1, 1, true, true, true, true, true),
            new GamePiece(1, 2, true, true, true, true, false)));
    ArrayList<GamePiece> row3 = new ArrayList<GamePiece>(
        Arrays.asList(new GamePiece(2, 0, true, true, false, false, false),
            new GamePiece(2, 1, true, true, false, false, false),
            new GamePiece(2, 2, true, true, false, false, false)));

    answer.add(row1);
    answer.add(row2);
    answer.add(row3);

    t.checkExpect(this.threex3.makeBoard(), answer);

    // Testing a 5x5 board
    ArrayList<ArrayList<GamePiece>> answer2 = new ArrayList<ArrayList<GamePiece>>();
    ArrayList<GamePiece> row15 = new ArrayList<GamePiece>(
        Arrays.asList(new GamePiece(0, 0, true, true, false, false, false),
            new GamePiece(0, 1, true, true, false, false, false),
            new GamePiece(0, 2, true, true, false, false, false),
            new GamePiece(0, 3, true, true, false, false, false),
            new GamePiece(0, 4, true, true, false, false, false)));
    ArrayList<GamePiece> row25 = new ArrayList<GamePiece>(
        Arrays.asList(new GamePiece(1, 0, true, true, false, false, false),
            new GamePiece(1, 1, true, true, false, false, false),
            new GamePiece(1, 2, true, true, false, false, false),
            new GamePiece(1, 3, true, true, false, false, false),
            new GamePiece(1, 4, true, true, false, false, false)));
    ArrayList<GamePiece> row35 = new ArrayList<GamePiece>(
        Arrays.asList(new GamePiece(2, 0, true, true, true, true, false),
            new GamePiece(2, 1, true, true, true, true, false),
            new GamePiece(2, 2, true, true, true, true, true),
            new GamePiece(2, 3, true, true, true, true, false),
            new GamePiece(2, 4, true, true, true, true, false)));
    ArrayList<GamePiece> row45 = new ArrayList<GamePiece>(
        Arrays.asList(new GamePiece(3, 0, true, true, false, false, false),
            new GamePiece(3, 1, true, true, false, false, false),
            new GamePiece(3, 2, true, true, false, false, false),
            new GamePiece(3, 3, true, true, false, false, false),
            new GamePiece(3, 4, true, true, false, false, false)));
    ArrayList<GamePiece> row55 = new ArrayList<GamePiece>(
        Arrays.asList(new GamePiece(4, 0, true, true, false, false, false),
            new GamePiece(4, 1, true, true, false, false, false),
            new GamePiece(4, 2, true, true, false, false, false),
            new GamePiece(4, 3, true, true, false, false, false),
            new GamePiece(4, 4, true, true, false, false, false)));

    answer2.add(row15);
    answer2.add(row25);
    answer2.add(row35);
    answer2.add(row45);
    answer2.add(row55);

    t.checkExpect(this.fivex5.makeBoard(), answer2);
    // More testing 5x5 creation:
    t.checkExpect(this.fivex5.board.get(0).get(0).bottom, false);
    t.checkExpect(this.fivex5.board.get(0).get(0).right && this.fivex5.board.get(0).get(0).left,
        true);
    t.checkExpect(this.fivex5.board.get(0).get(0).powerStation, false);
    t.checkExpect(this.fivex5.board.get(2).get(2).powerStation, true);
    t.checkExpect(this.fivex5.board.get(2).get(2).right && this.fivex5.board.get(2).get(2).left
        && this.fivex5.board.get(2).get(2).top && this.fivex5.board.get(2).get(2).bottom, true);
  }

  // Testing the whether clicking rotates the game pieces correctly.
  void testOnMouseClicked(Tester t) {
    initData();
    t.checkExpect(this.threex3.board.get(0).get(0).bottom, false);
    this.threex3.onMouseClicked(new Posn(10, 10), "RightButton");
    t.checkExpect(this.threex3.board.get(0).get(0).bottom, false);
    this.threex3.onMouseClicked(new Posn(10, 10), "LeftButton");
    t.checkExpect(this.threex3.board.get(0).get(0).bottom, true);
    this.threex3.onMouseClicked(new Posn(10, 10), "LeftButton");
    t.checkExpect(this.threex3.board.get(0).get(0).bottom, false);
    t.checkExpect(this.fivex5.board.get(2).get(2).powerStation, true);
    t.checkExpect(this.fivex5.board.get(2).get(2).right, true);
    this.fivex5.onMouseClicked(new Posn(2 * GamePiece.CELL_LENGTH, 2 * GamePiece.CELL_LENGTH),
        "LeftButton");
    t.checkExpect(this.fivex5.board.get(2).get(2).right, true);
    t.checkExpect(this.fivex5.board.get(2).get(2).powerStation, true);
  }

  // Testing whether the pieces are connected
  void testPiecesConnected(Tester t) {
    initData();
    t.checkExpect(this.threex3.twoPiecesConnected(this.threex3.board.get(0).get(0),
        this.threex3.board.get(1).get(0)), false);
    this.threex3.onMouseClicked(new Posn(10, 60), "LeftButton");
    t.checkExpect(this.threex3.twoPiecesConnected(this.threex3.board.get(0).get(0),
        this.threex3.board.get(0).get(1)), true);
    t.checkExpect(this.fourx4.twoPiecesConnected(this.fourx4.board.get(2).get(2),
        this.fourx4.board.get(2).get(3)), true);
    this.fourx4.onMouseClicked(
        new Posn(2 * GamePiece.CELL_LENGTH + 5, 3 * GamePiece.CELL_LENGTH + 5), "LeftButton");
    t.checkExpect(this.fourx4.twoPiecesConnected(this.fourx4.board.get(2).get(2),
        this.fourx4.board.get(2).get(3)), true);
  }

  // Testing drwaing individual game pieces
  void testDrawPiece(Tester t) {
    initData();
    LineImage vertLine = new LineImage(new Posn(0, GamePiece.CELL_LENGTH / 2), Color.GRAY);
    LineImage horLine = new LineImage(new Posn(GamePiece.CELL_LENGTH / 2, 0), Color.GRAY);
    // Testing top right piece
    WorldImage outlineTopRight = new RectangleImage(GamePiece.CELL_LENGTH, GamePiece.CELL_LENGTH,
        OutlineMode.OUTLINE, Color.BLACK);
    WorldImage resultTopRight = new OverlayImage(new RectangleImage(GamePiece.CELL_LENGTH,
        GamePiece.CELL_LENGTH, OutlineMode.SOLID, Color.DARK_GRAY), new EmptyImage());
    resultTopRight = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.PINHOLE, horLine, 0, 0,
        resultTopRight);
    resultTopRight = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.PINHOLE, horLine, 0, 0,
        resultTopRight);
    // Testing 4-way connection:
    WorldImage outline4way = new RectangleImage(GamePiece.CELL_LENGTH, GamePiece.CELL_LENGTH,
        OutlineMode.OUTLINE, Color.BLACK);
    WorldImage result4way = new OverlayImage(new RectangleImage(GamePiece.CELL_LENGTH,
        GamePiece.CELL_LENGTH, OutlineMode.SOLID, Color.DARK_GRAY), new EmptyImage());
    result4way = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.PINHOLE, horLine, 0, 0,
        result4way);
    result4way = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.PINHOLE, horLine, 0, 0,
        result4way);
    result4way = new OverlayOffsetAlign(AlignModeX.PINHOLE, AlignModeY.TOP, vertLine, 0, 0,
        result4way);
    result4way = new OverlayOffsetAlign(AlignModeX.PINHOLE, AlignModeY.BOTTOM, vertLine, 0, 0,
        result4way);

    WorldImage outlinestar = new RectangleImage(GamePiece.CELL_LENGTH, GamePiece.CELL_LENGTH,
        OutlineMode.OUTLINE, Color.BLACK);
    WorldImage resultstar = new OverlayImage(new RectangleImage(GamePiece.CELL_LENGTH,
        GamePiece.CELL_LENGTH, OutlineMode.SOLID, Color.DARK_GRAY), new EmptyImage());
    resultstar = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.PINHOLE, horLine, 0, 0,
        resultstar);
    resultstar = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.PINHOLE, horLine, 0, 0,
        resultstar);
    resultstar = new OverlayOffsetAlign(AlignModeX.PINHOLE, AlignModeY.TOP, vertLine, 0, 0,
        resultstar);
    resultstar = new OverlayOffsetAlign(AlignModeX.PINHOLE, AlignModeY.BOTTOM, vertLine, 0, 0,
        resultstar);
    WorldImage star = new StarImage(15, OutlineMode.SOLID, Color.CYAN);
    resultstar = new OverlayImage(star, resultstar);

    // Top right game piece
    t.checkExpect(this.fivex5.board.get(0).get(0).drawPiece(),
        new OverlayImage(outlineTopRight, resultTopRight));
    // Two down (4-way connection)
    t.checkExpect(this.fivex5.board.get(0).get(2).drawPiece(),
        new OverlayImage(outline4way, resultTopRight));
    // Testing the star (middle) piece
    t.checkExpect(this.fivex5.board.get(2).get(2).drawPiece(),
        new OverlayImage(outlinestar, resultstar));
  }

  // Testing rotation for various game pieces.
  void testRotate(Tester t) {
    initData();
    // top right piece
    t.checkExpect(this.fivex5.board.get(0).get(0).left && this.fivex5.board.get(0).get(0).right,
        true);
    this.fivex5.board.get(0).get(0).rotate();
    t.checkExpect(this.fivex5.board.get(0).get(0).left || this.fivex5.board.get(0).get(0).right,
        false);
    // piece with coord (2,0)
    t.checkExpect(this.fivex5.board.get(3).get(0).top || this.fivex5.board.get(3).get(0).bottom,
        false);
    this.fivex5.board.get(3).get(0).rotate();
    t.checkExpect(this.fivex5.board.get(3).get(0).top && this.fivex5.board.get(3).get(0).bottom,
        true);
    // star/4-way piece.
    t.checkExpect(this.fivex5.board.get(2).get(2).bottom && this.fivex5.board.get(2).get(2).top
        && this.fivex5.board.get(2).get(2).right && this.fivex5.board.get(2).get(2).left, true);
    this.fivex5.board.get(0).get(0).rotate();
    t.checkExpect(this.fivex5.board.get(2).get(2).bottom && this.fivex5.board.get(2).get(2).top
        && this.fivex5.board.get(2).get(2).right && this.fivex5.board.get(2).get(2).left, true);
  }
}
