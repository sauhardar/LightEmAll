import java.util.*;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

/* NOTES FOR EXTRA CREDIT:
 * Implemented for extra credit:
 * 1. Timer
 * 2. Gradient power line
 * 3. Score
 * 4. Reset button
 * 5. Ability to play different types of games
 * (1. manualGeneration 2. Fractal board 3. Kruskal's algorithm)
 * The boardHeight field was necessary because we needed to add timer and score
 * */

// The main game class
class LightEmAll extends World {
  // Random seed for rotation
  private final Random RANDOBJ = new Random(1);
  public int currSec = (int) (System.currentTimeMillis() / 1000);

  // a list of columns of GamePieces
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of ALL unique and sorted edges
  ArrayList<Edge> allEdges;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  int score;
  // height is the height of just the board/game
  // boardHeight is the height including the extra space (for extra credit)
  int boardHeight;
  int boardType;
  // the current location of the power station, as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  HashMap<GamePiece, Integer> graph;

  LightEmAll(int numRows, int numCols, int boardType) {

    this.width = numCols * GamePiece.CELL_LENGTH;
    this.height = numRows * GamePiece.CELL_LENGTH;
    this.boardHeight = numRows * GamePiece.CELL_LENGTH + (this.height / 5);

    // 0 is manualGeneration, 1 is fractal, 2 is random
    if (boardType == 0) {
      this.board = this.makeBoard();
      this.nodes = new ArrayList<GamePiece>();
      this.getNodes();
    }
    else if (boardType == 1) {
      this.board = this.manualBoard();
      this.fractalBoard(numRows, numCols, 0, 0);
      this.board.get(0).get(this.width / GamePiece.CELL_LENGTH / 2).powerStation = true;
      this.powerCol = this.width / GamePiece.CELL_LENGTH / 2;
      this.powerRow = 0;
      this.nodes = new ArrayList<GamePiece>();
      this.getNodes();
    }
    // When the user enters 2
    else if (boardType == 2) {
      this.board = this.manualBoard();
      // Assigns neighbors to each GamePiece
      for (ArrayList<GamePiece> row : this.board) {
        for (GamePiece gp : row) {
          gp.assignNeighbors(this.board);
        }
      }
      this.allEdges = new ArrayList<Edge>();
      this.addAllEdges();
      this.sortEdges();
      this.board.get(0).get(0).powerStation = true;
      this.powerCol = 0;
      this.powerRow = 0;
      this.nodes = new ArrayList<GamePiece>();
      this.getNodes();
      this.mst = new ArrayList<Edge>();
      this.addToMST();
      this.connect();
      this.rotatePieces();
    }
    this.boardType = boardType;
    this.graph = new HashMap<GamePiece, Integer>();
    this.initHash();
    this.radius = this.calcRadius();
    this.getPowered();
    this.score = 0;
  }

  // EFFECT: Rotates the wiring by a random number
  // Rotates all the GamePieces by a random amount
  void rotatePieces() {
    for (ArrayList<GamePiece> row : this.board) {
      for (GamePiece gp : row) {
        int rand = this.RANDOBJ.nextInt(4);
        while (rand != 0) {
          gp.rotate();
          rand--;
        }
      }
    }
  }

  // EFFECT: Fills the allEdges field with the edges
  // Adds all the edges to the allEdges field
  void addAllEdges() {
    // In each row in the board
    for (ArrayList<GamePiece> row : this.board) {
      // in each gp in the row
      for (GamePiece gp : row) {
        // for each neighbhor
        for (GamePiece neighbhor : gp.neighbors) {
          // Ensures the same edge is not added twice.
          // A to B is the same as B to A.
          if (this.existsEdge(neighbhor, gp, this.allEdges)) {
            // DON'T ADD EDGE
          }
          else {
            this.allEdges.add(new Edge(gp, neighbhor));
          }
        }
      }
    }
  }

  // EFFECT: The allEdges arraylist is put in non-descending order
  // Sorts the edges by weight
  void sortEdges() {
    this.allEdges.sort(new SortByWeight());
  }

  // Determines if there already exists an edge with the given GamePieces
  boolean existsEdge(GamePiece from, GamePiece to, ArrayList<Edge> pool) {
    for (Edge edge : pool) {
      if (edge.fromNode.samePiece(from) && edge.toNode.samePiece(to)) {
        return true;
      }
    }
    return false;
  }

  // EFFECT: Adds all the appropriate edges to the MST field
  // If the edge does not create a cycle, add it to the minimum spanning tree.
  void addToMST() {
    HashMap<String, String> representatives = new HashMap<String, String>();
    ArrayList<Edge> sortedEdges = new ArrayList<Edge>(this.allEdges); // an alias

    // Initially links each node to itself
    for (GamePiece gp : this.nodes) {
      representatives.put(gp.toString(), gp.toString());
    }

    while (!mstDone(representatives)) {
      Edge curr = sortedEdges.remove(0);
      if (this.findRep(representatives, curr.fromNode.toString())
          .equals(this.findRep(representatives, curr.toNode.toString()))) {
        // Ignore this edge
      }
      else {
        this.mst.add(curr);
        representatives.put(findRep(representatives, curr.fromNode.toString()),
            findRep(representatives, curr.toNode.toString()));
        // method that updates all nodes that previously had the rep that is changed
      }
    }
  }

  // EFFECT: Adds the wires to each node for each GamePiece
  // Connects all the wires for each GamePiece
  void connect() {
    for (Edge e : this.mst) {
      e.connectNodes();
    }
  }

  // Finds the representative of each Node
  String findRep(HashMap<String, String> reps, String node) {
    if (reps.get(node).equals(node)) {
      return node;
    }
    else {
      return findRep(reps, reps.get(node));
    }
  }

  // Determines if MST has been constructed (only one representative is itself)
  boolean mstDone(HashMap<String, String> representatives) {
    int numSelf = 0;

    Iterator<Map.Entry<String, String>> it = representatives.entrySet().iterator();

    while (it.hasNext()) {
      Map.Entry<String, String> edge = it.next();
      if (edge.getKey().equals(edge.getValue())) {
        numSelf++;
      }
    }
    return numSelf <= 1;
  }

  // EFFECT: Sets isPowered to true if the cell has power
  // Runs through the GamePieces and determines if they are powered
  void getPowered() {
    this.setDepths(this.powerRow, this.powerCol);

    for (GamePiece gp : this.nodes) {
      if (this.piecesConnected(this.board.get(powerRow).get(powerCol), gp)
          && (this.graph.get(gp) <= this.radius)) {
        gp.isPowered = true;
      }
      else {
        gp.isPowered = false;
      }
      gp.distToPS = this.graph.get(gp);
    }
  }

  // Determines if two GamePieces are connected
  boolean piecesConnected(GamePiece target, GamePiece other) {
    return piecesConnectedAccumulator(target, other, new ArrayList<GamePiece>());
  }

  // Determines if two GamePieces are connected using accumulator
  boolean piecesConnectedAccumulator(GamePiece target, GamePiece other,
      ArrayList<GamePiece> soFar) {
    return leftConnected(target, other, soFar) || rightConnected(target, other, soFar)
        || topConnected(target, other, soFar) || bottomConnected(target, other, soFar);
  }

  // Determines if GamePiece on left is connected
  boolean leftConnected(GamePiece target, GamePiece other, ArrayList<GamePiece> soFar) {
    int rowVal = target.row;
    int colVal = target.col;

    if (colVal > 0 && target.left && this.board.get(rowVal).get(colVal - 1).right) {
      if (soFar.contains(this.board.get(rowVal).get(colVal - 1))) {
        // Do Nothing
      }
      else if (this.board.get(rowVal).get(colVal - 1).samePiece(other)) {
        return true;
      }
      else {
        soFar.add(this.board.get(rowVal).get(colVal - 1));
        return piecesConnectedAccumulator(this.board.get(rowVal).get(colVal - 1), other, soFar);
      }
    }
    return false;
  }

