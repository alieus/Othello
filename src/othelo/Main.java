/*
 * 3120005 - Aliprantis Efstathios
 * 3120144 - Pappas Dimitrios-Spiridon
 * 3120178 - Sinacheri Anna-Chloe
 */
package othelo;

import othelo.ui.UiAgent;
import othelo.ai.AiAgent;


public class Main {
    public static void main(String[] args) {
        GameManager gm = new GameManager();
        gm.setWhiteAgent(new UiAgent(gm));
        gm.setBlackAgent(new AiAgent(gm, GameManager.BLACK, GameManager.DEFAULT_AI_LEVEL));
        gm.startAgents();
    }

}
