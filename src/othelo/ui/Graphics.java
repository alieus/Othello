/*
 * 3120005 - Aliprantis Efstathios
 * 3120144 - Pappas Dimitrios-Spiridon
 * 3120178 - Sinacheri Anna-Chloe
 */
package othelo.ui;
import gr.entij.event.*;
import gr.entij.graphics2d.effects.*;
import gr.entij.graphics2d.gentities.*;
import gr.entij.*;
import static gr.entij.EFilter.*;
import gr.entij.graphics2d.positionings.DefaultPositionTransformation;
import static gr.entij.graphics2d.Util.readImage;
import static java.util.Arrays.asList;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import gr.entij.graphics2d.*;
import gr.entij.graphics2d.positionings.AreaTransformation;

import java.awt.Cursor;
import java.awt.Point;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.border.EmptyBorder;

import othelo.Agent;
import othelo.GameManager;
import  static othelo.GameManager.*;
import othelo.ai.AiAgent;

/**
 * This class is responsible for the graphical user interface of the application.
 */
public class Graphics {
    
    public static final String ABOUT_TEXT =
"<html>\n" +
"<h1>Othello Impl.</h1>\n" +
"<p/>" +
"<font size=\"4\">Original Authors</font>:<br/>\n" +
"Stathis Aliprantis (CS Dept. AUEB)<br/>\n" +
"Chloe Synacheri (CS Dept. AUEB)<br/>\n" +
"Dimitris Pappas (CS Dept. AUEB)<br/>\n" +
"<br/>" +
"<font size=\"4\">Maintainer/Design</font>: Stathis Aliprantis - alieus@hotmail.gr<br/>\n" +
"<br/>" +
"<font size=\"4\">Version</font>: 0.2 - 2014 12 11<br/>\n" +
"<br/>" +
"<font size=\"4\">Game Engine</font>: EntiJ<br/>\n" +
"<br/>" +
"<font size=\"4\">Platform</font>: Java 8 (and later)<br/>\n" +
"<br/>" +
"<font size=\"4\">Iconsets</font>:<br/>\n" +
"Orbz Icons (13 icons used)<br/>\n" +
"Artist: Arrioch (Available for custom work)<br/>\n" +
"License: CC Attribution-Noncommercial-No Derivate 4.0<br/>\n" +
"http://www.iconarchive.com/show<br/>/orbz-icons-by-arrioch.html<br/>\n" +
"<br/>" +
"Mac Icons (1 icon used)<br/>\n" +
"Artist: Artua.com (Available for custom work)<br/>\n" +
"License: Free for non-commercial use.<br/>\n" +
"http://www.iconarchive.com/show/<br/>mac-icons-by-artua/Setting-icon.html<br/>\n" +
"</html>";
    
    static BufferedImage whitePiece;
    static BufferedImage blackPiece;
    static BufferedImage black;
    static BufferedImage white;
    static BufferedImage selector1;
    static BufferedImage config;

    private static final String[] imageFiles = {"orbz-sun-icon.png", "orbz-moon-icon.png",
            "orbz-nature-icon.png", "orbz-fire-icon.png", "orbz-air-icon.png",
            "orbz-life-icon.png", "orbz-lightning-icon.png", "orbz-machine-icon.png",
            "orbz-water-icon.png", "orbz-spirit-icon.png", "orbz-ice-icon.png",
            "orbz-earth-icon.png", "orbz-death-icon.png"};
    
    private static final List<BufferedImage> images = new ArrayList<>(imageFiles.length);
    
    static SmoothChangeImage toBlack;
    static SmoothChangeImage toWhite;
    