  // Determines if GamePiece on right is connected
  boolean rightConnected(GamePiece target, GamePiece other, ArrayList<GamePiece> soFar) {
    int rowVal = target.row;
    int colVal = target.col;

    if (colVal < this.width / GamePiece.CELL_LENGTH - 1 && target.right
        && this.board.get(rowVal).get(colVal + 1).left) {
      if (soFar.contains(this.board.get(rowVal).get(colVal + 1))) {
        // Do Nothing
      }
      else if (this.board.get(rowVal).get(colVal + 1).samePiece(other)) {
        return true;
      }
      else {
        soFar.add(this.board.get(rowVal).get(colVal + 1));
        return piecesConnectedAccumulator(this.board.get(rowVal).get(colVal + 1), other, soFar);
      }
    }
    return false;
  }

  // Determines if GamePiece above is connected
  boolean topConnected(GamePiece target, GamePiece other, ArrayList<GamePiece> soFar) {
    int rowVal = target.row;
    int colVal = target.col;

    if (rowVal > 0 && target.top && this.board.get(rowVal - 1).get(colVal).bottom) {
      if (soFar.contains(this.board.get(rowVal - 1).get(colVal))) {
        // Do Nothing
      }
      else if (this.board.get(rowVal - 1).get(colVal).samePiece(other)) {
        return true;
      }
      else {
        soFar.add(this.board.get(rowVal - 1).get(colVal));
        return piecesConnectedAccumulator(this.board.get(rowVal - 1).get(colVal), other, soFar);
      }
    }
    return false;
  }

  // Determines if GamePiece below is connected
  boolean bottomConnected(GamePiece target, GamePiece other, ArrayList<GamePiece> soFar) {
    int rowVal = target.row;
    int colVal = target.col;

    if (rowVal < this.height / GamePiece.CELL_LENGTH - 1 && target.bottom
        && this.board.get(rowVal + 1).get(colVal).top) {
      if (soFar.contains(this.board.get(rowVal + 1).get(colVal))) {
        // Do Nothing
      }
      else if (this.board.get(rowVal + 1).get(colVal).samePiece(other)) {
        return true;
      }
      else {
        soFar.add(this.board.get(rowVal + 1).get(colVal));
        return piecesConnectedAccumulator(this.board.get(rowVal + 1).get(colVal), other, soFar);
      }
    }
    return false;
  }

  // Determines if two neighbors are connected
  boolean twoPiecesConnected(GamePiece target, GamePiece other) {
    if (target.samePiece(other)) {
      return true;
    }

    // target is ABOVE other
    if (target.row + 1 == other.row && target.col == other.col) {
      return target.bottom && other.top;
    } // target is BELOW other
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

  // Flattens the graph into a list of GamePieces
  void getNodes() {
    for (ArrayList<GamePiece> row : this.board) {
      for (GamePiece gp : row) {
        this.nodes.add(gp);
      }
    }
  }

  // Creates the initial hashmap with all distances set to -1
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

      // Check ABOVE target
      if (rowVal > 0 && this.twoPiecesConnected(listFirst, this.board.get(rowVal - 1).get(colVal))
          && !(soFar.contains(this.board.get(rowVal - 1).get(colVal)))) {

        worklist.add(this.board.get(rowVal - 1).get(colVal));
        this.graph.put(this.board.get(rowVal - 1).get(colVal),
            this.graph.get(this.board.get(rowVal).get(colVal)) + 1);

        if (graph.get(this.board.get(rowVal - 1).get(colVal)) > graph.get(lastFound)) {
          lastFound = this.board.get(rowVal - 1).get(colVal);
        }
        soFar.add(this.board.get(rowVal - 1).get(colVal));
      }

      // Check BELOW target
      if (rowVal + 1 < this.height / GamePiece.CELL_LENGTH
          && this.twoPiecesConnected(listFirst, this.board.get(rowVal + 1).get(colVal))
          && !(soFar.contains(this.board.get(rowVal + 1).get(colVal)))) {

        worklist.add(this.board.get(rowVal + 1).get(colVal));
        this.graph.put(this.board.get(rowVal + 1).get(colVal),
            this.graph.get(this.board.get(rowVal).get(colVal)) + 1);

        if (graph.get(this.board.get(rowVal + 1).get(colVal)) > graph.get(lastFound)) {
          lastFound = this.board.get(rowVal + 1).get(colVal);
        }
        soFar.add(this.board.get(rowVal + 1).get(colVal));
      }

      // Check LEFT OF target
      if (colVal > 0 && this.twoPiecesConnected(listFirst, this.board.get(rowVal).get(colVal - 1))
          && !(soFar.contains(this.board.get(rowVal).get(colVal - 1)))) {

        worklist.add(this.board.get(rowVal).get(colVal - 1));
        this.graph.put(this.board.get(rowVal).get(colVal - 1),
            this.graph.get(this.board.get(rowVal).get(colVal)) + 1);

        if (graph.get(this.board.get(rowVal).get(colVal - 1)) > graph.get(lastFound)) {
          lastFound = this.board.get(rowVal).get(colVal - 1);
        }
        soFar.add(this.board.get(rowVal).get(colVal - 1));
      }

      // Check RIGHT OF target
      if (colVal + 1 < this.width / GamePiece.CELL_LENGTH
          && this.twoPiecesConnected(listFirst, this.board.get(rowVal).get(colVal + 1))
          && !(soFar.contains(this.board.get(rowVal).get(colVal + 1)))) {

        worklist.add(this.board.get(rowVal).get(colVal + 1));
        this.graph.put(this.board.get(rowVal).get(colVal + 1),
            this.graph.get(this.board.get(rowVal).get(colVal)) + 1);

        if (graph.get(this.board.get(rowVal).get(colVal + 1)) > graph.get(lastFound)) {
          lastFound = this.board.get(rowVal).get(colVal + 1);
        }
        soFar.add(this.board.get(rowVal).get(colVal + 1));
      }
      worklist.remove(0);
    }

