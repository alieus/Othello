/*
 * 3120005 - Aliprantis Efstathios
 * 3120144 - Pappas Dimitrios-Spiridon
 * 3120178 - Sinacheri Anna-Chloe
 */
package othelo.ai;

import java.util.Arrays;
import othelo.GameManager;

/**
 * Please read {@link GameManager} before reading this class.
 */
final class State {
    static final StatePool pool = new StatePool();
    
    static final int META_DATA_START = GameManager.TERRAIN_SIZE * 3;
    static final int WHITE_HAS_MOVE = META_DATA_START;
    static final int BLACK_HAS_MOVE = META_DATA_START + 1;
    static final int UTIL_CALCED = META_DATA_START + 2;
    static final int DESTROYED = META_DATA_START + 3;
    static final int WALL_START = GameManager.TERRAIN_SIZE * 2;
    
    static final int WALL_VALUE = 15;
    
    final boolean[] t = new boolean[GameManager.TERRAIN_SIZE * 3 + 4];
    int move;
    int hash;
    private int utility;
    State child;
    State nextSibling;
    int round;
    {
        AiAgent.stateCount++;
    }
    
    public static State newFromPool() {
        return pool.get();
    }
    
    public State() {
    }

    public void inherit(State parent, boolean player, int newPiece, byte[] flippedPieces, int flippedPiecesCount) {
        assert !isMarkedDestroyed();
        System.arraycopy(parent.t, 0, t, 0, META_DATA_START);
        t[newPiece] = true;
        t[GameManager.TERRAIN_SIZE + newPiece] = player;
        for (int i = 0; i < flippedPiecesCount; i++) {
            t[GameManager.TERRAIN_SIZE + flippedPieces[i]] = player;
        }
        round = parent.round + 1;
        move = newPiece;
        init();
    }

    public void setData(boolean[] ter, int round, int move) {
        assert !isMarkedDestroyed();
        System.arraycopy(ter, 0, t, 0, GameManager.TERRAIN_SIZE * 2);
        Arrays.fill(t, WALL_START, META_DATA_START, false); // clear walls
        this.round = round;
        this.move = move;
        init();
    }

    void init() {
        assert child == null;
        assert nextSibling == null;
        child = null;
        nextSibling = null;
        t[WHITE_HAS_MOVE] = GameManager.getAMove(t, GameManager.WHITE_BOOL) >= 0;
        t[BLACK_HAS_MOVE] = GameManager.getAMove(t, GameManager.BLACK_BOOL) >= 0;
        t[UTIL_CALCED] = false;
        calcHash();
    }

    public boolean whiteHasMove() {
        return t[WHITE_HAS_MOVE];
    }

    public boolean blackHasMove() {
        return t[BLACK_HAS_MOVE];
    }

    public boolean isTerminal() {
        return !t[WHITE_HAS_MOVE] && !t[BLACK_HAS_MOVE];
    }

    public void setUtility(int utility) {
        this.utility = utility;
        t[UTIL_CALCED] = true;
    }

    public void clearUtility() {
        t[UTIL_CALCED] = false;
    }
    
    public void markDestroyed(boolean destroyed) {
        t[DESTROYED] = destroyed;
    }

    public boolean isMarkedDestroyed() {
        return t[DESTROYED];
    }

    @Override
    public int hashCode() {
        return hash;
    }

    void calcHash() {
        int newHash = 1;
        for (int i = 0; i < GameManager.TERRAIN_SIZE; i++) {
            newHash = newHash * 31 + (t[i] ? (t[GameManager.TERRAIN_SIZE + i] ? 1 : -1) : 0);
        }
        hash = newHash;
    }

