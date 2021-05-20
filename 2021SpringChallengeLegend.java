import java.util.*;
import java.util.stream.Collectors;

class Cell {
    private int index;
    private int richness;
    private int[] neighbours;
    private boolean shadowedNextRound;
    private boolean hasNeighborTree;
    private int countTreesInLine; //possible shadows
    private int completeHeuristics;
    private int growHeuristics;
    private int seedHeuristics;
    private List<Integer> edgeCells = new ArrayList<>(Arrays.asList(19, 22, 25, 28, 31, 34));

    public int getIndex() {
        return index;
    }

    public int getRichness() {
        return richness;
    }

    public int getNeighbour(int nr) {
        return neighbours[nr];
    }

    public int[] getNeighbours() {
        return neighbours;
    }

    public boolean isShadowedNextRound() {
        return shadowedNextRound;
    }

    public void setShadowedNextRound(boolean shadowedNextRound) {
        this.shadowedNextRound = shadowedNextRound;
    }

    public void setCellShadowNextRound(Game game, int left, int fromTreeSize) {
        int neighborToShadow = this.getNeighbour((game.getSunDirectionTo() + 1) % 6);
        //from a certain tree 3 cell is shadowed, so we measure with left values 3,2,1
        if (neighborToShadow != -1 && left >= 1) {
            Cell neighborToShadowCell = game.getBoardCell(neighborToShadow);
            //if there is a bigger tree on the cell than fromTreeSize, than no shadow
            if (game.getTree(neighborToShadowCell.getIndex()) != null &&
                    game.getTree(neighborToShadowCell.getIndex()).getSize() <= fromTreeSize)
                neighborToShadowCell.setShadowedNextRound(true);
            //go on with the neighbor cell in that direction
            neighborToShadowCell.setCellShadowNextRound(game, --left, fromTreeSize);
        }
    }

    public void setCellHasTreeInLine(Game game, int left, int sunDirection) {
        int neighborCellIndexToCheck = this.getNeighbour(sunDirection);
        //from a certain tree 3 cell is shadowed, so we measure with left values 3,2,1
        if (neighborCellIndexToCheck != -1 && left >= 1) {
            Cell neighborCellToCheck = game.getBoardCell(neighborCellIndexToCheck);
            //cell possible will have shadow
            neighborCellToCheck.setCountTreesInLine(neighborCellToCheck.getCountTreesInLine() + 1);
            //go on on that direction max 3 step
            neighborCellToCheck.setCellHasTreeInLine(game, --left, sunDirection);
        }
    }

    public int getCompleteHeuristics() {
        return completeHeuristics;
    }

    public int getGrowHeuristics() {
        return growHeuristics;
    }

    public int getSeedHeuristics() {
        return seedHeuristics;
    }

    public void setCompleteHeuristics() {
        //which tree is shadowed next round, best candidate to cut off
        completeHeuristics = isShadowedNextRound() ? richness + 1 : richness;
        //edgecells have less shadow usually, so complete with lowest priority
        if (edgeCells.contains(this.index)) completeHeuristics = 0;
    }

    public void setGrowHeuristics() {
        //which tree is shadowed next round won't produce sun points, so better not to grow it
        growHeuristics = isShadowedNextRound() ? richness - 1 : richness;
    }

    public void setSeedHeuristics() {
        //important to seed in rich ground but if it's shadowed, then not a great deal
        seedHeuristics = richness - 2 * countTreesInLine;
    }


    public Cell(int index, int richness, int[] neighbours) {
        this.index = index;
        this.richness = richness;
        this.neighbours = neighbours;
        this.completeHeuristics = richness;
        this.growHeuristics = richness;
        this.seedHeuristics = richness;
    }

    @Override
    public String toString() {
        return "Cell: index: " + index + " richness: " + richness + " complete heuristics: " +
                completeHeuristics + " growHeuristics: " + growHeuristics;
    }

    public boolean isHasNeighborTree() {
        return hasNeighborTree;
    }