    return lastFound;
  }

  // Calculates the radius from the power station
  int calcRadius() {
    GamePiece lastFound = setDepths(this.powerRow, this.powerCol);
    lastFound = setDepths(lastFound.row, lastFound.col);
    return (this.graph.get(lastFound) / 2) + 1;
  }

  // Makes the scene with all the game pieces drawn.
  // Now integrates timer, score, etc.
  public WorldScene makeScene() {
    int extraSpace = this.boardHeight - this.height;
    int indentSpace = extraSpace / 10;
    this.radius = calcRadius();
    this.getPowered();

    WorldImage extraSpaceRect = new RectangleImage(this.width, extraSpace, OutlineMode.SOLID,
        Color.DARK_GRAY);
    WorldImage time = new TextImage("Time: " + this.processTime(), this.width / 22, Color.white);
    WorldImage score = new TextImage("Moves: " + this.score, this.width / 22, Color.white);
    WorldImage gameTitle = new TextImage(this.processTitle(), this.width / 18, Color.white);
    WorldImage resetText = new TextImage("RESET", extraSpace / 8, Color.white);
    WorldImage resetButton = new RectangleImage(extraSpace / 2, extraSpace / 4, OutlineMode.SOLID,
        Color.black);

    resetButton = new OverlayImage(resetText, resetButton);

    extraSpaceRect = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.TOP, time, -indentSpace,
        -indentSpace, extraSpaceRect);
    extraSpaceRect = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.BOTTOM, score, -indentSpace,
        indentSpace, extraSpaceRect);
    extraSpaceRect = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.MIDDLE, gameTitle,
        -indentSpace * 3, 0, extraSpaceRect);
    extraSpaceRect = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, resetButton,
        indentSpace, 0, extraSpaceRect);

    WorldScene scene = new WorldScene(this.width, this.height);

    for (ArrayList<GamePiece> row : this.board) {
      for (GamePiece cell : row) {
        scene.placeImageXY(cell.drawPiece(this.radius),
            cell.col * GamePiece.CELL_LENGTH + GamePiece.CELL_LENGTH / 2,
            (cell.row * GamePiece.CELL_LENGTH + GamePiece.CELL_LENGTH / 2) + extraSpace);
      }
    }
    scene.placeImageXY(extraSpaceRect, this.width / 2, extraSpace / 2);
    return scene;
  }

  // Returns the title of the game depending on the boardType the user entered
  String processTitle() {
    if (this.boardType == 0) {
      return "Manual Generation";
    }
    else if (this.boardType == 1) {
      return "Fractal Puzzle";
    }
    else {
      return "Kruskal's Enigma";
    }
  }

  // Returns the correctly formatted time as String, accounting for minutes
  String processTime() {
    int seconds = (int) (System.currentTimeMillis() / 1000) - currSec;

    int min = seconds / 60;
    seconds = seconds % 60;

    String minStr = "";
    String secStr = "";

    if (min < 10) {
      minStr = "0" + min;
    }
    else {
      minStr = ((Integer) min).toString();
    }
    if (seconds < 10) {
      secStr = "0" + seconds;
    }
    else {
      secStr = ((Integer) seconds).toString();
    }
    return minStr + ":" + secStr;
  }

  // Not used anymore
  // Creates a board with the manual generation
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

  // NOT USED FOR PART 3
  // Creates the game board using a subdivision algorithm for fractal-like wiring
  public void fractalBoard(int numRows, int numCols, int currRow, int currCol) {
    int startRow = currRow;
    int startCol = currCol;

    if (numRows == 1 || numCols == 1) {
      // At the base case of one row or one column, irrespective
      // of the other dimension, the program should stop. No U should be drawn.
      // This is done because we shouldn't do anything if it is just one row/col
    }

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

    if (numRows == 1 || numCols == 1 || numCols == 2) {
      // We don't do anything if it is just one row/col or two columns
    }
    // When there is only one row, all pieces should have the top field be true.
    else if (numRows == 1 && numCols > 2) {
      this.fractalBoard(1, (int) Math.ceil(numCols / 2), currRow, currCol);
      this.fractalBoard(1, numCols / 2, currRow, (int) Math.ceil(currCol / 2));
    }
    // Other base cases:
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
    // Sets the powerstation to be the middle of the top row
    this.board.get(0).get(this.width / GamePiece.CELL_LENGTH / 2).powerStation = true;
    this.powerCol = this.width / GamePiece.CELL_LENGTH / 2;
    this.powerRow = 0;
  }

  // Handles all clicking when clicking within the game board
  public void onMouseClicked(Posn mousePos, String button) {

    int extraSpace = this.boardHeight - this.height;
    int buttonIndentSpace = extraSpace / 10;
    int resetButtonHeight = extraSpace / 4;
    int resetButtonWidth = extraSpace / 2;

    if (mousePos.y >= extraSpace) {
      int posX = mousePos.x / GamePiece.CELL_LENGTH;
      int posY = (mousePos.y - extraSpace) / GamePiece.CELL_LENGTH;

      GamePiece gp = this.board.get(posY).get(posX);

      if (button.equals("LeftButton")
          && (posY <= this.boardHeight && 0 <= posY && posX <= this.width && 0 <= posX)) {
        gp.rotate();
      }
      this.score++;
    }
    // Reset button:
    if (button.equals("LeftButton") && mousePos.y >= extraSpace / 2 - (resetButtonHeight / 2)
        && mousePos.y <= extraSpace / 2 + (resetButtonHeight / 2)
        && mousePos.x >= this.width - buttonIndentSpace - resetButtonWidth
        && mousePos.x <= this.width - buttonIndentSpace) {
      this.score = 0;
      currSec = (int) (System.currentTimeMillis() / 1000);

      if (this.boardType == 0) {
        this.board = this.makeBoard();
      }
      else if (this.boardType == 1) {
        this.board = this.manualBoard();
        this.fractalBoard(this.height / GamePiece.CELL_LENGTH, this.width / GamePiece.CELL_LENGTH,
            0, 0);
        this.board.get(0).get(this.width / GamePiece.CELL_LENGTH / 2).powerStation = true;
        this.powerCol = this.width / GamePiece.CELL_LENGTH / 2;
        this.powerRow = 0;
        this.nodes = new ArrayList<GamePiece>();
        this.getNodes();
      }
      else if (this.boardType == 2) {
        this.rotatePieces();
        this.board.get(this.powerRow).get(this.powerCol).powerStation = false;
        this.powerCol = 0;
        this.powerRow = 0;
        this.board.get(this.powerRow).get(this.powerCol).powerStation = true;
      }
    }
  }

  // Handles all keys clicked (to move the powerstation)
  public void onKeyEvent(String key) {
    if (key.equals("left")) {
      if ((this.powerCol - 1 > -1)
          && twoPiecesConnected(this.board.get(this.powerRow).get(this.powerCol),
              this.board.get(this.powerRow).get(this.powerCol - 1))) {
        this.board.get(this.powerRow).get(this.powerCol).powerStation = false;
        this.board.get(this.powerRow).get(this.powerCol - 1).powerStation = true;
        this.powerCol--;
        this.score++;
      }
    }

    else if (key.equals("right")) {
      if ((this.powerCol + 1 < this.width / GamePiece.CELL_LENGTH)
          && twoPiecesConnected(this.board.get(this.powerRow).get(this.powerCol),
              this.board.get(this.powerRow).get(this.powerCol + 1))) {
        this.board.get(this.powerRow).get(this.powerCol).powerStation = false;
        this.board.get(this.powerRow).get(this.powerCol + 1).powerStation = true;
        this.powerCol++;
        this.score++;
      }
    }

    else if (key.equals("up")) {
      if ((this.powerRow - 1 >= 0)
          && twoPiecesConnected(this.board.get(this.powerRow).get(this.powerCol),
              this.board.get(this.powerRow - 1).get(this.powerCol))) {
        this.board.get(this.powerRow).get(this.powerCol).powerStation = false;
        this.board.get(this.powerRow - 1).get(this.powerCol).powerStation = true;
        this.powerRow--;
        this.score++;
      }
    }

    else if (key.equals("down")) {
      if ((this.powerRow + 1 < (this.height / GamePiece.CELL_LENGTH))
          && twoPiecesConnected(this.board.get(this.powerRow).get(this.powerCol),
              this.board.get(this.powerRow + 1).get(this.powerCol))) {
        this.board.get(this.powerRow).get(this.powerCol).powerStation = false;
        this.board.get(this.powerRow + 1).get(this.powerCol).powerStation = true;
        this.powerRow++;
        this.score++;
      }
    }
  }

  // Determines if the game is won, else keep going
  public WorldEnd worldEnds() {
    if (allConnected()) {
      return new WorldEnd(true, this.finalScene());
    }
    else {
      return new WorldEnd(false, this.makeScene());
    }
  }

  // Determines if all GamePieces are powered up and connected to the powerstation
  boolean allConnected() {
    for (GamePiece gp : this.nodes) {
      if (!gp.isPowered) {
        return false;
      }
    }
    return true;
  }

  // The end scene that congratulates the user if game is over (ie. won)
  public WorldScene finalScene() {
    int fontSize = this.height / 8;
    WorldScene ws = this.makeScene();
    WorldImage score = new TextImage("Score: " + this.score, fontSize, Color.MAGENTA);
    WorldImage time = new TextImage("Time: " + this.processTime(), fontSize, Color.MAGENTA);
    WorldImage win = new TextImage("You won!", fontSize, Color.MAGENTA);
    WorldImage finalImage = new AboveImage(win, score, time);

    ws.placeImageXY(finalImage, this.width / 2, this.boardHeight / 2);
    return ws;
  }
}

// A function object comparator that helps sort the Edges by weight
class SortByWeight implements Comparator<Edge> {

  // Compares two given edges and returns -1 if the first is
  // smaller, 1 if the first is greater, and 0 if they are equal.
  public int compare(Edge edge1, Edge edge2) {
    if (edge1.weight < edge2.weight) {
      return -1;
    }
    else if (edge1.weight > edge2.weight) {
      return 1;
    }
    else {
      return 0;
    }
  }
}

