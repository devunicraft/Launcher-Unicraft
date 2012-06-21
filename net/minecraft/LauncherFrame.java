/*     */ package net.minecraft;
/*     */ 
/*     */ import java.awt.BorderLayout;
/*     */ import java.awt.Color;
/*     */ import java.awt.Dimension;
/*     */ import java.awt.Frame;
/*     */ import java.awt.event.WindowAdapter;
/*     */ import java.awt.event.WindowEvent;
/*     */ import java.io.IOException;
/*     */ import java.io.PrintStream;
/*     */ import java.net.URLEncoder;
/*     */ import java.util.HashMap;
/*     */ import java.util.Map;
/*     */ import javax.imageio.ImageIO;
/*     */ import javax.swing.JPanel;
/*     */ import javax.swing.JPasswordField;
/*     */ import javax.swing.JTextField;
import javax.swing.UIManager;
/*     */ 
/*     */ public class LauncherFrame extends Frame
/*     */ {
/*     */   public static final int VERSION = 13;
/*     */   private static final long serialVersionUID = 1L;
/*  21 */   public Map<String, String> customParameters = new HashMap();
/*     */   public Launcher launcher;
/*     */   public LoginForm loginForm;
/*     */ 
/*     */   public LauncherFrame()
/*     */   {
/*  27 */     super("Unicraft");
/*     */ 
/*  29 */     setBackground(Color.BLACK);
/*  30 */     this.loginForm = new LoginForm(this);
/*  31 */     JPanel p = new JPanel();
/*  32 */     p.setLayout(new BorderLayout());
/*  33 */     p.add(this.loginForm, "Center");
/*     */ 
/*  35 */     p.setPreferredSize(new Dimension(854, 480));
/*     */ 
/*  37 */     setLayout(new BorderLayout());
/*  38 */     add(p, "Center");
/*     */ 
/*  40 */     pack();
/*  41 */     setLocationRelativeTo(null);
/*     */     try
/*     */     {
/*  44 */       setIconImage(ImageIO.read(LauncherFrame.class.getResource("favicon.png")));
/*     */     } catch (IOException e1) {
/*  46 */       e1.printStackTrace();
/*     */     }
/*     */ 
/*  49 */     addWindowListener(new WindowAdapter() {
/*     */       public void windowClosing(WindowEvent arg0) {
/*  51 */         new Thread() {
/*     */           public void run() {
/*     */             try {
/*  54 */               Thread.sleep(30000L);
/*     */             } catch (InterruptedException e) {
/*  56 */               e.printStackTrace();
/*     */             }
/*  58 */             System.out.println("FORCING EXIT!");
/*  59 */             System.exit(0);
/*     */           }
/*     */         }
/*  62 */         .start();
/*  63 */         if (LauncherFrame.this.launcher != null) {
/*  64 */           LauncherFrame.this.launcher.stop();
/*  65 */           LauncherFrame.this.launcher.destroy();
/*     */         }
/*  67 */         System.exit(0);
/*     */       } } );
/*     */   }
/*     */ 
/*     */   public void playCached(String userName) {
/*     */     try {
/*  74 */       if ((userName == null) || (userName.length() <= 0)) {
/*  75 */         userName = "Player";
/*     */       }
/*  77 */       this.launcher = new Launcher();
/*  78 */       this.launcher.customParameters.putAll(this.customParameters);
/*  79 */       this.launcher.customParameters.put("userName", userName);
/*  80 */       this.launcher.init();
/*  81 */       removeAll();
/*  82 */       add(this.launcher, "Center");
/*  83 */       validate();
/*  84 */       this.launcher.start();
/*  85 */       this.loginForm = null;
/*  86 */       setTitle("Unicraft");
/*     */     } catch (Exception e) {
/*  88 */       e.printStackTrace();
/*  89 */       showError(e.toString());
/*     */     }
/*     */   }
private void showError(String string) {
	// TODO Auto-generated method stub
	
}
/*     */ 
public String getFakeResult(String userName) {
    return MinecraftUtil.getFakeLatestVersion() + ":35b9fd01865fda9d70b157e244cf801c:" + userName + ":12345:";
  }

  public void login(String userName) {
    String result = getFakeResult(userName);
    String[] values = result.split(":");
    launcher = new Launcher();
    launcher.customParameters.putAll(customParameters);
    launcher.customParameters.put("userName", values[2].trim());
    launcher.customParameters.put("sessionId", values[3].trim());
    launcher.customParameters.put("latestVersion", values[0].trim());
    launcher.customParameters.put("downloadTicket", values[1].trim());
    launcher.init();
    removeAll();
    add(launcher, "Center");
    validate();
    launcher.start();
    loginForm.loginOk();
    loginForm = null;
    setTitle("Unicraft");
  }

/*     */ 
/*     */   public boolean canPlayOffline(String userName) {
/* 146 */     Launcher launcher = new Launcher();
/* 147 */     launcher.customParameters.putAll(this.customParameters);
/* 148 */     launcher.init(userName, null, null, null);
/* 149 */     return launcher.canPlayOffline();
/*     */   }
/*     */ 
/*     */   public static void main(String[] args) {
/*     */     try {
/* 154 */       UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
/*     */     }
/*     */     catch (Exception localException) {
/*     */     }
/* 158 */     LauncherFrame launcherFrame = new LauncherFrame();
/* 159 */     launcherFrame.setVisible(true);
/* 160 */     launcherFrame.customParameters.put("stand-alone", "true");
/*     */ 
/* 162 */     if (args.length >= 3) {
/* 163 */       String ip = args[2];
/* 164 */       String port = "25565";
/* 165 */       if (ip.contains(":")) {
/* 166 */         String[] parts = ip.split(":");
/* 167 */         ip = parts[0];
/* 168 */         port = parts[1];
/*     */       }
/*     */ 
/* 171 */       launcherFrame.customParameters.put("server", ip);
/* 172 */       launcherFrame.customParameters.put("port", port);
/*     */     }
/*     */ 
/* 175 */     if (args.length >= 1) {
/* 176 */       launcherFrame.loginForm.userName.setText(args[0]);
/* 177 */       if (args.length >= 2) {
/* 179 */         launcherFrame.loginForm.doLogin();
/*     */       }
/*     */     }
/*     */   }
/*     */ }

/* Location:           C:\Documents and Settings\FLEJA\Bureau\minecraft.jar
 * Qualified Name:     net.minecraft.LauncherFrame
 * JD-Core Version:    0.6.0
 */