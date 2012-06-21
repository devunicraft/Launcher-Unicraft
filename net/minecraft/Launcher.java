/*     */ package net.minecraft;
/*     */ 
/*     */ import java.applet.Applet;
/*     */ import java.applet.AppletStub;
/*     */ import java.awt.BorderLayout;
/*     */ import java.awt.Color;
/*     */ import java.awt.Font;
/*     */ import java.awt.FontMetrics;
/*     */ import java.awt.Graphics;
/*     */ import java.awt.Image;
/*     */ import java.awt.event.MouseEvent;
/*     */ import java.awt.event.MouseListener;
/*     */ import java.awt.image.BufferedImage;
/*     */ import java.awt.image.VolatileImage;
/*     */ import java.io.IOException;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.URL;
/*     */ import java.util.HashMap;
/*     */ import java.util.Map;
/*     */ import javax.imageio.ImageIO;
/*     */ 
/*     */ public class Launcher extends Applet
/*     */   implements Runnable, AppletStub, MouseListener
/*     */ {
/*     */   private static final long serialVersionUID = 1L;
/*  24 */   public Map<String, String> customParameters = new HashMap();
/*     */   private GameUpdater gameUpdater;
/*  27 */   private boolean gameUpdaterStarted = false;
/*     */   private Applet applet;
/*     */   private Image bgImage;
/*  30 */   private boolean active = false;
/*  31 */   private int context = 0;
/*  32 */   private boolean hasMouseListener = false;
/*     */   private VolatileImage img;
/*     */ 
/*     */   public boolean isActive()
/*     */   {
/*  38 */     if (this.context == 0) {
/*  39 */       this.context = -1;
/*     */       try {
/*  41 */         if (getAppletContext() != null) this.context = 1; 
/*     */       }
/*     */       catch (Exception localException) {
/*     */       }
/*     */     }
/*  45 */     if (this.context == -1) return this.active;
/*  46 */     return super.isActive();
/*     */   }
/*     */ 
/*     */   public void init(String userName, String latestVersion, String downloadTicket, String sessionId)
/*     */   {
/*     */     try {
/*  52 */       this.bgImage = ImageIO.read(LoginForm.class.getResource("dirt.png")).getScaledInstance(32, 32, 16);
/*     */     } catch (IOException e) {
/*  54 */       e.printStackTrace();
/*     */     }
/*     */ 
/*  57 */     this.customParameters.put("username", userName);
/*  58 */     this.customParameters.put("sessionid", sessionId);
/*     */ 
/*  60 */     this.gameUpdater = new GameUpdater(latestVersion, "minecraft.jar?user=" + userName + "&ticket=" + downloadTicket);
/*     */   }
/*     */ 
/*     */   public boolean canPlayOffline() {
/*  64 */     return this.gameUpdater.canPlayOffline();
/*     */   }
/*     */ 
/*     */   public void init() {
/*  68 */     if (this.applet != null) {
/*  69 */       this.applet.init();
/*  70 */       return;
/*     */     }
/*  72 */     init(getParameter("userName"), getParameter("latestVersion"), getParameter("downloadTicket"), getParameter("sessionId"));
/*     */   }
/*     */ 
/*     */   public void start() {
/*  76 */     if (this.applet != null) {
/*  77 */       this.applet.start();
/*  78 */       return;
/*     */     }
/*  80 */     if (this.gameUpdaterStarted) return;
/*     */ 
/*  82 */     Thread t = new Thread() {
/*     */       public void run() {
/*  84 */         Launcher.this.gameUpdater.run();
/*     */         try {
/*  86 */           if (!Launcher.this.gameUpdater.fatalError)
/*  87 */             Launcher.this.replace(Launcher.this.gameUpdater.createApplet());
/*     */         }
/*     */         catch (ClassNotFoundException e)
/*     */         {
/*  91 */           e.printStackTrace();
/*     */         } catch (InstantiationException e) {
/*  93 */           e.printStackTrace();
/*     */         } catch (IllegalAccessException e) {
/*  95 */           e.printStackTrace();
/*     */         }
/*     */       }
/*     */     };
/*  99 */     t.setDaemon(true);
/* 100 */     t.start();
/*     */ 
/* 102 */     t = new Thread() {
/*     */       public void run() {
/* 104 */         while (Launcher.this.applet == null) {
/* 105 */           Launcher.this.repaint();
/*     */           try {
/* 107 */             Thread.sleep(10L);
/*     */           } catch (InterruptedException e) {
/* 109 */             e.printStackTrace();
/*     */           }
/*     */         }
/*     */       }
/*     */     };
/* 114 */     t.setDaemon(true);
/* 115 */     t.start();
/*     */ 
/* 117 */     this.gameUpdaterStarted = true;
/*     */   }
/*     */ 
/*     */   public void stop() {
/* 121 */     if (this.applet != null) {
/* 122 */       this.active = false;
/* 123 */       this.applet.stop();
/* 124 */       return;
/*     */     }
/*     */   }
/*     */ 
/*     */   public void destroy() {
/* 129 */     if (this.applet != null) {
/* 130 */       this.applet.destroy();
/* 131 */       return;
/*     */     }
/*     */   }
/*     */ 
/*     */   public void replace(Applet applet) {
/* 136 */     this.applet = applet;
/* 137 */     applet.setStub(this);
/* 138 */     applet.setSize(getWidth(), getHeight());
/*     */ 
/* 140 */     setLayout(new BorderLayout());
/* 141 */     add(applet, "Center");
/*     */ 
/* 143 */     applet.init();
/* 144 */     this.active = true;
/* 145 */     applet.start();
/* 146 */     validate();
/*     */   }
/*     */ 
/*     */   public void update(Graphics g)
/*     */   {
/* 153 */     paint(g);
/*     */   }
/*     */ 
/*     */   public void paint(Graphics g2) {
/* 157 */     if (this.applet != null) return;
/*     */ 
/* 159 */     int w = getWidth() / 2;
/* 160 */     int h = getHeight() / 2;
/* 161 */     if ((this.img == null) || (this.img.getWidth() != w) || (this.img.getHeight() != h)) {
/* 162 */       this.img = createVolatileImage(w, h);
/*     */     }
/*     */ 
/* 165 */     Graphics g = this.img.getGraphics();
/* 166 */     for (int x = 0; x <= w / 32; x++) {
/* 167 */       for (int y = 0; y <= h / 32; y++)
/* 168 */         g.drawImage(this.bgImage, x * 32, y * 32, null);
/*     */     }
/* 170 */     if (this.gameUpdater.pauseAskUpdate) {
/* 171 */       if (!this.hasMouseListener) {
/* 172 */         this.hasMouseListener = true;
/* 173 */         addMouseListener(this);
/*     */       }
/* 175 */       g.setColor(Color.LIGHT_GRAY);
/* 176 */       String msg = "Une nouvelle mise à jour est disponible.";
/* 177 */       g.setFont(new Font(null, 1, 20));
/* 178 */       FontMetrics fm = g.getFontMetrics();
/* 179 */       g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 - fm.getHeight() * 2);
/*     */ 
/* 181 */       g.setFont(new Font(null, 0, 12));
/* 182 */       fm = g.getFontMetrics();
/*     */ 
/* 184 */       g.fill3DRect(w / 2 - 56 - 8, h / 2, 56, 20, true);
/* 185 */       g.fill3DRect(w / 2 + 8, h / 2, 56, 20, true);
/*     */ 
/* 187 */       msg = "Voulez-vous mettre à jour?";
/* 188 */       g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 - 8);
/*     */ 
/* 190 */       g.setColor(Color.BLACK);
/* 191 */       msg = "Oui";
/* 192 */       g.drawString(msg, w / 2 - 56 - 8 - fm.stringWidth(msg) / 2 + 28, h / 2 + 14);
/* 193 */       msg = "Non";
/* 194 */       g.drawString(msg, w / 2 + 8 - fm.stringWidth(msg) / 2 + 28, h / 2 + 14);
/*     */     }
/*     */     else
/*     */     {
/* 198 */       g.setColor(Color.LIGHT_GRAY);
/*     */ 
/* 202 */       String msg = "Mise à jour d'Unicraft";
/* 203 */       if (this.gameUpdater.fatalError) {
/* 204 */         msg = "Echec du lancement";
/*     */       }
/*     */ 
/* 207 */       g.setFont(new Font(null, 1, 20));
/* 208 */       FontMetrics fm = g.getFontMetrics();
/* 209 */       g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 - fm.getHeight() * 2);
/*     */ 
/* 211 */       g.setFont(new Font(null, 0, 12));
/* 212 */       fm = g.getFontMetrics();
/* 213 */       msg = this.gameUpdater.getDescriptionForState();
/* 214 */       if (this.gameUpdater.fatalError) {
/* 215 */         msg = this.gameUpdater.fatalErrorDescription;
/*     */       }
/*     */ 
/* 218 */       g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 + fm.getHeight() * 1);
/* 219 */       msg = this.gameUpdater.subtaskMessage;
/* 220 */       g.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2 + fm.getHeight() * 2);
/*     */ 
/* 222 */       if (!this.gameUpdater.fatalError) {
/* 223 */         g.setColor(Color.black);
/* 224 */         g.fillRect(64, h - 64, w - 128 + 1, 5);
/* 225 */         g.setColor(new Color(32768));
/* 226 */         g.fillRect(64, h - 64, this.gameUpdater.percentage * (w - 128) / 100, 4);
/* 227 */         g.setColor(new Color(2138144));
/* 228 */         g.fillRect(65, h - 64 + 1, this.gameUpdater.percentage * (w - 128) / 100 - 2, 1);
/*     */       }
/*     */     }
/*     */ 
/* 232 */     g.dispose();
/*     */ 
/* 236 */     g2.drawImage(this.img, 0, 0, w * 2, h * 2, null);
/*     */   }
/*     */ 
/*     */   public void run() {
/*     */   }
/*     */ 
/*     */   public String getParameter(String name) {
/* 243 */     String custom = (String)this.customParameters.get(name);
/* 244 */     if (custom != null) return custom; try
/*     */     {
/* 246 */       return super.getParameter(name);
/*     */     } catch (Exception e) {
/* 248 */       this.customParameters.put(name, null);
/* 249 */     }return null;
/*     */   }
/*     */ 
/*     */   public void appletResize(int width, int height)
/*     */   {
/*     */   }
/*     */ 
/*     */   public URL getDocumentBase() {
/*     */     try {
/* 258 */       return new URL("http://www.minecraft.net/game/");
/*     */     } catch (MalformedURLException e) {
/* 260 */       e.printStackTrace();
/*     */     }
/* 262 */     return null;
/*     */   }
/*     */ 
/*     */   public void mouseClicked(MouseEvent arg0) {
/*     */   }
/*     */ 
/*     */   public void mouseEntered(MouseEvent arg0) {
/*     */   }
/*     */ 
/*     */   public void mouseExited(MouseEvent arg0) {
/*     */   }
/*     */ 
/*     */   public void mousePressed(MouseEvent me) {
/* 275 */     int x = me.getX() / 2;
/* 276 */     int y = me.getY() / 2;
/* 277 */     int w = getWidth() / 2;
/* 278 */     int h = getHeight() / 2;
/*     */ 
/* 280 */     if (contains(x, y, w / 2 - 56 - 8, h / 2, 56, 20)) {
/* 281 */       removeMouseListener(this);
/* 282 */       this.gameUpdater.shouldUpdate = true;
/* 283 */       this.gameUpdater.pauseAskUpdate = false;
/* 284 */       this.hasMouseListener = false;
/*     */     }
/* 286 */     if (contains(x, y, w / 2 + 8, h / 2, 56, 20)) {
/* 287 */       removeMouseListener(this);
/* 288 */       this.gameUpdater.shouldUpdate = false;
/* 289 */       this.gameUpdater.pauseAskUpdate = false;
/* 290 */       this.hasMouseListener = false;
/*     */     }
/*     */   }
/*     */ 
/*     */   private boolean contains(int x, int y, int xx, int yy, int w, int h) {
/* 295 */     return (x >= xx) && (y >= yy) && (x < xx + w) && (y < yy + h);
/*     */   }
/*     */ 
/*     */   public void mouseReleased(MouseEvent arg0)
/*     */   {
/*     */   }
/*     */ }

/* Location:           C:\Documents and Settings\FLEJA\Bureau\minecraft.jar
 * Qualified Name:     net.minecraft.Launcher
 * JD-Core Version:    0.6.0
 */