    public void setHasNeighborTree(boolean hasNeighborTree) {
        this.hasNeighborTree = hasNeighborTree;
    }

    public int getCountTreesInLine() {
        return countTreesInLine;
    }

    public void setCountTreesInLine(int countTreesInLine) {
        this.countTreesInLine = countTreesInLine;
    }

}

class Tree {
    private int cellIndex;
    private int size;
    private boolean isMine;

    public int getCellIndex() {
        return cellIndex;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isMine() {
        return isMine;
    }

    public Tree(int cellIndex, int size, boolean isMine) {
        this.cellIndex = cellIndex;
        this.size = size;
        this.isMine = isMine;
    }
}

class Action {
    private String name;
    private int cellIndexFrom;
    private int cellIndexTo;

    public String getName() {
        return name;
    }

    public int getCellIndexFrom() {
        return cellIndexFrom;
    }

    public int getCellIndexTo() {
        return cellIndexTo;
    }

    static Action parse(String action) {
        String[] command = action.split(" ");
        switch (command[0]) {
            case "WAIT":
                return new Action("WAIT");
            case "COMPLETE":
                return new Action("COMPLETE", Integer.parseInt(command[1]));
            case "GROW":
                return new Action("GROW", Integer.parseInt(command[1]));
            case "SEED":
                return new Action("SEED", Integer.parseInt(command[1]), Integer.parseInt(command[2]));
            default:
                System.err.println("DEBUG: Unknown command parsed!");
                return null;
        }
    }

    public Action(String name) {
        this.name = name;
    }

    public Action(String name, int cellIndexFrom) {
        this.name = name;
        this.cellIndexFrom = cellIndexFrom;
    }

    public Action(String name, int cellIndexFrom, int cellIndexTo) {
        this.name = name;
        this.cellIndexFrom = cellIndexFrom;
        this.cellIndexTo = cellIndexTo;
    }

    @Override
    public String toString() {
        switch (name) {
            case "WAIT":
                return "WAIT";
            case "COMPLETE":
                return "COMPLETE " + cellIndexFrom;
            case "GROW":
                return "GROW " + cellIndexFrom;
            case "SEED":
                return "SEED " + cellIndexFrom + " " + cellIndexTo;
            default:
                return "Invalid command at cell.toString";
        }
    }
}

class Game {
    private List<Cell> board = new ArrayList<>();
    private List<Action> possibleActions = new ArrayList<>();
    private List<Tree> trees = new ArrayList<>();
    private int day;
    private int nutrient;
    private int mySunPoints, oppSunPoints;
    private int myScore, oppScore;
    private boolean oppIsWaiting;
    private int sunDirectionTo;
    private int dailySpGenerationNextRound;

    /*
     2 1
    3   0
     4 5
     */

    public void addBoardCell(Cell cell) {
        board.add(cell);
    }

    public List<Cell> getBoardCells() {
        return new ArrayList<>(board);
    }

    public void clearBoard() {
        board.clear();
    }

    public void addPossibleAction(Action action) {
        possibleActions.add(action);
    }

    public List<Action> getPossibleActions() {
        return new ArrayList<>(possibleActions);
    }

    public void clearPossibleActions() {
        possibleActions.clear();
    }

    public void addTree(Tree tree) {
        trees.add(tree);
    }

    public List<Tree> getTrees() {
        return new ArrayList<>(trees);
    }

