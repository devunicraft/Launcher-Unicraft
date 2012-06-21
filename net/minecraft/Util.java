/*     */ package net.minecraft;
/*     */ 
/*     */ import java.io.BufferedReader;
/*     */ import java.io.DataInputStream;
/*     */ import java.io.DataOutputStream;
/*     */ import java.io.File;
/*     */ import java.io.InputStream;
/*     */ import java.io.InputStreamReader;
/*     */ import java.io.PrintStream;
/*     */ import java.lang.reflect.Method;
/*     */ import java.net.URI;
/*     */ import java.net.URL;
/*     */ import java.security.PublicKey;
/*     */ import java.security.cert.Certificate;
/*     */ import javax.net.ssl.HttpsURLConnection;
/*     */ 
/*     */ public class Util
/*     */ {
/*  21 */   private static File workDir = null;
/*     */ 
/*     */   public static File getWorkingDirectory() {
/*  24 */     if (workDir == null) workDir = getWorkingDirectory("unicraft");
/*  25 */     return workDir;
/*     */   }
/*     */ 
/*     */    public static File getWorkingDirectory(String applicationName) {
    String userHome = System.getProperty("user.home", ".");
    File workingDirectory;
    if(getPlatform() == OS.solaris || getPlatform() ==  OS.linux)
    {
      workingDirectory = new File(userHome, '.' + applicationName + '/');
    }
    else if(getPlatform() == OS.windows)
    {
      String applicationData = System.getenv("APPDATA");
      if (applicationData != null) workingDirectory = new File(applicationData, "." + applicationName + '/'); else
        workingDirectory = new File(userHome, '.' + applicationName + '/');
    }
    else if(getPlatform() == OS.macos)
    {
      workingDirectory = new File(userHome, "Library/Application Support/" + applicationName);
    }
    else
    {
      workingDirectory = new File(userHome, applicationName + '/');
    }
    
    if ((!workingDirectory.exists()) && (!workingDirectory.mkdirs())) throw new RuntimeException("Le répertoire de travail n'a pas pu être créé: " + workingDirectory);
    return workingDirectory;
  }
/*     */ 
/*     */   private static OS getPlatform() {
/*  52 */     String osName = System.getProperty("os.name").toLowerCase();
/*  53 */     if (osName.contains("win")) return OS.windows;
/*  54 */     if (osName.contains("mac")) return OS.macos;
/*  55 */     if (osName.contains("solaris")) return OS.solaris;
/*  56 */     if (osName.contains("sunos")) return OS.solaris;
/*  57 */     if (osName.contains("linux")) return OS.linux;
/*  58 */     if (osName.contains("unix")) return OS.linux;
/*  59 */     return OS.unknown;
/*     */   }
/*     */ 
/*     */   public static String excutePost(String targetURL, String urlParameters)
/*     */   {
/*  64 */     HttpsURLConnection connection = null;
/*     */     try
/*     */     {
/*  67 */       URL url = new URL(targetURL);
/*  68 */       connection = (HttpsURLConnection)url.openConnection();
/*  69 */       connection.setRequestMethod("POST");
/*  70 */       connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
/*     */ 
/*  72 */       connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
/*  73 */       connection.setRequestProperty("Content-Language", "en-US");
/*     */ 
/*  75 */       connection.setUseCaches(false);
/*  76 */       connection.setDoInput(true);
/*  77 */       connection.setDoOutput(true);
/*     */ 
/*  80 */       connection.connect();
/*  81 */       Certificate[] certs = connection.getServerCertificates();
/*     */ 
/*  83 */       byte[] bytes = new byte[294];
/*  84 */       DataInputStream dis = new DataInputStream(Util.class.getResourceAsStream("minecraft.key"));
/*  85 */       dis.readFully(bytes);
/*  86 */       dis.close();
/*     */ 
/*  88 */       Certificate c = certs[0];
/*  89 */       PublicKey pk = c.getPublicKey();
/*  90 */       byte[] data = pk.getEncoded();
/*     */ 
/*  92 */       for (int i = 0; i < data.length; i++) {
/*  93 */         if (data[i] == bytes[i]) continue; throw new RuntimeException("Public key mismatch");
/*     */       }
/*     */ 
/*  97 */       DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
/*  98 */       wr.writeBytes(urlParameters);
/*  99 */       wr.flush();
/* 100 */       wr.close();
/*     */ 
/* 103 */       InputStream is = connection.getInputStream();
/* 104 */       BufferedReader rd = new BufferedReader(new InputStreamReader(is));
/*     */ 
/* 106 */       StringBuffer response = new StringBuffer();
/*     */       String line;
/* 107 */       while ((line = rd.readLine()) != null)
/*     */       {
/* 108 */         response.append(line);
/* 109 */         response.append('\r');
/*     */       }
/* 111 */       rd.close();
/*     */ 
/* 115 */       String str1 = response.toString();
/*     */       return str1;
/*     */     }
/*     */     catch (Exception e)
/*     */     {
/* 119 */       e.printStackTrace();
/*     */       return null;
/*     */     }
/*     */     finally
/*     */     {
/* 124 */       if (connection != null)
/* 125 */         connection.disconnect();
/*     */     }
/*     */   }
/*     */ 
/*     */   public static boolean isEmpty(String str) {
/* 131 */     return (str == null) || (str.length() == 0);
/*     */   }
/*     */ 
/*     */   public static void openLink(URI uri) {
/*     */     try {
/* 136 */       Object o = Class.forName("java.awt.Desktop").getMethod("getDesktop", new Class[0]).invoke(null, new Object[0]);
/* 137 */       o.getClass().getMethod("browse", new Class[] { URI.class }).invoke(o, new Object[] { uri });
/*     */     } catch (Throwable e) {
/* 139 */       System.out.println("Echec de l'ouverture du lien " + uri.toString());
/*     */     }
/*     */   }
/*     */ 
/*     */   private static enum OS
/*     */   {
/*  18 */     linux, solaris, windows, macos, unknown;
/*     */   }
/*     */ }

/* Location:           C:\Documents and Settings\FLEJA\Bureau\minecraft.jar
 * Qualified Name:     net.minecraft.Util
 * JD-Core Version:    0.6.0
 */