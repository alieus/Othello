/*
 * 3120005 - Aliprantis Efstathios
 * 3120144 - Pappas Dimitrios-Spiridon
 * 3120178 - Sinacheri Anna-Chloe
 */
package othelo.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import othelo.Agent;
import othelo.GameManager;
import static othelo.GameManager.*;

/**
 * An agent that determines the next react by using Artificial Intelligence algorithms.
 * Please read {@link GameManager} before reading this class.
 */
public class AiAgent implements Agent {
    
    private static final class Configuration {
        final int depth; // the maximum depth of the minimax
        final int holdStatesDepth; // the depth at wich the states are not discarted show
                                   // they need not be recalulated at the next turn
        final int minCutOffDepth; // the minimum depth of the minimax
        final int badCutOffutility; // marginal cumulative cut off utility for bad states. worst states are cut off
        final int goodCutOffUtility; // marginal cumulative cut off utility for good states. better states are cut off
        final int noCutOffRound; // the maximun round that cut offs are allowed. no cut offs allowed after this round
        
        static final Map<Integer, Configuration> difficultyLevels = new HashMap<>();
        static {
            difficultyLevels.put(1, new Configuration(1, 0, -1, -1, -1, 0));
            difficultyLevels.put(2, new Configuration(2, 2, -1, -1, -1, 0));
            difficultyLevels.put(3, new Configuration(3, 3, 2, 30, 60, 64-10));
            difficultyLevels.put(4, new Configuration(4, 3, 2, 35, 70, 64-10));
            difficultyLevels.put(5, new Configuration(5, 3, 3, 40, 80, 64-10));
            difficultyLevels.put(6, new Configuration(6, 3, 3, 40, 80, 64-11));
            difficultyLevels.put(7, new Configuration(7, 4, 4, 40, 80, 64-12));
            difficultyLevels.put(8, new Configuration(8, 4, 4, 50, 100, 64-13));
            difficultyLevels.put(9, new Configuration(9, 4, 4, 50, 100, 64-14));
            difficultyLevels.put(10, new Configuration(10, 4, 4, 60, 120, 64-15));
        }
        
        static Configuration difficultyLevel(int level) {
            return difficultyLevels.get(level);
        }
        
        public Configuration(int depth, int holdStatesDepth, int minCutOffDepth, int badCutOffutility, int goodCutOffUtility, int noCutOffRound) {
            this.depth = depth;
            this.holdStatesDepth = holdStatesDepth;
            this.minCutOffDepth = minCutOffDepth;
            this.badCutOffutility = badCutOffutility;
            this.goodCutOffUtility = goodCutOffUtility;
            this.noCutOffRound = noCutOffRound;
        }
        
        
    }
    
    /**
     * Worker for paraller minimax execution
     */
    private final class MinimaxWorker implements Callable<Integer> {
        State root;
        boolean player = !AiAgent.this.player;
        int depth = 1;
        int a = Integer.MIN_VALUE;
        int b = Integer.MAX_VALUE;
        byte[] buffer = new byte[MAX_FLIP_COUNT];
        
        public MinimaxWorker(State root) {
            this.root = root;
        }

        @SuppressWarnings("unused")
        public MinimaxWorker(State root, boolean player) {
            this.root = root;
            this.player = player;
        }
        
        @SuppressWarnings("unused")
        public MinimaxWorker(State root, boolean player, int depth, int a, int b) {
            this.root = root;
            this.player = player;
            this.depth = depth;
            this.a = a;
            this.b = b;
        }
        
        @SuppressWarnings("unused")
        public void resetAB() {
            a = Integer.MIN_VALUE;
            b = Integer.MAX_VALUE;
        }
        
        @Override
        public Integer call() throws Exception {
            final int minimax = minimax(root, depth, player, a, b, 0, buffer);
            return minimax;
        }
        
    }
    
    static int stateCount = 0;
    private GameManager gMan;
    private boolean player;
    private State currentState;
    private int currentUtility;
    private boolean destroyed = false;
    
    private Configuration conf;
    
    public AiAgent(GameManager gMan, long player, int difficultyLevel) {
        if (difficultyLevel < 1 || difficultyLevel > 10)
            throw new IllegalArgumentException("Difficulty level should be in 1..10. Found: "+difficultyLevel);
        conf = Configuration.difficultyLevel(difficultyLevel);
        this.gMan = gMan;
        this.player = player == WHITE ? WHITE_BOOL : BLACK_BOOL;
        gMan.getTerrain().addPropertyListener(e -> {
            if (e.oldValues.containsKey(CURRENT_PLAYER) && gMan.currentPlayer() == NO_ONE) {
                if (currentState != null && !currentState.isMarkedDestroyed()) {
                    currentState.destroy();
                    currentState = null;
                }
            }
        });
    }
    
    public void setDifficultyLevel(int difficultyLevel) {
        if (difficultyLevel < 1 || difficultyLevel > 10)
            throw new IllegalArgumentException("Difficulty level should be in 1..10. Found: "+difficultyLevel);
        conf = Configuration.difficultyLevel(difficultyLevel);
    }
    

    
    @Override
    public void informCanPlay(MoveConsumer mc) {
        if (destroyed) return;
        try {
            final Integer nextMove = getNextMove();
            if (destroyed) return;
            if (!mc.acceptMove(nextMove)) {
                System.err.println("AiAgent proposed move "+nextMove+" which is invalid...");
                mc.acceptMove(gMan.getAMove(getPlayer()));
            }
        } catch (Throwable e) {
            e.printStackTrace();
            mc.acceptMove(gMan.getAMove(getPlayer()));
        }
    }
    
    @Override
    public void destroy() {
        destroyed = true;
//        if (currentState != null && !currentState.isMarkedDestroyed()) {
//            currentState.destroy();
//        }
    }
    
