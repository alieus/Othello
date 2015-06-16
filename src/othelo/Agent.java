/*
 * 3120005 - Aliprantis Efstathios
 * 3120144 - Pappas Dimitrios-Spiridon
 * 3120178 - Sinacheri Anna-Chloe
 */
package othelo;

/**
 * An entity that has the ability to determine a suitable next move for a player
 * given the current state of the terrain (board).
 */
public interface Agent {
    
    static interface MoveConsumer {
        boolean acceptMove(int move);
        boolean expired();
    }
    
    void informCanPlay(MoveConsumer mc);
    
    void destroy();
}
