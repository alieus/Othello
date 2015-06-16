/*
 * 3120005 - Aliprantis Efstathios
 * 3120144 - Pappas Dimitrios-Spiridon
 * 3120178 - Sinacheri Anna-Chloe
 */
package othelo.ai;
import java.util.Objects;

final class StatePool {
    static final int DEFAULT_CAPACITY = 3000000;
    private State head;
    private int capacity = DEFAULT_CAPACITY;
    private int size;
    private final Object lock = new Object();
    
    static int pushCount;
    static int getCount;
    
    static void pushGet() {
        System.out.println("get "+getCount+" push "+pushCount);
    }
    
    public StatePool() {
        this(DEFAULT_CAPACITY);
    }

    public StatePool(int capacity) {
        this.capacity = capacity;
    }

    public State get() {
        getCount++;
        synchronized (lock) {
            if (head != null) {
                State result = head;
                head = head.nextSibling;
                size--;
                result.nextSibling = null;
                result.markDestroyed(false);
                return result;
            } else {
                return new State();
            }
        }
    }

    public void accept(State state) {
        Objects.requireNonNull(state, "state may not be nul");
        pushCount++;
        synchronized (lock) {
            assert !state.isMarkedDestroyed() : "already destroyed";
            if (state.isMarkedDestroyed()) return;
            assert state.child == null : "state has child";
            assert state.nextSibling == null : "state has nextSibling";
            state.markDestroyed(true);
            if (size < capacity) {
                state.nextSibling = head;
                head = state;
                size++;
            }
        }
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return capacity;
    }

    public float ratio() {
        return size / (float) capacity;
    }
    
}