    private long getPlayer() {
        return player ? WHITE : BLACK;
    }
    
    private State getCurrentState() {
        int round = gMan.fillCount();
        State realState = State.newFromPool();
        realState.setData(gMan.terrainToArray(), round, 0);
        if (currentState == null) {
            currentState = realState;
        } else if (!currentState.equals(realState)) {
            final State search = findRecursice(currentState, realState, true);
            
            currentState.destroy();
            if (search != null) {
                assert !search.isMarkedDestroyed();
                currentState = search;
            } else {
                System.err.println("Current State Not Found");
                currentState = realState;
            }
        }
        return currentState;
    }
    
    private State findRecursice(State root, State toFind, boolean removeFromParent) {
        int round = toFind.round;
        if (root == null || root.child == null || root.child.round > round) return null;
        if (root.child.round  == round) {
            State child = root.child;
            State previousChild = null;
            while (child != null) {
                if (child.equals(toFind)) {
                    if (removeFromParent) {
                        // remove the state
                        if (previousChild != null) {
                            previousChild.nextSibling = child.nextSibling;
                        } else {
                            root.child = child.nextSibling;
                        }
                        child.nextSibling = null;
                    }
                    return child;
                } 
                previousChild = child;
                child = child.nextSibling;
            }
            return null;
        } else {
            State child = root.child;
            while (child != null) {
                final State search = findRecursice(child, toFind, removeFromParent);
                if (search != null) return search;
                child = child.nextSibling;
            }
        }
        return null;
    }
    
    public Integer getNextMove() {
        if (gMan.currentPlayer() != getPlayer()) {
            return null;
        }
       
        currentState = getCurrentState();
        currentState.clearUtility();
        currentUtility = currentState.calcUtility();
//        minimax(currentState, 5, player, Integer.MIN_VALUE, Integer.MAX_VALUE);
        parallelMinimaxOnRoot(currentState);
        State child = currentState.child;
        State previousChild = null;
        while (child != null) {
            if (child.calcUtility() == currentState.calcUtility()) {
                // remove the state
                if (previousChild != null) {
                    previousChild.nextSibling = child.nextSibling;
                } else {
                    currentState.child = child.nextSibling;
                }
                child.nextSibling = null;
                currentState.destroy();
                assert !child.isMarkedDestroyed();
                currentState = child;
                System.gc();
                return child.move;
            }
            previousChild = child;
            child = child.nextSibling;
        }
        System.gc();
        return gMan.getAMove(getPlayer());
    }
    
    /**
     * Calls minimax for each of the root's children of a diffrent thread and returns
     * the best of their results.
     * 
     * @param state
     * @return
     */
    private int parallelMinimaxOnRoot(State state) {
        if (state.isTerminal() || destroyed) {
            return state.calcUtility();
        }
        int result;
        List<MinimaxWorker> minimaxWorkers = new ArrayList<>();
        State child = state.getChildren(player, new byte[MAX_FLIP_COUNT]);
        while (child != null) {
            minimaxWorkers.add(new MinimaxWorker(child));
            child = child.nextSibling;
        }
        List<Integer> minimaxResults = new ArrayList<>(minimaxWorkers.size());
        try {
            final List<Future<Integer>> execResults = THREAD_POOL.invokeAll(minimaxWorkers);
            for (Future<Integer> execResult : execResults) {
                minimaxResults.add(execResult.get());
            }
        } catch (InterruptedException |CancellationException | ExecutionException e) {e.printStackTrace();}
        
        if (player == WHITE_BOOL) {
            result = minimaxResults.stream().mapToInt(i -> i).max().getAsInt();
        } else {
            result = minimaxResults.stream().mapToInt(i -> i).min().getAsInt();
        }
        state.setUtility(result);
        return result;
    }
    
    // buffer size should be at least MAX_FLIP_COUNT
    private int minimax(State state, int depth, boolean player, int a, int b, int cutOffFactor, byte[] buffer) {
        state.clearUtility();
        if (depth == conf.depth || state.isTerminal() || destroyed) {
            return state.calcUtility();
        } else if (depth >= conf.minCutOffDepth && state.round < conf.noCutOffRound) { // cut off
            cutOffFactor += (state.calcUtility()-currentUtility);
            if (this.player == WHITE_BOOL && (cutOffFactor < -conf.badCutOffutility || cutOffFactor > conf.goodCutOffUtility)
                    || this.player == BLACK_BOOL && (cutOffFactor > conf.badCutOffutility || cutOffFactor < -conf.goodCutOffUtility)) {
                return state.calcUtility();
            }
        }
        
        if (state.whiteHasMove() && (player == WHITE_BOOL || !state.blackHasMove())) {
            State child = state.getChildren(WHITE_BOOL, buffer);
            while (child != null) {
                int utility = minimax(child, depth+1, BLACK_BOOL, a, b, cutOffFactor, buffer);
                if (utility > a) {
                    a = utility ;
                    if (b <= a ) {
                        break;
                    }
                }
                child = child.nextSibling;
            }
            state.setUtility(a);
        } else {
            State child = state.getChildren(BLACK_BOOL, buffer);
            while (child != null) {
                int utility = minimax(child, depth+1, WHITE_BOOL, a, b, cutOffFactor, buffer);
                if (utility < b) {
                    b = utility ;
                    if (b <= a ) {
                        break ;
                    }
                }
                child = child.nextSibling;
            }
            state.setUtility(b);
        }
        if (depth > conf.holdStatesDepth && 64 - (conf.depth-depth) > state.round) {
            state.destroySubtree(); // destroy the produced states
        }
        return state.calcUtility() ;
    }
    
}