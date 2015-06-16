/*
 * 3120005 - Aliprantis Efstathios
 * 3120144 - Pappas Dimitrios-Spiridon
 * 3120178 - Sinacheri Anna-Chloe
 */
package othelo;

import othelo.ui.Graphics;
import gr.entij.EntitySet;
import gr.entij.Terrain;
import gr.entij.Entity;
import static gr.entij.EFilter.*;
import gr.entij.Reaction;
import othelo.ai.AiAgent;
import gr.entij.graphics2d.GTerrain;
import gr.entij.graphics2d.Util;

import java.util.*;

import static java.util.Arrays.asList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class GameManager {
    public static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();
    
    public static final long WHITE = 1;
    public static final long BLACK = -1;
    public static final long NO_ONE = 0;
    public static final String PIECE = "piece";
    public static final String SQUARE = "square";
    
    public static final String CURRENT_PLAYER = "current player";
    
    public static final Long REINITIALIZE = 0L;
    public static final Long START = 1L;
    public static final Long STOP = 2L;
    
    /**
     * The 8 directions. (up-left, up, up-right, left, ...)
     */
    public static final List<Integer> DIRECTIONS = Collections.unmodifiableList(
            asList(-9, -8, -7, -1, 1, 7, 8, 9));
    
    public static final int TERRAIN_SIZE = 64;
    public static final int MAX_FLIP_COUNT = 18;
    public static final int DEFAULT_AI_LEVEL = 4;
    
    public static final boolean WHITE_BOOL = true;
    public static final boolean BLACK_BOOL = false;

    public static long otherPalyer(long player) {
        if (player == BLACK) return WHITE;
        else if (player == WHITE) return BLACK;
        else throw new IllegalArgumentException("player must be one of BLACK, WHITE");
    }

    
    private final Terrain t;
    private GTerrain gra;
    
    private Agent whiteAgent;
    private Agent blackAgent;
    private boolean stopped = false;
    
    private int moveDelay = 650;
    private long lastMoveTime = -1;
    
    public GameManager() {
        t = setUpTerrain();
        initGraphic(true);
        setBlackAgent(null);
        setWhiteAgent(null);
    }

    private Terrain setUpTerrain() {
        Terrain t = new Terrain();
        for (int i = 0; i < TERRAIN_SIZE; i++) {
            t.add(new Entity(SQUARE, i, 0));
        }
        t.add(new Entity(PIECE, 27, WHITE));
        t.add(new Entity(PIECE, 28, BLACK));
        t.add(new Entity(PIECE, 35, BLACK));
        t.add(new Entity(PIECE, 36, WHITE));
        
        t.set(String.valueOf(BLACK), 2);
        t.set(String.valueOf(WHITE), 2);
        t.set(CURRENT_PLAYER, WHITE);
        
        t.addLogic((e, m) -> {
            if (m.equals(REINITIALIZE)) {
                reinitialize();
                return new Reaction();
            } else if (m.equals(START)) {
                startAgents();
                return new Reaction();
            } else if (m.equals(STOP)) {
                stopped = true;
                return new Reaction();
            }
            return null;
        });
        
        return t;
    }
    
    public void reinitialize() {
        (new EntitySet(t.named(PIECE))).forEach(e -> e.destroy());
        t.add(new Entity(PIECE, 27, WHITE));
        t.add(new Entity(PIECE, 28, BLACK));
        t.add(new Entity(PIECE, 35, BLACK));
        t.add(new Entity(PIECE, 36, WHITE));
        
        t.set(String.valueOf(BLACK), 2);
        t.set(String.valueOf(WHITE), 2);
        t.set(CURRENT_PLAYER, WHITE);
    }
    
    private void initGraphic(boolean showWindow) {
        if (showWindow) {
            gra = Graphics.setUpGraphicAndShowWindow(this, 630, 690);
        } else {
            gra = Graphics.setUpGraphic(this);
        }
    }
    
    public Terrain getTerrain() {
        return t;
    }

    public GTerrain getGraphic() {
        return gra;
    }

    public Agent getWhiteAgent() {
        return whiteAgent;
    }

    public Agent getBlackAgent() {
        return blackAgent;
    }

    public final void setWhiteAgent(Agent whiteAgent) {
        if (whiteAgent == this.whiteAgent) return;
        if (this.whiteAgent != null) {
            this.whiteAgent.destroy();
        }
        this.whiteAgent = whiteAgent != null ? whiteAgent : new AiAgent(this, WHITE, DEFAULT_AI_LEVEL);
    }

    public final void setBlackAgent(Agent blackAgent) {
        if (blackAgent == this.blackAgent) return;
        if (this.blackAgent != null) {
            this.blackAgent.destroy();
        }
        this.blackAgent = blackAgent != null ? blackAgent : new AiAgent(this, BLACK, DEFAULT_AI_LEVEL);
    }

    public boolean startAgents() {
        long nowCurrPlayer = currentPlayer();
        if (nowCurrPlayer == NO_ONE) return false;
        
        Agent toPlay = nowCurrPlayer == WHITE ? whiteAgent : blackAgent;
        
        Agent.MoveConsumer mc = new Agent.MoveConsumer() {
            boolean consumed;
            final int round = fillCount();
            
            @Override public boolean acceptMove(int move) {
                if (expired()) throw new IllegalStateException("Move consumer has expired");
                if (lastMoveTime >= 0) {
                    long diffrence = System.currentTimeMillis() - lastMoveTime;
                    if (diffrence < moveDelay) {
                        Util.sleep(moveDelay - diffrence);
                    }
                }
                consumed = play(move) != null;
                if (consumed) {
                    lastMoveTime = System.currentTimeMillis();
                }
                return consumed;
            }
            
            @Override public boolean expired() {
                return consumed || nowCurrPlayer != currentPlayer() || round != fillCount()
                        || nowCurrPlayer == WHITE && toPlay != whiteAgent
                        || nowCurrPlayer == BLACK && toPlay != blackAgent;
            }
        };
        
        stopped = false;
        THREAD_POOL.execute(() -> toPlay.informCanPlay(mc));
//        new Thread(() -> toPlay.informCanPlay(mc)).start();
        return true;
    }
    
    public long currentPlayer() {
        return t.get(CURRENT_PLAYER);
    }
    
    /**
     * Returns the number of quares that are filled with pieces.
     * @return the number of quares that are filled with pieces
     */
    public int fillCount() {
        return t.named(PIECE).size();
    }
    
    /**
     * Attempts to make a react by putting a piece on the board as {@code player}.
     * If the react is valid, all side effects of the react are performed.
 Otherwise, nothing happens.
 
 Side effects of a react consist of putting the new piece on the board
 and flipping the opponent's pieces where necessary.
     * 
     * @param t the Terrain (chessboard)
     * @param pos the position to add the new piece; must be >= 0 and <= 63
     * @param player the player who makes the react; one of {@code BLACK, WHITE}
     * @return the newly added piece or {@code null} if the react was invalid
     * @throws IllegalArgumentException if {@code player} is not one of {@code BLACK, WHITE}
     * or {@code pos} is not in range 0..63
     */
    private Entity play(long pos) throws IllegalArgumentException {
        if (pos < 0 || pos >= TERRAIN_SIZE)
            throw new IllegalArgumentException("pos must be >= 0 and <= 63; found "+pos);
        if (t.at(pos).hasAny(named(PIECE))) {
            return null;
        }
        
        long player = t.get(CURRENT_PLAYER);
        
        byte[] toFlip = new byte[MAX_FLIP_COUNT];
        final int toFlipCount = calcToFlip(terrainToArray(), player == WHITE, (int) pos, toFlip);
        
        if (toFlipCount > 0) {
            Entity result = new Entity(PIECE, pos, player);
            t.add(result);
            
            for (int i = 0; i < toFlipCount; i++) {
                t.at(toFlip[i]).any(named(PIECE)).get().setState(player);
            }
            
            Map<String, Object> state = new HashMap<>(5, 0.9F);
            state.put(BLACK+"", t.named(PIECE).count(inState(BLACK)));
            state.put(WHITE+"", t.named(PIECE).count(inState(WHITE)));
            long nextPlayer = otherPalyer(player);
            if (!playerHasMove(nextPlayer)) {
                nextPlayer = playerHasMove(player) ? player : NO_ONE;
            }
            state.put(CURRENT_PLAYER, nextPlayer);
            t.putAll(state);
            
            if (currentPlayer() != NO_ONE && !stopped) {
                startAgents();
            }
            
            return result;
        }
        
        return null;
    }
    
    /**
     * Calculates the pieces that must be flipped as effect of the given react.
     * 
     * @param t a representation of the terrain's state (see {@link #terrainToArray(boolean[]))
     * @param player the player who performs the react 
     * @param pos the position of the new piece
     * @param result an array that will contain the indices of the pieces to be flipped;
     * the size of the array must be ata least {@code MAX_FLIP_COUNT}
     * @return the number of pieces that must be flipped
     */
    public static int calcToFlip(boolean[] t, boolean player, int pos, byte[] result) {
        int  count = 0;
//        for (int dire : DIRECTIONS) {
        for (int di = 0; di < DIRECTIONS.size(); di++) {
            int dire = DIRECTIONS.get(di);
            // do not exceed the side edges
            if (xDistance(pos, pos+dire) > 1 || !inBounds(pos+dire)) {
                continue;
            }
            
            int currPos = pos+dire;
            while (t[currPos]) {
                boolean piece = t[TERRAIN_SIZE + currPos];
                if (piece == player) { // found a player's piece
                    // all intermediate pieces must be flipped  
                    for (int i = pos+dire; i != currPos; i += dire) {
                        result[count] = (byte) i;
                        count++;
                    }
                    break;
                }
                
                // do not exceed the side edges
                if (xDistance(currPos, currPos+dire) > 1 || !inBounds(currPos+dire)) {
                    break;
                }
                currPos += dire;
            }
        }
        return count;
    }
    
    /**
     * Returns an arbitrary valid react for the given player at the current state.
     * 
     * @param player one of WHITE, BLACK
     * @return a react for {@code player} or -1 if there is no valid react
     */
    public int getAMove(long player) {
        return getAMove(terrainToArray(), player == WHITE);
    }
    
    /**
     * Determines whether the given player has a valid react at the current state.
     * @param player  one of WHITE, BLACK
     * @return whether the given player has a valid react
     */
    public boolean playerHasMove(long player) {
        return getAMove(player) >= 0;
    }
    
    /**
     * Returns an arbitrary valid react for the given player at the given state.
     * 
     * @param t a representation of the terrain's state (see {@link #terrainToArray(boolean[]))
     * @param player one of WHITE_BOOL, BLACK_BOOL
     * @return a react for {@code player} or -1 if there is no valid react
     */
    public static int getAMove(boolean[] t, boolean player) {
        for (int i = 0; i < TERRAIN_SIZE; i++) {
            if (t[i]) continue;
            
            for (int di = 0; di < DIRECTIONS.size(); di++) {
                int dire = DIRECTIONS.get(di);
                boolean foundOppon = false;
                // do not exceed the side edges
                if (xDistance(i, i+dire) > 1 || !inBounds(i+dire)) {
                    continue;
                }
                
                int currPos = i+dire;
                
                while (t[currPos]) {
                    boolean piece = t[TERRAIN_SIZE + currPos];
                    if (piece == player) {
                        if (foundOppon) {
                            return i;
                        } else {
                            break;
                        }
                    } else {  // piece.getState() == other player
                        foundOppon = true;
                    }
                    
                    if (xDistance(currPos, currPos+dire) > 1 || !inBounds(currPos+dire)) {
                        break;
                    }
                    currPos += dire;
                }
            }
        }
        return -1;
    }
    
    /**
     * Fills the given array with a representation of the current state of the
     * terrain. 
     * The the first 64 cells are filled true iff there exists a piece at that position.
     * The second 64 cells filled with true if the piece at that position is WHITE.
     * The content of one pf the second 64 cell is unspesified if there is no piece
     * at all at that position.
     * 
     * @param array an array of size ata least 2*64=128
     */
    public void terrainToArray(boolean[] array) {
        Arrays.fill(array, 0, TERRAIN_SIZE, false);
        t.named(PIECE).stream().forEach((piece) -> {
            int posit = (int) piece.getPosit();
            array[posit] = true;
            array[TERRAIN_SIZE+posit] = piece.getState() == WHITE;
        });
    }
    
    /**
     * Returns a new array with a representation of the current state of the
     * terrain. See {@link #terrainToArray(boolean[])}.
     * 
     * @return an array representing the current state
     */
    public boolean[] terrainToArray() {
        boolean[] result = new boolean[TERRAIN_SIZE*2];
        terrainToArray(result);
        return result;
    }
    
    /**
     * Calculates the distance of two positions in the x-axis
     * The result is between 0 and 7
     * 
     * @param pos1
     * @param pos2
     * @return the distance of two positions in the x-axis
     */
    public static int xDistance(long pos1, long pos2) {
        return (int) Math.abs(pos1 % 8 - pos2 % 8);
    }
    
    /**
     * Determines whether the given position is inside the bounds of the terrain.
     * 
     * @param pos 
     * @return whether the given position is inside the bounds of the terrain
     */
    public static boolean inBounds(long pos) {
        return pos >= 0 && pos < TERRAIN_SIZE;
    }
}