    @Override
    public boolean equals(Object obj) {
        // Not checking type for speed
        final State other = (State) obj;
        if (this.hash != other.hash) {
            return false;
        }
        for (int i = 0; i < GameManager.TERRAIN_SIZE; i++) {
            if (this.t[i] != other.t[i]) {
                return false;
            }
            if (this.t[i] && this.t[GameManager.TERRAIN_SIZE + i] != other.t[GameManager.TERRAIN_SIZE + i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the utility/minimax value of this state.
     * For terminal states:
     * BLACK wins => Integer.MIN_VALUE
     * WHITE wins => Integer.MAX_VALUE
     * DRAW => 0
     *
     * For non-terminal states:
     *
     * A wall is a set of pieces that cannot change state (e.g. the opponent cannot take them back)
     *
     * We calculate sum of the values of all pieces.
     * The values of the pieces are the following:
     * BLACK piece on wall: -16
     * WHITE piece on wall: +16
     * BLACK piece simple: -1
     * WHITE piece simple: +1
     *
     * @return the utility/minimax value of this state.
     */
    int calcUtility() {
        if (t[UTIL_CALCED]) {
            return utility;
        }
        if (isTerminal()) {
            int score = 0;
            for (int i = 0; i < GameManager.TERRAIN_SIZE; i++) {
                if (t[i]) {
                    score += t[GameManager.TERRAIN_SIZE + i] ? 1 : -1;
                }
            }
            if (score > 0) {
                return Integer.MAX_VALUE;
            } else if (score < 0) {
                return Integer.MIN_VALUE;
            } else {
                return 0;
            }
        }
        
        int result = 0;
        for (int i = 0; i < GameManager.TERRAIN_SIZE; i++) {
            if (t[i]) {
                result += t[GameManager.TERRAIN_SIZE + i] ? 1 : -1;
            }
        }
        
        // calculate walls
        boolean changed = true;
        boolean bottomUp = false;
        while (changed) {
            changed = false;
            if (bottomUp) {
                for (int i = GameManager.TERRAIN_SIZE - 1; i >= 0; i--) {
                    if (!isWall(i) && t[i]) {
                        if (calcIsWall(i, t[GameManager.TERRAIN_SIZE + i])) {
                            t[WALL_START + i] = true;
                            changed = true;
                        }
                    }
                }
            } else {
                for (int i = 0; i < GameManager.TERRAIN_SIZE; i++) {
                    if (!isWall(i) && t[i]) {
                        if (calcIsWall(i, t[GameManager.TERRAIN_SIZE + i])) {
                            t[WALL_START + i] = true;
                            changed = true;
                        }
                    }
                }
            }
            bottomUp = !bottomUp;
        }
        
        // sum walls
        for (int i = 0; i < GameManager.TERRAIN_SIZE; i++) {
            if (isWall(i)) {
                result += t[GameManager.TERRAIN_SIZE + i] ? WALL_VALUE : -WALL_VALUE;
            }
        }
        
        utility = result;
        return result;
    }

    boolean calcIsWall(int posit, boolean player) {
        boolean noLeft = posit % 8 == 0;
        boolean noUp = posit < 8;
        boolean noRight = posit % 8 == 7;
        boolean noDown = posit > 53;
        return (noUp || noLeft || wallOf(player, posit - 9) || noDown || noRight || wallOf(player, posit + 9) || (isWall(posit - 9) && isWall(posit + 9))) && (noUp || wallOf(player, posit - 8) || noDown || wallOf(player, posit + 9) || (isWall(posit - 8) && isWall(posit + 8))) && (noUp || noRight || wallOf(player, posit - 7) || noDown || noLeft || wallOf(player, posit + 7) || (isWall(posit - 7) && isWall(posit + 7))) && (noRight || wallOf(player, posit + 1) || noLeft || wallOf(player, posit - 1) || (isWall(posit - 1) && isWall(posit + 1)));
    }

    private boolean wallOf(boolean player, int pos) {
        return t[WALL_START + pos] && t[GameManager.TERRAIN_SIZE + pos] == player;
    }

    private boolean isWall(int pos) {
        return t[WALL_START + pos];
    }
    
    // buffer size should be at least MAX_FLIP_COUNT
    State getChildren(boolean player, byte[] buffer) {
        if (child != null || isTerminal()) {
            return child;
        }
        State currentChild = null;
        for (int i = 0; i < GameManager.TERRAIN_SIZE; i++) {
            if (!t[i]) {
                int flipCount = GameManager.calcToFlip(t, player, i, buffer);
                if (flipCount > 0) {
                    if (currentChild == null) {
                        child = pool.get();
                        currentChild = child;
                    } else {
                        currentChild.nextSibling = pool.get();
                        currentChild = currentChild.nextSibling;
                    }
                    currentChild.inherit(this, player, i, buffer, flipCount);
                }
            }
        }
        return child;
    }

    // unlinks linked states and adds this states back to the pool
    void destroy() {
        State currentChild = child;
        child = null;
        nextSibling = null;
        while (currentChild != null) {
            State toDestroy = currentChild;
            currentChild = currentChild.nextSibling;
            toDestroy.destroy();
        }
        pool.accept(this);
    }
    
    void destroySubtree() {
        State currentChild = child;
        child = null;
        while (currentChild != null) {
            State toDestroy = currentChild;
            currentChild = currentChild.nextSibling;
            toDestroy.destroy();
        }
    }
}