    public void clearTrees() {
        trees.clear();
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getNutrient() {
        return nutrient;
    }

    public void setNutrient(int nutrient) {
        this.nutrient = nutrient;
    }

    public int getMySunPoints() {
        return mySunPoints;
    }

    public void setMySunPoints(int mySunPoints) {
        this.mySunPoints = mySunPoints;
    }

    public int getOppSunPoints() {
        return oppSunPoints;
    }

    public void setOppSunPoints(int oppSunPoints) {
        this.oppSunPoints = oppSunPoints;
    }

    public int getMyScore() {
        return myScore;
    }

    public void setMyScore(int myScore) {
        this.myScore = myScore;
    }

    public int getOppScore() {
        return oppScore;
    }

    public void setOppScore(int oppScore) {
        this.oppScore = oppScore;
    }

    public boolean isOppIsWaiting() {
        return oppIsWaiting;
    }

    public void setOppIsWaiting(boolean oppIsWaiting) {
        this.oppIsWaiting = oppIsWaiting;
    }

    public void setSunDirectionTo(int sunDirectionTo) {
        this.sunDirectionTo = sunDirectionTo;
    }

    public int getSunDirectionTo() {
        return sunDirectionTo;
    }

    public Cell getBoardCell(int cellIndex) {
        for (Cell cell : board) {
            if (cell.getIndex() == cellIndex) return cell;
        }
        return null;
    }

    public Tree getTree(int cellIndex) {
        for (Tree tree : trees) {
            if (tree.getCellIndex() == cellIndex) return tree;
        }
        return null;
    }

    public int getDailySpGeneration() {
        return dailySpGenerationNextRound;
    }

    /*
    set shadow for the next round
    for example sun to 0, then next round sun to 1
    so neighbors toward direction 1 should be shadowed
    seed does not have shadow
     */
    public void setShadows() {
        //set shadows from 1trees
        for (Cell cell : board) {
            if (this.getTree(cell.getIndex()) != null && this.getTree(cell.getIndex()).getSize() == 1) {
                //set the shadowed cells
                cell.setCellShadowNextRound(this, 1, 1);
            }
        }

        //set shadows from 2trees
        for (Cell cell : board) {
            if (this.getTree(cell.getIndex()) != null && this.getTree(cell.getIndex()).getSize() == 2) {
                //set the shadowed cells
                cell.setCellShadowNextRound(this, 2, 2);
            }
        }

        //set shadows from 3trees
        for (Cell cell : board) {
            if (this.getTree(cell.getIndex()) != null && this.getTree(cell.getIndex()).getSize() == 3) {
                //set the shadowed cells
                cell.setCellShadowNextRound(this, 3, 3);
            }
        }

        updateHeuristics();
    }

    public void clearShadows() {
        for (Cell cell : board) {
            cell.setShadowedNextRound(false);
        }
    }

    //set neighbor trees among my trees as cell property, it comes good at seeding
    public void setNeighborTrees() {
        //first delete if a cell had neighbor trees
        for (Cell cell : board) {
            cell.setHasNeighborTree(false);
        }

        //update the cells
        for (Cell cell : board) {
            for (Tree tree : trees) {
                if (Arrays.stream(cell.getNeighbours()).anyMatch(i -> i == tree.getCellIndex() && tree.isMine() && tree.getSize() >= 0)) {
                    cell.setHasNeighborTree(true);
                }
            }
        }
    }

    /*
    set cells where trees in line causing possible shadow
    this will be used at seeding heuristics
    */
    public void setTreesPossibleShadowedCells() {
        //first delete if a cell had neighbor trees
        for (Cell cell : board) {
            cell.setCountTreesInLine(0);
        }

        //update the cells
        for (Cell cell : board) {
            if (this.getTree(cell.getIndex()) != null) {
                //check every possible sun direction
                for (int sunDirection = 0; sunDirection < 6; sunDirection++) {
                    cell.setCellHasTreeInLine(this, 3, sunDirection);
                }
            }
        }
    }

    public void updateHeuristics() {
        //this 2 is good to seed heuristics
        setNeighborTrees();
        setTreesPossibleShadowedCells();

        //update the heuristics for every cell
        for (Cell cell : board) {
            cell.setCompleteHeuristics();
            cell.setGrowHeuristics();
            cell.setSeedHeuristics();
        }
    }

    //4 action possible COMPLETE, GROW, SEED, WAIT
    Action getNextAction() {
        //update all 3 heuristics
        updateHeuristics();

        //count my trees
        int count3TreesAll = (int) trees.stream().filter(tree -> tree.getSize() == 3 && tree.isMine()).count();
        int count2TreesAll = (int) trees.stream().filter(tree -> tree.getSize() == 2 && tree.isMine()).count();
        int count1TreesAll = (int) trees.stream().filter(tree -> tree.getSize() == 1 && tree.isMine()).count();

        int growCost3Tree = 7 + count3TreesAll;
        int growCost2Tree = 3 + count2TreesAll;
        int growCost1Tree = 1 + count1TreesAll;

        System.err.println("=== Costs ===");
        System.err.println("3Tree cost: " + growCost3Tree);
        System.err.println("2Tree cost: " + growCost2Tree);
        System.err.println("1Tree cost: " + growCost1Tree);
        System.err.println("Complete cost: " + 4);
        System.err.println("Seed cost: " + 0);
        System.err.println("Nutrition value we got: " + this.getNutrient());

        //not shadowed for SP calculation
        int count3TreesNotShadowedNextRound = 0, count2TreesNotShadowedNextRound = 0, count1TreesNotShadowedNextRound = 0;
        for (Cell cell : board) {
            if (this.getTree(cell.getIndex()) != null && this.getTree(cell.getIndex()).isMine() && !cell.isShadowedNextRound()) {
                switch (this.getTree(cell.getIndex()).getSize()) {
                    case 3:
                        count3TreesNotShadowedNextRound++;
                        break;
                    case 2:
                        count2TreesNotShadowedNextRound++;
                        break;
                    case 1:
                        count1TreesNotShadowedNextRound++;
                        break;
                }
            }
        }

        int countSeeds = (int) trees.stream().filter(tree -> tree.getSize() == 0 && tree.isMine()).count();

        dailySpGenerationNextRound = count3TreesNotShadowedNextRound * 3 + count2TreesNotShadowedNextRound * 2 + count1TreesNotShadowedNextRound * 1;

        System.err.println("!!! All 3tree count: " + count3TreesAll);
        System.err.println("!!! Not shadowed 3tree count: " + count3TreesNotShadowedNextRound);
        System.err.println("!!! Not shadowed 2tree count: " + count2TreesNotShadowedNextRound);
        System.err.println("!!! Not shadowed 1tree count: " + count1TreesNotShadowedNextRound);
        System.err.println("!!! seed count: " + countSeeds);

        Comparator<Cell> compareByCompleteHeuristics = (c1, c2) -> c2.getCompleteHeuristics() - c1.getCompleteHeuristics();
        Comparator<Cell> compareByGrowHeuristics = (c1, c2) -> c2.getGrowHeuristics() - c1.getGrowHeuristics();
        Comparator<Cell> compareBySeedHeuristics = (c1, c2) -> c2.getSeedHeuristics() - c1.getSeedHeuristics();

        //=========== COMPLETE ===========
        Collections.sort(board, compareByCompleteHeuristics);
        System.err.println("==== Sorted 3 trees =====");
        board.stream().filter(
                (Cell cell) -> this.getTree(cell.getIndex()) != null &&
                        this.getTree(cell.getIndex()).isMine() &&
                        this.getTree(cell.getIndex()).getSize() == 3)
                .forEach(System.err::println);

        List<Action> completeActions = possibleActions.stream().filter(action -> action.getName().equals("COMPLETE")).collect(Collectors.toList());

        //from day 18 every 3Trees should be completed
        if (day >= 18)
            for (Cell cell : board) {
                for (Action action : completeActions) {
                    //if nutritient=0 or 1 and cell richness is 1 than not effective to complete
                    // cost 4 SP and got 1 or 0 GP, although 3SP=1GP at the end
                    int gPEarned = 0;
                    if (cell.getRichness() > 1)
                        gPEarned = (int) Math.pow(2, cell.getRichness() - 1);
                    if (action.getCellIndexFrom() == cell.getIndex() && ((nutrient + gPEarned) > (((23 - day) * 3) - 4) / 3)
                            && mySunPoints >= 4) {
                        System.err.println("===== Completed 3tree ==== " + cell.getIndex());
                        return action;
                    }
                }
            }
        //from day 13 worth to complete 3Trees if they are shadowed next turn
        else if (day >= 13)
            for (Cell cell : board) {
                for (Action action : completeActions) {
                    //if nutritient=0 or 1 and cell richness is 1 than not effective to complete
                    // cost 4 SP and got 1 or 0 GP, although 3SP=1GP at the end
                    int gPEarned = 0;
                    if (cell.getRichness() > 1)
                        gPEarned = (int) Math.pow(2, cell.getRichness() - 1);
                    if (action.getCellIndexFrom() == cell.getIndex() && ((nutrient + gPEarned) > (((23 - day) * 3) - 4) / 3) &&
                            cell.isShadowedNextRound()) {
                        System.err.println("===== Completed 3tree ==== " + cell.getIndex());
                        return action;
                    }
                }
            }

        //=========== GROW ===========
        List<Action> growActions = possibleActions.stream().filter(action -> action.getName().equals("GROW")).collect(Collectors.toList());

        Collections.sort(board, compareByGrowHeuristics);
        for (Cell c : board) {
            if (this.getTree(c.getIndex()) != null && this.getTree(c.getIndex()).isMine())
                System.err.println("Cell ID: " + c.getIndex() + " GROW H: " + c.getGrowHeuristics());
        }

        System.err.println("==== Sorted 2 trees =====");
        board.stream().filter(
                (Cell cell) -> this.getTree(cell.getIndex()) != null &&
                        this.getTree(cell.getIndex()).isMine() &&
                        this.getTree(cell.getIndex()).getSize() == 2)
                .forEach(System.err::println);

        System.err.println("==== Sorted 1 trees =====");
        board.stream().filter(
                (Cell cell) -> this.getTree(cell.getIndex()) != null &&
                        this.getTree(cell.getIndex()).isMine() &&
                        this.getTree(cell.getIndex()).getSize() == 1)
                .forEach(System.err::println);

        System.err.println("==== Sorted 0 trees =====");
        board.stream().filter(
                (Cell cell) -> this.getTree(cell.getIndex()) != null &&
                        this.getTree(cell.getIndex()).isMine() &&
                        this.getTree(cell.getIndex()).getSize() == 0)
                .forEach(System.err::println);

        for (Cell cell : board) {
            for (Action action : growActions) {
                if (action.getCellIndexFrom() == cell.getIndex()) {
                    int treeSize = this.getTree(action.getCellIndexFrom()).getSize();
                    boolean go1 = false, go2 = false, go3 = false;

                    if (treeSize == 2)
                        //when it is worth to grow a certain tree
                        if ((((23 - day) * 3 - 3 > growCost3Tree) && cell.isShadowedNextRound()) ||
                                ((23 - day) * 3 > growCost3Tree))
                            go3 = true;

                    if (treeSize == 1)
                        if ((((23 - day) * 2 - 2 > growCost2Tree) && cell.isShadowedNextRound()) ||
                                ((23 - day) * 2 > growCost2Tree))
                            go2 = true;

                    if (treeSize == 0)
                        if ((((23 - day) * 1 - 1 > growCost1Tree) && cell.isShadowedNextRound()) ||
                                ((23 - day) * 1 > growCost1Tree))
                            go1 = true;

                    if (treeSize == 2 && (day < 22 || (day == 22 && cell.getRichness() > 1)) && go3) {
                        System.err.println("===== New 3tree ==== " + cell.getIndex());
                        return action;
                    } else if (treeSize == 1 && go2 && (count3TreesAll > 0 && growCost2Tree <= growCost3Tree / 3 * 2 || count3TreesAll == 0)) {
                        System.err.println("===== New 2tree ==== " + cell.getIndex());
                        return action;
                    } else if (treeSize == 0 && go1 && growCost1Tree <= growCost2Tree / 2) {
                        System.err.println("===== New 1tree ==== " + cell.getIndex());
                        return action;
                    }
                }
            }
        }

        //=========== SEED ===========
        Collections.sort(board, compareBySeedHeuristics);
        List<Action> seedActions = possibleActions.stream().filter(action -> action.getName().equals("SEED")).collect(Collectors.toList());
        for (Cell cell : board) {
            for (Action action : seedActions) {
                if (action.getCellIndexTo() == cell.getIndex() && !cell.isHasNeighborTree()) {
                    System.err.println("SeedHeuristics: id:" + cell.getIndex() + " hnt: " + cell.isHasNeighborTree() + " h:" + cell.getSeedHeuristics());
                }
            }
        }

        //only seed 1 seed at a time, no need more
        if (countSeeds == 0) {
            for (Cell cell : board) {
                for (Action action : seedActions) {
                    if (action.getCellIndexTo() == cell.getIndex() && !cell.isHasNeighborTree()) {
                        return action;
                    }
                }
            }
        }

        //=========== WAIT ===========
        //if no other option then just wait
        return possibleActions.get(0);
    }
}

class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        Game game = new Game();

