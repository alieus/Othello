/*
 * 3120005 - Aliprantis Efstathios
 * 3120144 - Pappas Dimitrios-Spiridon
 * 3120178 - Sinacheri Anna-Chloe
 */
package othelo.ui;

import gr.entij.graphics2d.event.GTMouseEvent;
import static gr.entij.EFilter.*;
import java.util.function.Consumer;
import othelo.Agent;
import othelo.GameManager;

/**
 * An agent that lets the user of the application select the next react.
 */
public class UiAgent implements Agent {
    private MoveConsumer moveConsumer;
    private final Consumer<GTMouseEvent> listener;
    private GameManager gMan;
    
    public UiAgent(GameManager gMan) {
        this.gMan = gMan;
        listener = e -> {
            if (!e.isPress() || moveConsumer == null || moveConsumer.expired()) return;
            
            e.getEntitySet().any(named(GameManager.SQUARE)).ifPresent(sq ->
                moveConsumer.acceptMove((int) sq.getPosit()));
        };
        gMan.getGraphic().addGtMouseListener(listener);
    }

    @Override
    public void informCanPlay(MoveConsumer mc) {
        this.moveConsumer = mc;
    }
    
    @Override
    public void destroy() {
        gMan.getGraphic().removeGtMouseListener(listener);
    }
}