    static final String PLAYER_ICON = "graphic.player icon";
    static final String GRAPHIC_TERRAIN = "graphic.terrain";
    private static final String PLAYER_IMG = "graphic.plima";
    private static final String PLAYER_CONF = "graphic.playerConf";
    private static final String START_NEW = "graphic.start new";
    private static final long SELECT_MOVE = 0;
    private static final long SHOW_HIDE_MOVE = 1;
    private static final long UNSELECT_MOVE = 2;
    
    
    static {
        try {
            for (String imageFile : imageFiles) {
                images.add(readImage("img/" +imageFile));
            }
            whitePiece = images.get(0);
            blackPiece = images.get(1);
            black = readImage("img/" + "blackMar.png");
            white = readImage("img/" + "whiteMar.png");
            selector1 = readImage("img/" + "selector1.png");
            config = readImage("img/" + "setting-icon.png");
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        toBlack = new SmoothChangeImage(600, 18, blackPiece);
        toWhite = new SmoothChangeImage(600, 18, whitePiece);
    }
    
    public static GEntity makeGraphicForPiece(Entity piece, boolean fadeIn) {
        GEntity gent = new GEntity(piece) {
            {
                setZIndex(1);
            }
            
            @Override
            protected void processStateEvent(StateEvent e) {
                addEffect(e.nextState == WHITE ? toWhite : toBlack);
            }

            @Override
            protected void processEntityEvent(EntityEvent e) {
                if (e.type == EntityEvent.Type.DESTROYED) {
                    if (getTarget().getPosit() % 2 != 0
                            || asList(27, 36, 28, 35).contains((int) getTarget().getPosit())) {
                        super.processEntityEvent(e);
                        return;
                    }
                    boolean b = Math.random() > 0.5;
                    int pos;
                    if (getTarget().getState() == WHITE) {
                        pos = b ? 27 : 36;
                    } else {
                        pos = b ? 28 : 35;
                    }
                    Point p = getPositioning().logicalToRealPosit(pos, getPositionType(), parent.getWidth(), parent.getHeight());
                    THREAD_POOL.submit(() -> {
                        setMouseIndex(-1);
                        smoothMove(p.x, p.y, 5, 13);
                        super.processEntityEvent(e);
                    });
                }
            }
            
        };
        if (fadeIn) {
            gent.addEffect(piece.getState() == GameManager.WHITE ? toWhite : toBlack);
        } else {
            gent.setImage(piece.getState() == GameManager.WHITE ? whitePiece : blackPiece);
        }
        return gent;
    }
    
    public static Color background = new Color(19, 19, 19);
    
    public static GTerrain setUpGraphic(GameManager gMan) {
        GTerrain gter = new GTerrain();
        gter.setBackground(background);
        Terrain t = gMan.getTerrain();
        gter.setPositionTransformation(BOARD_POS);
        final Terrain graTer = new Terrain(GRAPHIC_TERRAIN);
        t.add(graTer);
        
        for (Entity ent : t.named(GameManager.SQUARE)) {
            GEntity gent = new GEntity(ent);
            gent.setImage((ent.getPosit() + ent.getPosit()/8) % 2 == 0 ? black : white);
            Border border = new Border(0, 0, 0, 0, Color.GRAY);
            if (ent.getPosit() < 8) border.setUpWidth(2);
            if (ent.getPosit() > 55) border.setDownWidth(2);
            if (ent.getPosit()%8 == 0) border.setLeftWidth(2);
            if (ent.getPosit()%8 == 7) border.setRightWidth(2);
            gent.addEffect(border);
            
            gter.add(gent);
        }
        for (Entity ent : t.named(GameManager.PIECE)) {
            gter.add(makeGraphicForPiece(ent, false));
        }
        t.addAddRemoveListener(e -> {
            if (e.type == EntityEvent.Type.ADDED && GameManager.PIECE.equals(e.source.getName())) {
                gter.add(makeGraphicForPiece(e.source, false));
            }
                
        });
        
        
        setUpGraphicForGamestate(graTer, gter, t);
        setUpPlayerConf(graTer, gMan, gter);
        makeAboutButton(gter, graTer);
        
        gter.setFPS(50);
        
        return gter;
    }
    
    public static GTerrain setUpGraphicAndShowWindow(GameManager gMan, int width, int height) {
        GTerrain graphic = setUpGraphic(gMan);
        JFrame window = new JFrame();
        window.setTitle("Othello");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.add(graphic);
        window.setSize(width, height);
        window.setLocationByPlatform(true);
        window.setVisible(true);
        return graphic;
    }
    
    private static final String ABOUT = "graphic.about";
    
    private static GEntity makeAboutButton(GTerrain gra, Terrain gt) {
        JLabel label = new JLabel("About", JLabel.LEFT);
        label.setSize(40, 17);
        label.setForeground(Color.WHITE.darker().darker());
        label.setFont(new Font(null, Font.PLAIN, 12));
        GEntity result = new AWTComponentGEntity(new Entity(ABOUT), label);
        result.setPositioning(ABOUT_POS);
        result.setZIndex(10);
        result.setMouseIndex(10);
        result.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        JLabel textLabel = new JLabel(ABOUT_TEXT);
        textLabel.setSize(500, 500);
        textLabel.setForeground(new Color(240, 240, 240));
        textLabel.setFont(new Font(null, Font.BOLD, 12));
        textLabel.setBorder(new EmptyBorder(0, 10, 5, 5));
        GEntity aboutText = new AWTComponentGEntity(new Entity("about text"), textLabel);
        aboutText.setPositioning(ABOUT_TEXT_POS);
        aboutText.setZIndex(100);
        aboutText.setMouseIndex(100);
        aboutText.setVisible(false);
        aboutText.addEffect(new Background(Color.BLACK, 0.80f));
        aboutText.addEffect(new Border(2, 2, 2, 2, Color.WHITE));
        
        result.addGtMouseEnterListener(evt -> {
            label.setText(evt.isMouseEnter() ? "<html><u>About</u></html>" : "About");
            result.informUpdate();
        });
        gra.addGtMouseListener(evt -> {
            if (evt.isClick()) {
                if (evt.getEntitySet().hasAny(named(ABOUT)) && !aboutText.isVisible()) {
                    aboutText.setVisible(true);
                } else {
                    aboutText.setVisible(false);
                }
//                new About(null, true).setVisible(true);
            }
        });
        
        gt.add(result.getTarget());
        gra.add(result);
        gt.add(aboutText.getTarget());
        gra.add(aboutText);
        
        return result;
    }
    
    private static void setUpGraphicForGamestate(Terrain gt, GTerrain ter, Entity gamestate) {
        long currPlayer = gamestate.get(GameManager.CURRENT_PLAYER);
        GEntity whiteIcon = new GEntity(new Entity(PLAYER_ICON, 0, GameManager.WHITE));
        GEntity blackIcon = new GEntity(new Entity(PLAYER_ICON, 8, GameManager.BLACK));
        GEntity filler1 = new ColorGEntity(new Entity(null, 0, 0), background, 1.0F);
        GEntity filler2 = new ColorGEntity(new Entity(null, 8, 0), background, 1.0F);
        GEntity selector = new GEntity(new Entity(null, currPlayer == WHITE ? 0 : 8, 0));
        JLabel whiteScoreLb = new JLabel(gamestate.get(GameManager.WHITE+"")+"");
        JLabel blackScoreLb = new JLabel(gamestate.get(GameManager.BLACK+"")+"");
        GEntity whiteScoreG = new AWTComponentGEntity(new Entity(null, 2, 0), whiteScoreLb);
        GEntity blackScoreG = new AWTComponentGEntity(new Entity(null, 5, 0), blackScoreLb);
        
        whiteIcon.setImage(whitePiece);
        blackIcon.setImage(blackPiece);
//        selector.setImage(selector1);
        Font font = new Font("arial", 0, 50);
        asList(whiteScoreLb, blackScoreLb).forEach(label -> {
            label.setOpaque(true);
            label.setSize(80, 50);
            label.setForeground(Color.WHITE);
            label.setBackground(background);
            label.setFont(font);
            label.setHorizontalAlignment(SwingConstants.CENTER);
        });
        asList(filler1, filler2, selector, whiteIcon, blackIcon, whiteScoreG, blackScoreG).forEach(gent -> {
            gent.setLogicalHeight(50);
            gent.setLogicalWidth(gent instanceof AWTComponentGEntity ? 75 : 50);
            gent.setPositionType(DefaultPositionTransformation.ABSOLUTE_SIZE);
            gent.setPositioning(TOPBAR_POS);
            ter.add(gent);
            gt.add(gent.getTarget());
        });
        
        selector.setMoveSmoth(false);
        BlinkImage blinkSelector = new BlinkImage(selector1, 600, 700);
        selector.addEffect(blinkSelector);
        
        JLabel gameOver = new JLabel("Game Over!", JLabel.CENTER);
        gameOver.setForeground(new Color(238, 238, 238));
        gameOver.setFont(new Font(null, Font.BOLD, 45));
        gameOver.setSize(350, 150);
        GEntity gameOverGt = new AWTComponentGEntity(new Entity("game over"), gameOver);
        gameOverGt.setPositioning(GAMEOVER_POS);
        gameOverGt.setZIndex(101);
        gameOverGt.setVisible(false);
        ter.add(gameOverGt);
        
        GEntity backOfGameOverGt = new ColorGEntity(new Entity("back of game over"), Color.BLACK, 0.75F);
        backOfGameOverGt.setMouseIndex(100);
        backOfGameOverGt.setZIndex(100);
        backOfGameOverGt.setPositioning(PositionTransformation.EVERYWHERE);
        backOfGameOverGt.setVisible(false);
        ter.add(backOfGameOverGt);

        gamestate.addPropertyListener(e -> {
            int whiteScore = gamestate.get(GameManager.WHITE+"");
            int blackScore = gamestate.get(GameManager.BLACK+"");
            whiteScoreLb.setText(whiteScore+"");
            blackScoreLb.setText(blackScore+"");
            whiteScoreG.informUpdate();
            blackScoreG.informUpdate();
            if (!e.oldValues.containsKey(GameManager.CURRENT_PLAYER)) {
                return;
            }
            long currentPlayer = gamestate.get(GameManager.CURRENT_PLAYER);
            if (currentPlayer != GameManager.NO_ONE) {
                selector.setVisible(true);
                selector.removeEffect(blinkSelector);
                selector.getTarget().setPosit(currentPlayer == WHITE ? 0 : 8);
                selector.addEffect(blinkSelector);
                selector.informUpdate();
            } else {
                selector.setVisible(false);
                gameOver.setText("<html><em>Game Over</em><br> <center>"+whiteScore+" - "+blackScore+"</center></html>");
                backOfGameOverGt.setVisible(true);
                gameOverGt.setVisible(true);
            }
        });
        
        ter.addGtMouseListener(e -> {
            if (e.isPress() && e.getGEntities().contains(backOfGameOverGt)) {
                backOfGameOverGt.setVisible(false);
                gameOverGt.setVisible(false);
                ter.renderAll();
                gamestate.react(REINITIALIZE);
                gamestate.react(START);
            }
        });
        
    }
    
    private static GEntity makeGraphicForPlima(Entity plima, BufferedImage ima, Terrain gt, GTerrain gra) {
        GEntity result = new ColorGEntity(plima, Color.BLACK, 0.8F) {

            @Override public void attach(Entity target) {
                super.attach(target);
                target.addLogic((e, m) -> {
                    if (m.equals(SHOW_HIDE_MOVE)) {
                        setVisible(!isVisible());
                    } else if (m.equals(SELECT_MOVE)) {
                        Entity selector = gt.named(PLAYER_IMG_SEL)
                                .any(inState(getTarget().getState()))
                                .orElseThrow(IllegalStateException::new);
                        if (selector.getPosit() == getTarget().getPosit()) return null;
                        // react the selector
                        selector.setPosit(getTarget().getPosit());
                        // change all icons
                        setPlayerIcon(getTarget().getState(), getImage(), gra);
                    }
                    return null;
                });
            }
            
        };
        
        PositionTransformation positioning = plima.getState() == WHITE
                ? WHITE_CONF_POS : BLACK_CONF_POS;
        result.setPositioning(positioning);
        result.setImage(ima);
        result.setVisible(false);
        result.setZIndex(10);
        result.setMouseIndex(1);
        
//        Border border = new Border(Color.WHITE);
//        final long posit = plima.getPosit();
//        if (posit%3 != 0) border.setLeftWidth(0);
//        if (posit%3 != 2 && posit != images.size()-1) border.setRightWidth(0);
//        if (posit > 2) border.setUpWidth(0);
//        if (posit < images.size()-3) border.setDownWidth(0);
//        result.addEffect(border);
        
        return result;
    }
    
    private static GEntity makeGraphicForPlayerConf(Entity conf) {
        GEntity result = new GEntity(conf);
        result.setPositionType(DefaultPositionTransformation.ABSOLUTE_SIZE);
        result.setPositioning(Graphics.CONF_POS);
        result.setLogicalSize(25, 25);
        result.setImage(config);
        result.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return result;
    }
    
    private static GEntity makeGraphicForStartNew(Entity start, GameManager gMan, Terrain gt) {
        JLabel label = new JLabel("Start New Game", JLabel.CENTER);
        label.setSize(150, 25);
        label.setForeground(Color.WHITE);
        label.setFont(new Font(null, Font.BOLD, 13));
        GEntity result = new AWTComponentGEntity(start, label) {

            @Override public void attach(Entity target) {
                super.attach(target);
                    target.addLogic((e, m) -> {
                        if (m.equals(SHOW_HIDE_MOVE)) {
                        setVisible(!isVisible());
                    } else if (m.equals(SELECT_MOVE)) {
                        gMan.getTerrain().react(STOP);
                        long selectedWhiteAgent = gt.named(LEVEL_SELECTOR).any(inState(WHITE))
                                .orElseThrow(IllegalStateException::new).getPosit();
                        long selectedBlackAgent = gt.named(LEVEL_SELECTOR).any(inState(BLACK))
                                .orElseThrow(IllegalStateException::new).getPosit();
                        Supplier<Agent> whiteSupplier = gt.named(LEVEL_ENTRY)
                                .any(at(selectedWhiteAgent).and(inState(WHITE)))
                                .orElseThrow(IllegalStateException::new).get(AGENT_SUPPLIER);
                        Supplier<Agent> blackSupplier = gt.named(LEVEL_ENTRY)
                                .any(at(selectedBlackAgent).and(inState(BLACK)))
                                .orElseThrow(IllegalStateException::new).get(AGENT_SUPPLIER);

                        gMan.setWhiteAgent(whiteSupplier.get());
                        gMan.setBlackAgent(blackSupplier.get());
                        gMan.getTerrain().react(REINITIALIZE);
                        gMan.getTerrain().set(CURRENT_PLAYER, getTarget().getState());
        //                gMan.startAgents();
                        gMan.getTerrain().react(START);
                    }
                    return null;
                });
            }
            
        };
        PositionTransformation positioning = start.getState() == WHITE
                ? WHITE_START_NEW_POS : BLACK_START_NEW_POS;
        result.addEffect(new Background(Color.BLACK, 0.8F));
        result.addEffect(new Border(0, 0, 1, 0, Color.WHITE));
        result.setPositioning(positioning);
        result.setVisible(false);
        result.setZIndex(10);
        result.setMouseIndex(10);
        result.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        result.addGtMouseEnterListener(evt -> {
            label.setText(evt.isMouseEnter() ? "<html><u>Start New Game</u></html>" : "Start New Game");
            result.informUpdate();
        });
        
        return result;
    }
    
    static final String AGENT_SUPPLIER = "graphic.agent supplier";
    static final String AGENT_NAME = "graphic.agent name";
    static final String LEVEL_ENTRY = "graphic.level entry";
    static final String LEVEL_SELECTOR = "graphic.level selector";
    static final String LEVEL_LABEL = "graphic.level label";
    
    private static GEntity makeGraphicForSetLevel(Entity ent, GameManager gMan, Terrain gt, boolean selected) {
        String name = ent.get(AGENT_NAME);
        JLabel label = new JLabel(name, JLabel.LEADING);
        label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        label.setSize(100, 20);
        label.setForeground(Color.WHITE);
        label.setFont(new Font(null, selected ? Font.BOLD : Font.PLAIN, 12));
        GEntity result = new AWTComponentGEntity(ent, label) {
            
            @Override public void attach(Entity target) {
                super.attach(target);
                target.addLogic((e, m) -> {
                    if (m == null) return null;
                    if (m.equals(SHOW_HIDE_MOVE)) {
                        setVisible(!isVisible());
                    } else if (m.equals(SELECT_MOVE)) {
                        System.out.println("select level "+getTarget().get(AGENT_NAME));
                        label.setFont(new Font(null, Font.BOLD, 12));
                        Entity sel = gt.named(LEVEL_SELECTOR).any(inStateOf(getTarget())).get();
                        gt.named(LEVEL_ENTRY).any(atPositOf(sel).and(inStateOf(getTarget())))
                            .ifPresent(en -> en.react(UNSELECT_MOVE));
                        sel.setPosit(getTarget().getPosit());
                        informUpdate();
                    } else if (m.equals(UNSELECT_MOVE)) {
                        label.setFont(new Font(null, Font.PLAIN, 12));
                        informUpdate();
                    }
                    return null;
                });
            }
        };
        prepareLevelGEntities(result, true);
        result.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        result.addGtMouseEnterListener(evt -> {
            label.setText(evt.isMouseEnter() ? "<html><u>"+name+"</u></html>" : name);
            result.informUpdate();
        });
        return result;
    }
    
    private static GEntity makeGraphicForSetLevelSel(Entity ent) {
        GEntity result = new GEntity(ent) {

            @Override public void attach(Entity target) {
                super.attach(target);
                target.addLogic((e, m) -> {
                    if (m.equals(SHOW_HIDE_MOVE)) {
                        setVisible(!isVisible());
                    }
                    return null;
                });
            }
        };
//        result.setImage(selector1);
        prepareLevelGEntities(result, true);
        result.addEffect(new Border(1, 0, 1, 0, Color.WHITE));
        result.setMouseIndex(11);
        result.setZIndex(9);
        return result;
    }
    
    private static GEntity makeGraphicForSetLevelLabel(Entity ent) {
        JLabel label = new JLabel("Level", JLabel.CENTER);
        label.setSize(75, 25);
        label.setForeground(Color.WHITE);
        label.setFont(new Font(null, Font.BOLD, 13));
        GEntity result = new AWTComponentGEntity(ent, label) {
            
            @Override public void attach(Entity target) {
                super.attach(target);
                target.addLogic((e, m) -> {
                    if (m.equals(SHOW_HIDE_MOVE)) {
                        setVisible(!isVisible());
                    }
                    return null;
                });
            }
        };
        prepareLevelGEntities(result, true);
        PositionTransformation positioning = result.getTarget().getState() == WHITE
                ? WHITE_LEVEL_CONF_LABEL_POS : BLACK_LEVEL_CONF_LABEL_POS;
        result.setPositioning(positioning);
        result.addEffect(new Border(0, 0, 1, 0, Color.WHITE));
        return result;
    }
    
    private static void prepareLevelGEntities(GEntity ge, boolean background) {
        PositionTransformation positioning = ge.getTarget().getState() == WHITE
                ? WHITE_LEVEL_CONF_POS : BLACK_LEVEL_CONF_POS;
        if (background) {
            ge.addEffect(new Background(Color.BLACK, 0.8F));
        }
        ge.setPositioning(positioning);
        ge.setVisible(false);
        ge.setZIndex(10);
        ge.setMouseIndex(10);
    }
    
    private static void setUpPlayerConf(Terrain gt, GameManager gMan, GTerrain gra) {
        GEntity whiteSelector = new GEntity(new Entity(PLAYER_IMG_SEL, 0, GameManager.WHITE));
        GEntity blackSelector = new GEntity(new Entity(PLAYER_IMG_SEL, 0, GameManager.BLACK));
        whiteSelector.setPositioning(WHITE_CONF_POS);
        blackSelector.setPositioning(BLACK_CONF_POS);
        
        asList(whiteSelector, blackSelector).stream().forEach(selector -> {
            selector.setImage(selector1);
            selector.setVisible(false);
            selector.setZIndex(11);
            gra.add(selector);
            gt.add(selector.getTarget());
            selector.getTarget().addLogic((e, m) -> {
                if (Objects.equals(m, SHOW_HIDE_MOVE)) selector.setVisible(!selector.isVisible());
                return null;
            });
        });
        
        class I { long pl; BufferedImage img; Entity sel;
            I(long p, BufferedImage i, Entity s) {pl=p; img=i; sel=s;}
        } 
        
        asList(new I(GameManager.WHITE, whitePiece, whiteSelector.getTarget()),
             new I(GameManager.BLACK, blackPiece, blackSelector.getTarget()))
                .stream().forEach((i) -> { 
            long p = 0;
            for (BufferedImage img : images) {
                Entity plimaEnt = new Entity(PLAYER_IMG, p, i.pl);
                gra.add(makeGraphicForPlima(plimaEnt, img, gt, gra));
                gt.add(plimaEnt);

                if (img == i.img) {
                    i.sel.setPosit(p);
                }
                p++;
            }
        });
        
        Entity whiteConfTar = new Entity(PLAYER_CONF, 0, 0);
        whiteConfTar.set("player", GameManager.WHITE);
        Entity blackConfTar = new Entity(PLAYER_CONF, 9, 0);
        blackConfTar.set("player", GameManager.BLACK);
        
        asList(whiteConfTar, blackConfTar).stream().forEach((ent) -> {
            gt.add(ent);
            gra.add(makeGraphicForPlayerConf(ent));
            ent.addStateListener(e -> {
                long player = e.source.get("player");
                gt.inState(player).stream()
                    .filter(named(PLAYER_IMG_SEL).or(named(START_NEW))
                        .or(named(LEVEL_ENTRY)).or(named(LEVEL_LABEL))
                        .or(named(LEVEL_SELECTOR)).or(named(PLAYER_IMG)))
                    .forEach(en -> en.react(SHOW_HIDE_MOVE));
                gra.refresh();
            });
        });
        
        asList(WHITE, BLACK).forEach(player -> {
            Entity startNew = new Entity(START_NEW, 0, player);
            gt.add(startNew);
            gra.add(makeGraphicForStartNew(startNew, gMan, gt));
            
            Entity levelSeletor = new Entity(LEVEL_SELECTOR,
                    player == WHITE ? 0 : DEFAULT_AI_LEVEL, player);
            gra.add(makeGraphicForSetLevelSel(levelSeletor));
            gt.add(levelSeletor);
            Entity levelLabel = new Entity(LEVEL_LABEL, 0, player);
            gra.add(makeGraphicForSetLevelLabel(levelLabel));
            gt.add(levelLabel);
            
            Entity uiAgentEntry = new Entity(LEVEL_ENTRY, 0, player);
            uiAgentEntry.set(AGENT_NAME, "Human");
            Supplier<Agent> supplier = () -> new UiAgent(gMan);
            uiAgentEntry.set(AGENT_SUPPLIER, supplier);
            gra.add(makeGraphicForSetLevel(uiAgentEntry, gMan, gt, levelSeletor.getPosit()==0));
            gt.add(uiAgentEntry);
            
            for (int i = 1; i < 10; i++) {
                Entity agentEntry = new Entity(LEVEL_ENTRY, i, player);
                agentEntry.set(AGENT_NAME, "Level "+i);
                final int iF = i;
                Supplier<Agent> aSupplier = () -> new AiAgent(gMan, player, iF);
                agentEntry.set(AGENT_SUPPLIER, aSupplier);
                gra.add(makeGraphicForSetLevel(agentEntry, gMan, gt, levelSeletor.getPosit()==i));
                gt.add(agentEntry);
            }
        });
        
        gra.addGtMouseListener(e -> {
            final EntitySet entitySet = e.getEntitySet();
            Entity plConf = entitySet.any(named(PLAYER_CONF)).orElse(null);
            if (e.isClick()) {
                if (plConf != null) {
                    plConf.setState((plConf.getState()+1)%2);
                }
                entitySet.any(named(PLAYER_IMG)).ifPresent(plima ->
                    plima.react(SELECT_MOVE));
                e.getEntitySet().any(named(START_NEW)).ifPresent(start ->
                    start.react(SELECT_MOVE));
                e.getEntitySet().any(named(LEVEL_ENTRY)).ifPresent(level ->
                    level.react(SELECT_MOVE));
                
                if (plConf == null && !entitySet.hasAny(named(PLAYER_IMG))
                        && !entitySet.hasAny(named(LEVEL_ENTRY)) && !entitySet.hasAny(named(LEVEL_LABEL))) {
                    gt.named(PLAYER_CONF).forEach(ent -> {
                        if (ent.getState() == 1) {
                            ent.setState(0);
                        }
                    });
                }
            }
            
        });
    }
    private static final String PLAYER_IMG_SEL = "graphic.player img sel";
    
    
    private static void setPlayerIcon(long player, BufferedImage icon, GTerrain gra) {
        if (player == GameManager.WHITE) {
            whitePiece = icon;
            toWhite.setNextImage(whitePiece);
        } else {
            blackPiece = icon;
            toBlack.setNextImage(blackPiece);
        }
        gra.getGEntities().stream().forEach((gent) -> {
            Entity geTar = gent.getTarget();
            if ((GameManager.PIECE.equals(geTar.getName()) || PLAYER_ICON.equals(geTar.getName()))
                    && geTar.getState() == player) {
                gent.setImage(icon);
            }
        });
    }
    
    private static final PositionTransformation BOARD_POS = new DefaultPositionTransformation(8, 8)
        .squarize().translateToArea(5, 54, -2, -5);
    
    private static final PositionTransformation TOPBAR_POS = new DefaultPositionTransformation(10, 1)
        .centerX(250).translateToArea(10, 2, 0, 50);

    private static final PositionTransformation CONF_POS = new DefaultPositionTransformation(10, 1)
        .centerX(325).translateToArea(20, 25, 0, 25);

//    private static final PositionTransformation WHITE_CONF_POS = new DefaultPositionTransformation(3, 5)
//        .translateToArea(10, 77, 150, 250).centerX(450);
//    private static final PositionTransformation BLACK_CONF_POS = new DefaultPositionTransformation(3, 5)
//        .translateToArea(-150, 77, 150, 250).centerX(450);
    
    private static final PositionTransformation WHITE_START_NEW_POS = new AreaTransformation(8, 52, 150, 25).centerX(500);
    private static final PositionTransformation BLACK_START_NEW_POS = new AreaTransformation(-221, 52, 150, 25).centerX(500);
    
    private static final PositionTransformation WHITE_CONF_POS = new DefaultPositionTransformation(3, 5)
        .translateToArea(8, 77, 150, 250).centerX(500);
    private static final PositionTransformation BLACK_CONF_POS = new DefaultPositionTransformation(3, 5)
        .translateToArea(-221, 77, 150, 250).centerX(500);
    
    private static final PositionTransformation WHITE_LEVEL_CONF_POS = new DefaultPositionTransformation(1, 10)
        .translateToArea(159, 77, 75, 200).centerX(500);
    private static final PositionTransformation BLACK_LEVEL_CONF_POS = new DefaultPositionTransformation(1, 10)
        .translateToArea(-70, 77, 75, 200).centerX(500);
    
    private static final PositionTransformation WHITE_LEVEL_CONF_LABEL_POS = new AreaTransformation(159, 52, 75, 25).centerX(500);
    private static final PositionTransformation BLACK_LEVEL_CONF_LABEL_POS = new AreaTransformation(-70, 52, 75, 25).centerX(500);

    private static final PositionTransformation GAMEOVER_POS = new DefaultPositionTransformation(1, 1)
            .center(350, 150);
    
    private static final PositionTransformation ABOUT_POS = new AreaTransformation(-1, 30, 40, 17).centerX(475);
    private static final PositionTransformation ABOUT_TEXT_POS = new DefaultPositionTransformation(1, 1)
            .center(500, 500);
    

    
}