        int numberOfCells = in.nextInt(); // 37
        for (int i = 0; i < numberOfCells; i++) {
            int index = in.nextInt(); // 0 is the center cell, the next cells spiral outwards
            int richness = in.nextInt(); // 0 if the cell is unusable, 1-3 for usable cells
            int neigh0 = in.nextInt(); // the index of the neighbouring cell for each direction
            int neigh1 = in.nextInt();
            int neigh2 = in.nextInt();
            int neigh3 = in.nextInt();
            int neigh4 = in.nextInt();
            int neigh5 = in.nextInt();
            int neighbors[] = new int[]{neigh0, neigh1, neigh2, neigh3, neigh4, neigh5};
            Cell cell = new Cell(index, richness, neighbors);
            game.addBoardCell(cell);
        }

        game.setSunDirectionTo(0);
        int turn = -1;
        // game loop
        while (true) {
            game.setDay(in.nextInt()); // the game lasts 24 days: 0-23
            //System.err.println("Turn: "+turn+" day: "+game.getDay());
            //new turn;
            game.setNutrient(in.nextInt()); // the base score you gain from the next COMPLETE action
            game.setMySunPoints(in.nextInt()); // your sun points
            game.setMyScore(in.nextInt()); // your current score
            game.setOppSunPoints(in.nextInt()); // opponent's sun points
            game.setOppScore(in.nextInt()); // opponent's score
            game.setOppIsWaiting(in.nextInt() != 0); // whether your opponent is asleep until the next day
            game.clearTrees();
            int numberOfTrees = in.nextInt(); // the current amount of trees
            for (int i = 0; i < numberOfTrees; i++) {
                int cellIndex = in.nextInt(); // location of this tree
                int size = in.nextInt(); // size of this tree: 0-3
                boolean isMine = in.nextInt() != 0; // 1 if this is your tree
                boolean isDormant = in.nextInt() != 0; // 1 if this tree is dormant
                Tree tree = new Tree(cellIndex, size, isMine);
                game.addTree(tree);
            }
            //set shadows because of new trees
            game.clearShadows();
            game.setShadows();

            //new turn?
            if (turn != game.getDay()) {
                turn = game.getDay();
                game.setSunDirectionTo(turn % 6);
                System.err.printf("===== %d. turn =====\n", turn);
                System.err.println("Sun To: " + game.getSunDirectionTo());
            }

            game.clearPossibleActions();
            int numberOfPossibleActions = in.nextInt(); // all legal actions
            if (in.hasNextLine()) {
                in.nextLine();
            }
            for (int i = 0; i < numberOfPossibleActions; i++) {
                String possibleAction = in.nextLine();
                game.addPossibleAction(Action.parse(possibleAction));
            }

            // GROW cellIdx | SEED sourceIdx targetIdx | COMPLETE cellIdx | WAIT <message>
            Action action = game.getNextAction();
            if (action.getName().equals("WAIT"))
                System.out.println(action + " " + "DSP: " + game.getDailySpGeneration() + " SP: " + game.getMySunPoints());
            else
                System.out.println(action);
        }
    }
}