// Represents one of the GamePieces
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
  int distToPS;
  ArrayList<GamePiece> neighbors;

  // Constructor where the GamePiece doesn't know its neighbors.
  GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom,
      boolean powerStation) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.isPowered = false;
    this.distToPS = 0;
    this.neighbors = new ArrayList<GamePiece>();
  }

  // Constructor where the GamePiece knows its neighbors.
  GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom,
      boolean powerStation, ArrayList<GamePiece> neighbors) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.isPowered = false;
    this.distToPS = 0;
    this.neighbors = new ArrayList<GamePiece>();
  }

  // Determines if the given piece is the same as this one
  boolean samePiece(GamePiece given) {
    return (this.row == given.row && this.col == given.col);
  }

  // Assigns neighbors to this GamePiece depending on its available neighbors
  void assignNeighbors(ArrayList<ArrayList<GamePiece>> board) {
    // If top left piece:
    if (this.row == 0 && this.col == 0) {
      this.neighbors.add(board.get(this.row).get(this.col + 1));
      this.neighbors.add(board.get(this.row + 1).get(this.col));
    }
    // If bottom left piece:
    else if (this.row == (board.size() - 1) && this.col == 0) {
      this.neighbors.add(board.get(this.row - 1).get(this.col));
      this.neighbors.add(board.get(this.row).get(this.col + 1));
    }
    // If top right piece:
    else if (this.row == 0 && this.col == (board.get(0).size() - 1)) {
      this.neighbors.add(board.get(this.row).get(this.col - 1));
      this.neighbors.add(board.get(this.row + 1).get(this.col));
    }
    // If bottom right piece:
    else if (this.row == (board.size() - 1) && this.col == (board.get(0).size() - 1)) {
      this.neighbors.add(board.get(this.row - 1).get(this.col));
      this.neighbors.add(board.get(this.row).get(this.col - 1));
    }
    // If top row:
    else if (this.row == 0) {
      this.neighbors.add(board.get(this.row).get(this.col - 1));
      this.neighbors.add(board.get(this.row).get(this.col + 1));
      this.neighbors.add(board.get(this.row + 1).get(this.col));
    }
    // If bottom row:
    else if (this.row == (board.size() - 1)) {
      this.neighbors.add(board.get(this.row).get(this.col - 1));
      this.neighbors.add(board.get(this.row).get(this.col + 1));
      this.neighbors.add(board.get(this.row - 1).get(this.col));
    }
    // If leftmost col:
    else if (this.col == 0) {
      this.neighbors.add(board.get(this.row - 1).get(this.col));
      this.neighbors.add(board.get(this.row + 1).get(this.col));
      this.neighbors.add(board.get(this.row).get(this.col + 1));
    }
    // If rightmost col:
    else if (this.col == (board.get(0).size() - 1)) {
      this.neighbors.add(board.get(this.row - 1).get(this.col));
      this.neighbors.add(board.get(this.row + 1).get(this.col));
      this.neighbors.add(board.get(this.row).get(this.col - 1));
    }
    else {
      // adds the piece to the LEFT
      this.neighbors.add(board.get(this.row).get(this.col - 1));
      // adds the piece to the RIGHT
      this.neighbors.add(board.get(this.row).get(this.col + 1));
      // adds the piece ABOVE
      this.neighbors.add(board.get(this.row - 1).get(this.col));
      // adds the piece BELOW
      this.neighbors.add(board.get(this.row + 1).get(this.col));
    }
  }

  // Draws each individual GamePiece
  WorldImage drawPiece(int radius) {
    WorldImage outline = new RectangleImage(CELL_LENGTH, CELL_LENGTH, OutlineMode.OUTLINE,
        Color.BLACK);
    WorldImage result = new OverlayImage(
        new RectangleImage(CELL_LENGTH, CELL_LENGTH, OutlineMode.SOLID, Color.DARK_GRAY),
        new EmptyImage());

    if (this.isPowered) {
      LineImage vertLine = new LineImage(new Posn(0, CELL_LENGTH / 2),
          new Color(255, 255 - (this.distToPS * 255 / radius), 0));
      LineImage horLine = new LineImage(new Posn(CELL_LENGTH / 2, 0),
          new Color(255, 255 - (this.distToPS * 255 / radius), 0));

      // Connected to the left
      if (this.left) {
        result = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.PINHOLE, horLine, 0, 0, result);
      }
      if (this.right) {
        result = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.PINHOLE, horLine, 0, 0,
            result);
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
    }
    else {
      LineImage vertLineNP = new LineImage(new Posn(0, CELL_LENGTH / 2), Color.GRAY);
      LineImage horLineNP = new LineImage(new Posn(CELL_LENGTH / 2, 0), Color.GRAY);

      // Connected to the left
      if (this.left) {
        result = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.PINHOLE, horLineNP, 0, 0,
            result);
      }
      if (this.right) {
        result = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.PINHOLE, horLineNP, 0, 0,
            result);
      }
      if (this.top) {
        result = new OverlayOffsetAlign(AlignModeX.PINHOLE, AlignModeY.TOP, vertLineNP, 0, 0,
            result);
      }
      if (this.bottom) {
        result = new OverlayOffsetAlign(AlignModeX.PINHOLE, AlignModeY.BOTTOM, vertLineNP, 0, 0,
            result);
      }
      if (this.powerStation) {
        WorldImage star = new StarImage(15, OutlineMode.SOLID, Color.CYAN);
        result = new OverlayImage(star, result);
      }
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

  // Returns a String representation of each GamePiece's location
  public String toString() {
    return "(" + new Integer(this.col + 1).toString() + ", " + new Integer(this.row + 1).toString()
        + ")";
  }
}

// Represents a wire; used for Kruskal's algorithm
class Edge {
  private static final Random RANDOBJ = new Random(1);

  GamePiece fromNode;
  GamePiece toNode;
  int weight;

  Edge(GamePiece fromNode, GamePiece toNode) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    // Can have a weight of any number [0, 99]
    this.weight = Edge.RANDOBJ.nextInt(100);
  }

  // EFFECT: Turns on certain connects to connect an actual edge
  // Connects the GamePieces that are part of an edge
  void connectNodes() {
    if (this.fromNode.col == this.toNode.col) {
      if (this.fromNode.row > this.toNode.row) {
        this.fromNode.top = true;
        this.toNode.bottom = true;
      }
      else {
        this.fromNode.bottom = true;
        this.toNode.top = true;
      }
    }
    else {
      if (this.fromNode.col > this.toNode.col) {
        this.fromNode.left = true;
        this.toNode.right = true;
      }
      else {
        this.fromNode.right = true;
        this.toNode.left = true;
      }
    }
  }
}

// All the examples and tests.
class ExamplesGame {
  LightEmAll test;
  LightEmAll twox2Power;
  LightEmAll threex3;
  LightEmAll threex3Power;
  LightEmAll fourx4;
  LightEmAll fourx4Power;
  LightEmAll fivex5;
  LightEmAll fivex5Power;
  LightEmAll kruskalsBoard;

  void initData() {
    // To use with big-bang
    test = new LightEmAll(10, 12, 1);
    twox2Power = new LightEmAll(2, 2, 1);
    // To test a 3x3 grid
    threex3 = new LightEmAll(3, 3, 0);
    // To test a 3x3 grid Powered
    threex3Power = new LightEmAll(3, 3, 1);
    // To test a 4x4 grid that
    fourx4Power = new LightEmAll(4, 4, 1);
    // To test a 4x4 grid
    fourx4 = new LightEmAll(4, 4, 0);
    // To test a 5x5 grid
    fivex5 = new LightEmAll(5, 5, 0);
    // To test a 5x5 grid
    fivex5Power = new LightEmAll(5, 5, 1);

    // To test kruskal's:
    kruskalsBoard = new LightEmAll(8, 8, 2);
  }

  // Runs the program with a predetermined, easy-to-solve pattern.
  void testMain(Tester t) {
    initData();
    this.kruskalsBoard.bigBang(this.kruskalsBoard.width, this.kruskalsBoard.boardHeight, .003);
  }

  // Testing the makeScene() method
  void testMakeScene(Tester t) {
    initData();

    int extraSpace3 = this.threex3Power.boardHeight - this.threex3Power.height;
    int indentSpace3 = extraSpace3 / 10;
    this.threex3Power.radius = this.threex3Power.calcRadius();
    this.threex3Power.getPowered();

    this.threex3Power.currSec = (int) (System.currentTimeMillis() / 1000);

    WorldImage extraSpaceRect3 = new RectangleImage(this.threex3Power.width, extraSpace3,
        OutlineMode.SOLID, Color.DARK_GRAY);
    WorldImage time3 = new TextImage("Time: " + this.threex3Power.processTime(),
        this.threex3Power.width / 22, Color.white);
    WorldImage score3 = new TextImage("Moves: " + this.threex3Power.score,
        this.threex3Power.width / 22, Color.white);
    WorldImage gameTitle3 = new TextImage(this.threex3Power.processTitle(),
        this.threex3Power.width / 18, Color.white);
    WorldImage resetText3 = new TextImage("RESET", extraSpace3 / 8, Color.white);
    WorldImage resetButton3 = new RectangleImage(extraSpace3 / 2, extraSpace3 / 4,
        OutlineMode.SOLID, Color.black);
    resetButton3 = new OverlayImage(resetText3, resetButton3);

    extraSpaceRect3 = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.TOP, time3, -indentSpace3,
        -indentSpace3, extraSpaceRect3);
    extraSpaceRect3 = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.BOTTOM, score3,
        -indentSpace3, indentSpace3, extraSpaceRect3);
    extraSpaceRect3 = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.MIDDLE, gameTitle3,
        -indentSpace3 * 3, 0, extraSpaceRect3);
    extraSpaceRect3 = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, resetButton3,
        indentSpace3, 0, extraSpaceRect3);

    WorldScene sceneThree = new WorldScene(this.threex3Power.width, this.threex3Power.height);

    for (ArrayList<GamePiece> row : this.threex3Power.board) {
      for (GamePiece cell : row) {
        sceneThree.placeImageXY(cell.drawPiece(this.threex3Power.radius),
            cell.col * GamePiece.CELL_LENGTH + GamePiece.CELL_LENGTH / 2,
            (cell.row * GamePiece.CELL_LENGTH + GamePiece.CELL_LENGTH / 2) + extraSpace3);
      }
    }
    sceneThree.placeImageXY(extraSpaceRect3, this.threex3Power.width / 2, extraSpace3 / 2);

    int extraSpace4 = this.fourx4Power.boardHeight - this.fourx4Power.height;
    int indentSpace4 = extraSpace4 / 10;
    this.fourx4Power.radius = this.fourx4Power.calcRadius();
    this.fourx4Power.getPowered();

    this.fourx4Power.currSec = (int) (System.currentTimeMillis() / 1000);

    WorldImage extraSpaceRect4 = new RectangleImage(this.fourx4Power.width, extraSpace4,
        OutlineMode.SOLID, Color.DARK_GRAY);
    WorldImage time4 = new TextImage("Time: " + this.fourx4Power.processTime(),
        this.fourx4Power.width / 22, Color.white);
    WorldImage score4 = new TextImage("Moves: " + this.fourx4Power.score,
        this.fourx4Power.width / 22, Color.white);
    WorldImage gameTitle4 = new TextImage(this.fourx4Power.processTitle(),
        this.fourx4Power.width / 18, Color.white);
    WorldImage resetText4 = new TextImage("RESET", extraSpace4 / 8, Color.white);
    WorldImage resetButton4 = new RectangleImage(extraSpace4 / 2, extraSpace4 / 4,
        OutlineMode.SOLID, Color.black);
    resetButton4 = new OverlayImage(resetText4, resetButton4);

    extraSpaceRect4 = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.TOP, time4, -indentSpace4,
        -indentSpace4, extraSpaceRect4);
    extraSpaceRect4 = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.BOTTOM, score4,
        -indentSpace4, indentSpace4, extraSpaceRect4);
    extraSpaceRect4 = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.MIDDLE, gameTitle4,
        -indentSpace4 * 3, 0, extraSpaceRect4);
    extraSpaceRect4 = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, resetButton4,
        indentSpace4, 0, extraSpaceRect4);

    WorldScene sceneFour = new WorldScene(this.fourx4Power.width, this.fourx4Power.height);

    for (ArrayList<GamePiece> row : this.fourx4Power.board) {
      for (GamePiece cell : row) {
        sceneFour.placeImageXY(cell.drawPiece(this.fourx4Power.radius),
            cell.col * GamePiece.CELL_LENGTH + GamePiece.CELL_LENGTH / 2,
            (cell.row * GamePiece.CELL_LENGTH + GamePiece.CELL_LENGTH / 2) + extraSpace4);
      }
    }
    sceneFour.placeImageXY(extraSpaceRect4, this.fourx4Power.width / 2, extraSpace4 / 2);

    /* Issue with the time */
    // t.checkExpect(this.threex3Power.makeScene(), sceneThree);
    // t.checkExpect(this.fourx4Power.makeScene(), sceneFour);
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

  // Testing the method manualBoad()
  void testManualBoard(Tester t) {
    initData();

    ArrayList<ArrayList<GamePiece>> answer = new ArrayList<ArrayList<GamePiece>>();
    ArrayList<GamePiece> firstRow = new ArrayList<GamePiece>();
    ArrayList<GamePiece> secRow = new ArrayList<GamePiece>();
    ArrayList<GamePiece> thirdRow = new ArrayList<GamePiece>();
    GamePiece one = new GamePiece(0, 0, false, false, false, false, false);
    GamePiece two = new GamePiece(0, 1, false, false, false, false, false);
    GamePiece three = new GamePiece(0, 2, false, false, false, false, false);
    GamePiece four = new GamePiece(1, 0, false, false, false, false, false);
    GamePiece five = new GamePiece(1, 1, false, false, false, false, false);
    GamePiece six = new GamePiece(1, 2, false, false, false, false, false);
    GamePiece seven = new GamePiece(2, 0, false, false, false, false, false);
    GamePiece eight = new GamePiece(2, 1, false, false, false, false, false);
    GamePiece nine = new GamePiece(2, 2, false, false, false, false, false);

    firstRow.add(one);
    firstRow.add(two);
    firstRow.add(three);
    secRow.add(four);
    secRow.add(five);
    secRow.add(six);
    thirdRow.add(seven);
    thirdRow.add(eight);
    thirdRow.add(nine);

    answer.addAll(Arrays.asList(firstRow, secRow, thirdRow));

    t.checkExpect(this.threex3.manualBoard(), answer);
  }

  // Testing the whether clicking rotates the game pieces correctly.
  void testOnMouseClicked(Tester t) {
    initData();
    t.checkExpect(this.threex3.board.get(0).get(0).bottom, false);
    this.threex3.onMouseClicked(new Posn(10, 10), "RightButton");
    t.checkExpect(this.threex3.board.get(0).get(0).bottom, false);
    this.threex3.onMouseClicked(new Posn(10, 34), "LeftButton");
    t.checkExpect(this.threex3.board.get(0).get(0).bottom, true);
    this.threex3.onMouseClicked(new Posn(10, 34), "LeftButton");
    t.checkExpect(this.threex3.board.get(0).get(0).bottom, false);
    t.checkExpect(this.fivex5.board.get(2).get(2).powerStation, true);
    t.checkExpect(this.fivex5.board.get(2).get(2).right, true);
    this.fivex5.onMouseClicked(new Posn(2 * GamePiece.CELL_LENGTH, 2 * GamePiece.CELL_LENGTH),
        "LeftButton");
    t.checkExpect(this.fivex5.board.get(2).get(2).right, true);
    t.checkExpect(this.fivex5.board.get(2).get(2).powerStation, true);
    this.kruskalsBoard.onMouseClicked(new Posn(294, 34), "LeftButton");
    t.checkExpect(this.kruskalsBoard.score, 0);
  }

  // Testing whether the pieces are connected
  void testTwoPiecesConnected(Tester t) {
    initData();
    t.checkExpect(this.threex3.twoPiecesConnected(this.threex3.board.get(0).get(0),
        this.threex3.board.get(1).get(0)), false);
    this.threex3.onMouseClicked(new Posn(10, 84), "LeftButton");
    t.checkExpect(this.threex3.twoPiecesConnected(this.threex3.board.get(0).get(0),
        this.threex3.board.get(0).get(1)), true);
    t.checkExpect(this.fourx4.twoPiecesConnected(this.fourx4.board.get(2).get(2),
        this.fourx4.board.get(2).get(3)), true);
    this.fourx4.onMouseClicked(
        new Posn(2 * GamePiece.CELL_LENGTH + 5, 3 * GamePiece.CELL_LENGTH + 5), "LeftButton");
    t.checkExpect(this.fourx4.twoPiecesConnected(this.fourx4.board.get(2).get(2),
        this.fourx4.board.get(2).get(3)), true);

    t.checkExpect(this.fivex5Power.twoPiecesConnected(this.fivex5Power.board.get(0).get(2),
        this.fivex5Power.board.get(0).get(1)), false);
    t.checkExpect(this.fivex5Power.twoPiecesConnected(this.fivex5Power.board.get(0).get(2),
        this.fivex5Power.board.get(1).get(2)), true);
    t.checkExpect(this.fivex5Power.twoPiecesConnected(this.fivex5Power.board.get(0).get(2),
        this.fivex5Power.board.get(2).get(1)), false);

    t.checkExpect(this.fourx4Power.twoPiecesConnected(this.fourx4Power.board.get(0).get(0),
        this.fourx4Power.board.get(1).get(0)), true);
    this.fourx4Power.board.get(1).get(0).rotate();
    t.checkExpect(this.fourx4Power.twoPiecesConnected(this.fourx4Power.board.get(0).get(0),
        this.fourx4Power.board.get(1).get(0)), false);

    initData();
    t.checkExpect(this.fourx4Power.twoPiecesConnected(this.fourx4Power.board.get(1).get(0),
        this.fourx4Power.board.get(2).get(0)), true);
    this.fourx4Power.board.get(1).get(0).rotate();
    this.fourx4Power.board.get(1).get(0).rotate();
    this.fourx4Power.board.get(1).get(0).rotate();
    t.checkExpect(this.fourx4Power.twoPiecesConnected(this.fourx4Power.board.get(1).get(0),
        this.fourx4Power.board.get(2).get(0)), false);
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
    // piece with coordinates (2,0)
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

  // testing getPowered()
  void testGetPowered(Tester t) {
    initData();
    t.checkExpect(this.fourx4Power.board.get(0).get(0).isPowered, false);
    t.checkExpect(this.fourx4Power.board.get(1).get(0).isPowered, false);
    t.checkExpect(this.fourx4Power.board.get(1).get(0).isPowered, false);
    t.checkExpect(this.fourx4Power.board.get(3).get(1).isPowered, true);
    t.checkExpect(this.fourx4Power.board.get(3).get(2).isPowered, true);
  }

  // testing piecesConnected()
  void testPiecesConnected(Tester t) {
    initData();
    // piecesConnected()
    t.checkExpect(this.fourx4Power.piecesConnected(this.fourx4Power.board.get(0).get(0),
        this.fourx4Power.board.get(3).get(0)), true);
    this.fourx4Power.board.get(1).get(0).rotate();
    this.fourx4Power.board.get(1).get(0).rotate();
    this.fourx4Power.board.get(1).get(0).rotate();
    t.checkExpect(this.fourx4Power.piecesConnected(this.fourx4Power.board.get(1).get(0),
        this.fourx4Power.board.get(3).get(0)), false);

    initData();
    // piecesConnectedAccumulator()
    t.checkExpect(this.fourx4Power.piecesConnectedAccumulator(this.fourx4Power.board.get(0).get(0),
        this.fourx4Power.board.get(3).get(0), new ArrayList<GamePiece>()), true);
    this.fourx4Power.board.get(1).get(0).rotate();
    this.fourx4Power.board.get(1).get(0).rotate();
    this.fourx4Power.board.get(1).get(0).rotate();
    t.checkExpect(this.fourx4Power.piecesConnectedAccumulator(this.fourx4Power.board.get(0).get(0),
        this.fourx4Power.board.get(3).get(0), new ArrayList<GamePiece>()), false);

    initData();
    // leftConnected()
    t.checkExpect(this.fourx4Power.leftConnected(this.fourx4Power.board.get(1).get(2),
        this.fourx4Power.board.get(3).get(0), new ArrayList<GamePiece>()), false);
    t.checkExpect(this.fourx4Power.leftConnected(this.fourx4Power.board.get(1).get(3),
        this.fourx4Power.board.get(1).get(2), new ArrayList<GamePiece>()), true);

    // rightConnected()
    t.checkExpect(this.fourx4Power.rightConnected(this.fourx4Power.board.get(1).get(2),
        this.fourx4Power.board.get(3).get(0), new ArrayList<GamePiece>()), true);
    t.checkExpect(this.fourx4Power.leftConnected(this.fourx4Power.board.get(1).get(2),
        this.fourx4Power.board.get(1).get(1), new ArrayList<GamePiece>()), false);

    // topConnected()
    t.checkExpect(this.fourx4Power.topConnected(this.fourx4Power.board.get(1).get(2),
        this.fourx4Power.board.get(0).get(1), new ArrayList<GamePiece>()), true);
    t.checkExpect(this.fourx4Power.leftConnected(this.fourx4Power.board.get(3).get(0),
        this.fourx4Power.board.get(3).get(3), new ArrayList<GamePiece>()), false);

    // bottomConnected()
    t.checkExpect(this.fourx4Power.bottomConnected(this.fourx4Power.board.get(2).get(2),
        this.fourx4Power.board.get(2).get(0), new ArrayList<GamePiece>()), true);
    t.checkExpect(this.fourx4Power.bottomConnected(this.fourx4Power.board.get(3).get(2),
        this.fourx4Power.board.get(3).get(0), new ArrayList<GamePiece>()), false);
  }

  // Testing method getNodes()
  void testGetNodes(Tester t) {
    initData();
    this.fourx4Power.nodes.clear();
    t.checkExpect(this.fourx4Power.nodes.size(), 0);
    this.fourx4Power.getNodes();
    // There are and should be 16 because this is a 4x4
    t.checkExpect(this.fourx4Power.nodes.size(), 16);
  }

  // Testing initHash()
  void testInitHash(Tester t) {
    initData();
    this.fourx4Power.graph.clear();
    t.checkExpect(this.fourx4Power.graph.size(), 0);
    this.fourx4Power.initHash();
    t.checkExpect(this.fourx4Power.graph.size(), 16);
    // Checks if any GamePiece's value in the HashMap is -1 (initially)
    t.checkExpect(this.fourx4Power.graph.get(this.fourx4Power.board.get(0).get(0)), -1);
    t.checkExpect(this.fourx4Power.graph.get(this.fourx4Power.board.get(0).get(3)), -1);
  }

  // Testing setDepths((
  void testSetDepths(Tester t) {
    initData();
    // Finds the farthest GamePiece from the given GamePiece
    t.checkExpect(this.fourx4Power.setDepths(0, 2), this.fourx4Power.board.get(0).get(1));
    t.checkExpect(this.fourx4Power.setDepths(0, 0), this.fourx4Power.board.get(0).get(2));
    t.checkExpect(this.fourx4Power.setDepths(0, 1), this.fourx4Power.board.get(0).get(2));
    t.checkExpect(this.fourx4Power.setDepths(2, 0), this.fourx4Power.board.get(0).get(2));
    t.checkExpect(this.fivex5Power.setDepths(0, 3), this.fivex5Power.board.get(0).get(2));

  }

  // Testing calcRadius
  void testCalcRadius(Tester t) {
    initData();
    t.checkExpect(this.fourx4Power.calcRadius(), 6);
    t.checkExpect(this.fivex5Power.calcRadius(), 8);
  }

  // Testing fractalBoard()
  void testFractalBoard(Tester t) {
    initData();

    t.checkExpect(this.fourx4Power.board.get(0).get(2).powerStation, true);
    t.checkExpect(this.fourx4Power.board.get(0).get(2).bottom, true);
    t.checkExpect(this.fourx4Power.board.get(1).get(2).bottom, false);
    t.checkExpect(this.fourx4Power.board.get(1).get(2).top, true);
    t.checkExpect(this.fourx4Power.board.get(1).get(2).right, true);
    t.checkExpect(this.fourx4Power.board.get(3).get(2).top, true);
    t.checkExpect(this.fourx4Power.board.get(3).get(2).left, true);
    t.checkExpect(this.fourx4Power.board.get(3).get(2).right, true);
    t.checkExpect(this.fourx4Power.board.get(3).get(2).bottom, false);
  }

  // Testing drawPiece()
  void testDrawPiece(Tester t) {
    initData();
    // Top left peice that is not powered
    LineImage vertLineNP1 = new LineImage(new Posn(0, GamePiece.CELL_LENGTH / 2), Color.GRAY);
    WorldImage outline1 = new RectangleImage(GamePiece.CELL_LENGTH, GamePiece.CELL_LENGTH,
        OutlineMode.OUTLINE, Color.BLACK);
    WorldImage result1 = new OverlayImage(new RectangleImage(GamePiece.CELL_LENGTH,
        GamePiece.CELL_LENGTH, OutlineMode.SOLID, Color.DARK_GRAY), new EmptyImage());

    result1 = new OverlayOffsetAlign(AlignModeX.PINHOLE, AlignModeY.BOTTOM, vertLineNP1, 0, 0,
        result1);

    result1 = new OverlayImage(outline1, result1);

    // drawing a star
    LineImage poweredVert = new LineImage(new Posn(0, GamePiece.CELL_LENGTH / 2),
        new Color(255, 255, 0));
    WorldImage outline2 = new RectangleImage(GamePiece.CELL_LENGTH, GamePiece.CELL_LENGTH,
        OutlineMode.OUTLINE, Color.BLACK);
    WorldImage result2 = new OverlayImage(new RectangleImage(GamePiece.CELL_LENGTH,
        GamePiece.CELL_LENGTH, OutlineMode.SOLID, Color.DARK_GRAY), new EmptyImage());

    result2 = new OverlayOffsetAlign(AlignModeX.PINHOLE, AlignModeY.BOTTOM, poweredVert, 0, 0,
        result2);

    WorldImage star = new StarImage(15, OutlineMode.SOLID, Color.CYAN);
    result2 = new OverlayImage(star, result2);

    result2 = new OverlayImage(outline2, result2);

    // drawing cell with top and right true with g value of 213

    LineImage poweredvert2 = new LineImage(new Posn(0, GamePiece.CELL_LENGTH / 2),
        new Color(255, 213, 0));
    LineImage poweredhor2 = new LineImage(new Posn(GamePiece.CELL_LENGTH / 2, 0),
        new Color(255, 213, 0));

    WorldImage outline3 = new RectangleImage(GamePiece.CELL_LENGTH, GamePiece.CELL_LENGTH,
        OutlineMode.OUTLINE, Color.BLACK);
    WorldImage result3 = new OverlayImage(new RectangleImage(GamePiece.CELL_LENGTH,
        GamePiece.CELL_LENGTH, OutlineMode.SOLID, Color.DARK_GRAY), new EmptyImage());

    result3 = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.PINHOLE, poweredhor2, 0, 0,
        result3);
    result3 = new OverlayOffsetAlign(AlignModeX.PINHOLE, AlignModeY.TOP, poweredvert2, 0, 0,
        result3);

    result3 = new OverlayImage(outline3, result3);

    t.checkExpect(this.fourx4Power.board.get(0).get(0).drawPiece(6), result1);
    t.checkExpect(this.fourx4Power.board.get(0).get(2).drawPiece(6), result2);
    t.checkExpect(this.fourx4Power.board.get(1).get(2).drawPiece(6), result3);
  }

  // Testing worldEnds()
  void testWorldEnds(Tester t) {
    initData();
    // t.checkExpect(this.fourx4Power.worldEnds(), new WorldEnd(false,
    // this.fourx4Power.makeScene()));
    // t.checkExpect(this.fivex5Power.worldEnds(), new WorldEnd(false,
    // this.fivex5Power.makeScene()));
    this.twox2Power.onKeyEvent("down");
    this.twox2Power.getPowered();
    // t.checkExpect(this.twox2Power.worldEnds(), new WorldEnd(true,
    // this.twox2Power.finalScene()));
  }

  // Testing the method allConnected()
  void testAllConnected(Tester t) {
    initData();
    t.checkExpect(this.fourx4Power.allConnected(), false);
    t.checkExpect(this.twox2Power.allConnected(), false);
    this.twox2Power.onKeyEvent("down");
    this.twox2Power.getPowered();

    t.checkExpect(this.twox2Power.allConnected(), true);
  }

  // Testing the method finalScene()
  void testFinalScene(Tester t) {
    initData();

    int fontSize = this.twox2Power.height / 8;

    WorldScene result = this.twox2Power.makeScene();

    WorldImage score = new TextImage("Score: " + this.twox2Power.score, fontSize, Color.MAGENTA);
    WorldImage time = new TextImage("Time: " + this.twox2Power.processTime(), fontSize,
        Color.MAGENTA);
    WorldImage win = new TextImage("You won!", fontSize, Color.MAGENTA);
    WorldImage finalImage = new AboveImage(win, score, time);

    result.placeImageXY(finalImage, this.twox2Power.width / 2, this.twox2Power.boardHeight / 2);

    // t.checkExpect(this.twox2Power.finalScene(), result);
  }

  // Tests the method processTitle()
  void testProcessTitle(Tester t) {
    initData();
    t.checkExpect(this.fivex5.processTitle(), "Manual Generation");
    t.checkExpect(this.fivex5Power.processTitle(), "Fractal Puzzle");
    t.checkExpect(this.kruskalsBoard.processTitle(), "Kruskal's Enigma");
  }

  // Tests the method processTime()
  void testProcessTime(Tester t) {
    initData();

    t.checkExpect(this.kruskalsBoard.processTime(), "00:00");
  }

  // Tests the method mstDone()
  void testMstDone(Tester t) {
    initData();
    HashMap<String, String> tester = new HashMap<String, String>();
    tester.put("One", "One");
    tester.put("Two", "Two");
    t.checkExpect(this.kruskalsBoard.mstDone(tester), false);
    tester.put("Two", "One");
    t.checkExpect(this.kruskalsBoard.mstDone(tester), true);
  }

  // Tests the findRep() method
  void testFindRep(Tester t) {
    initData();

    HashMap<String, String> tester = new HashMap<String, String>();
    tester.put("One", "One");
    tester.put("Two", "One");
    tester.put("Three", "Two");

    t.checkExpect(this.kruskalsBoard.findRep(tester, "One"), "One");
    t.checkExpect(this.kruskalsBoard.findRep(tester, "Two"), "One");
    t.checkExpect(this.kruskalsBoard.findRep(tester, "Three"), "One");
  }

  // Tests the connect() method
  void testConnect(Tester t) {
    initData();

    GamePiece gp1 = new GamePiece(0, 0, false, false, false, false, false);
    GamePiece gp2 = new GamePiece(1, 0, false, false, false, false, false);
    GamePiece gp3 = new GamePiece(1, 1, false, false, false, false, false);
    GamePiece gp4 = new GamePiece(0, 1, false, false, false, false, true);

    Edge e1 = new Edge(gp1, gp2);
    Edge e2 = new Edge(gp2, gp3);
    Edge e3 = new Edge(gp3, gp4);

    ArrayList<Edge> loEdge = new ArrayList<Edge>(Arrays.asList(e1, e2, e3));

    this.twox2Power.mst = loEdge;
    this.twox2Power.connect();

    t.checkExpect(gp1.bottom && gp2.top, true);
    t.checkExpect(gp2.right && gp3.left, true);
    t.checkExpect(gp3.top && gp4.bottom, true);
  }

  // Tests the addToMst() method
  void testAddToMST(Tester t) {
    initData();

    t.checkExpect(this.kruskalsBoard.mst.size(), 63);
    t.checkExpect(this.kruskalsBoard.mst.get(0).weight <= this.kruskalsBoard.mst.get(1).weight,
        true);
    t.checkExpect(this.kruskalsBoard.mst.get(1).weight >= this.kruskalsBoard.mst.get(4).weight,
        false);
  }

  // Testing the method existsEdge()
  void testExistsEdge(Tester t) {
    initData();

    Edge one = new Edge(this.kruskalsBoard.board.get(0).get(0),
        this.kruskalsBoard.board.get(0).get(1));
    Edge two = new Edge(this.kruskalsBoard.board.get(1).get(1),
        this.kruskalsBoard.board.get(0).get(1));
    Edge three = new Edge(this.kruskalsBoard.board.get(0).get(0),
        this.kruskalsBoard.board.get(1).get(0));
    ArrayList<Edge> testEdge = new ArrayList<Edge>(Arrays.asList(one, two, three));

    t.checkExpect(this.kruskalsBoard.existsEdge(this.kruskalsBoard.board.get(0).get(0),
        this.kruskalsBoard.board.get(0).get(1), testEdge), true);
    t.checkExpect(this.kruskalsBoard.existsEdge(this.kruskalsBoard.board.get(1).get(0),
        this.kruskalsBoard.board.get(0).get(1), testEdge), false);
  }

  // Testing the method sortEdges()
  void testSortEdges(Tester t) {
    initData();

    this.kruskalsBoard.sortEdges();

    // Sorts all the data already
    t.checkExpect(this.kruskalsBoard.mst.get(0).weight <= this.kruskalsBoard.mst.get(1).weight,
        true);

    t.checkExpect(this.kruskalsBoard.mst.get(1).weight <= this.kruskalsBoard.mst.get(2).weight,
        true);
  }

  // Testing the addAllEdges() method
  void testAddAllEdges(Tester t) {
    initData();
    t.checkExpect(this.kruskalsBoard.allEdges.size(), 112);
  }

  void testRotatePieces(Tester t) {
    initData();

    this.twox2Power.rotatePieces();

    t.checkExpect(this.twox2Power.board.get(0).get(0).left
        || this.twox2Power.board.get(0).get(0).right || this.twox2Power.board.get(0).get(0).top
        || this.twox2Power.board.get(0).get(0).bottom, true);

    t.checkExpect((this.twox2Power.board.get(1).get(0).top
        && this.twox2Power.board.get(1).get(0).right)
        || (this.twox2Power.board.get(1).get(0).right && this.twox2Power.board.get(1).get(0).bottom)
        || (this.twox2Power.board.get(1).get(0).bottom && this.twox2Power.board.get(1).get(0).left)
        || (this.twox2Power.board.get(1).get(0).left && this.twox2Power.board.get(1).get(0).top),
        true);
  }

  // Testing the compare() method
  void testCompare(Tester t) {
    initData();
    this.kruskalsBoard.sortEdges();

    Edge e1 = new Edge(this.kruskalsBoard.board.get(0).get(0),
        this.kruskalsBoard.board.get(0).get(1));
    e1.weight = 10;
    Edge e2 = new Edge(this.kruskalsBoard.board.get(0).get(0),
        this.kruskalsBoard.board.get(0).get(1));
    e2.weight = 15;

    t.checkExpect(new SortByWeight().compare(e1, e2), -1);
    t.checkExpect(new SortByWeight().compare(e2, e1), 1);
    t.checkExpect(new SortByWeight().compare(e1, e1), 0);
  }

  // Testing the assignNeighbors() method
  void testAssignNeighbors(Tester t) {
    initData();

    ArrayList<GamePiece> neighbor00 = new ArrayList<GamePiece>(Arrays
        .asList(this.threex3Power.board.get(0).get(1), this.threex3Power.board.get(1).get(0)));

    ArrayList<GamePiece> neighbor01 = new ArrayList<GamePiece>(
        Arrays.asList(this.threex3Power.board.get(0).get(0), this.threex3Power.board.get(0).get(2),
            this.threex3Power.board.get(1).get(1)));

    ArrayList<GamePiece> neighbor02 = new ArrayList<GamePiece>(Arrays
        .asList(this.threex3Power.board.get(0).get(1), this.threex3Power.board.get(1).get(2)));

    ArrayList<GamePiece> neighbor10 = new ArrayList<GamePiece>(
        Arrays.asList(this.threex3Power.board.get(0).get(0), this.threex3Power.board.get(2).get(0),
            this.threex3Power.board.get(1).get(1)));

    ArrayList<GamePiece> neighbor11 = new ArrayList<GamePiece>(
        Arrays.asList(this.threex3Power.board.get(1).get(0), this.threex3Power.board.get(1).get(2),
            this.threex3Power.board.get(0).get(1), this.threex3Power.board.get(2).get(1)));

    ArrayList<GamePiece> neighbor12 = new ArrayList<GamePiece>(
        Arrays.asList(this.threex3Power.board.get(0).get(2), this.threex3Power.board.get(2).get(2),
            this.threex3Power.board.get(1).get(1)));

    ArrayList<GamePiece> neighbor20 = new ArrayList<GamePiece>(Arrays
        .asList(this.threex3Power.board.get(1).get(0), this.threex3Power.board.get(2).get(1)));

    ArrayList<GamePiece> neighbor21 = new ArrayList<GamePiece>(
        Arrays.asList(this.threex3Power.board.get(2).get(0), this.threex3Power.board.get(2).get(2),
            this.threex3Power.board.get(1).get(1)));

    ArrayList<GamePiece> neighbor22 = new ArrayList<GamePiece>(Arrays
        .asList(this.threex3Power.board.get(1).get(2), this.threex3Power.board.get(2).get(1)));

    this.threex3Power.board.get(0).get(0).assignNeighbors(this.threex3Power.board);
    t.checkExpect(this.threex3Power.board.get(0).get(0).neighbors, neighbor00);
    this.threex3Power.board.get(0).get(1).assignNeighbors(this.threex3Power.board);
    t.checkExpect(this.threex3Power.board.get(0).get(1).neighbors, neighbor01);
    this.threex3Power.board.get(0).get(2).assignNeighbors(this.threex3Power.board);
    t.checkExpect(this.threex3Power.board.get(0).get(2).neighbors, neighbor02);
    this.threex3Power.board.get(1).get(0).assignNeighbors(this.threex3Power.board);
    t.checkExpect(this.threex3Power.board.get(1).get(0).neighbors, neighbor10);
    this.threex3Power.board.get(1).get(1).assignNeighbors(this.threex3Power.board);
    t.checkExpect(this.threex3Power.board.get(1).get(1).neighbors, neighbor11);
    this.threex3Power.board.get(1).get(2).assignNeighbors(this.threex3Power.board);
    t.checkExpect(this.threex3Power.board.get(1).get(2).neighbors, neighbor12);
    this.threex3Power.board.get(2).get(0).assignNeighbors(this.threex3Power.board);
    t.checkExpect(this.threex3Power.board.get(2).get(0).neighbors, neighbor20);
    this.threex3Power.board.get(2).get(1).assignNeighbors(this.threex3Power.board);
    t.checkExpect(this.threex3Power.board.get(2).get(1).neighbors, neighbor21);
    this.threex3Power.board.get(2).get(2).assignNeighbors(this.threex3Power.board);
    t.checkExpect(this.threex3Power.board.get(2).get(2).neighbors, neighbor22);
  }

  // Testing the toString() method
  void testToString(Tester t) {
    initData();

    t.checkExpect(this.kruskalsBoard.board.get(0).get(0).toString(), "(1, 1)");
  }

  // Testing the connectNodes() method
  void testConnectNodes(Tester t) {
    initData();
    Edge e = new Edge(this.kruskalsBoard.board.get(0).get(0),
        this.kruskalsBoard.board.get(0).get(1));
    e.connectNodes();
    t.checkExpect(this.kruskalsBoard.board.get(0).get(0).right, true);
    t.checkExpect(this.kruskalsBoard.board.get(0).get(1).left, true);
  }
}
