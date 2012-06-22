/*    */ package net.minecraft;
/*    */
/*     */ import java.applet.Applet;
/*     */ import java.io.BufferedReader;
/*     */ import java.io.BufferedWriter;
/*     */ import java.io.DataInputStream;
/*     */ import java.io.DataOutputStream;
/*     */ import java.io.File;
/*     */ import java.io.FileInputStream;
/*     */ import java.io.FileOutputStream;
/*     */ import java.io.FilePermission;
/*     */ import java.io.FileWriter;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.InputStreamReader;
/*     */ import java.io.OutputStream;
/*     */ import java.io.PrintStream;
/*     */ import java.io.PrintWriter;
/*     */ import java.io.StringWriter;
/*     */ import java.io.Writer;
/*     */ import java.lang.reflect.Constructor;
/*     */ import java.lang.reflect.Field;
/*     */ import java.lang.reflect.Method;
/*     */ import java.net.HttpURLConnection;
/*     */ import java.net.JarURLConnection;
/*     */ import java.net.SocketPermission;
/*     */ import java.net.URI;
/*     */ import java.net.URL;
/*     */ import java.net.URLClassLoader;
/*     */ import java.net.URLConnection;
/*     */ import java.security.AccessControlException;
/*     */ import java.security.AccessController;
/*     */ import java.security.CodeSource;
/*     */ import java.security.PermissionCollection;
/*     */ import java.security.PrivilegedExceptionAction;
/*     */ import java.security.ProtectionDomain;
/*     */ import java.security.SecureClassLoader;
/*     */ import java.security.cert.Certificate;
/*     */ import java.util.Enumeration;
/*     */ import java.util.Scanner;
/*     */ import java.util.StringTokenizer;
/*     */ import java.util.Vector;
/*     */ import java.util.jar.JarEntry;
/*     */ import java.util.jar.JarFile;
/*     */ import java.util.jar.JarOutputStream;
/*     */ import java.util.jar.Pack200;
import java.util.jar.Pack200.Unpacker;
/*    */
/*    */ public class GameUpdater
/*    */  implements Runnable
/*    */ {
/*    */  public static final int STATE_INIT = 1;
/*    */  public static final int STATE_DETERMINING_PACKAGES = 2;
/*    */  public static final int STATE_CHECKING_CACHE = 3;
/*    */  public static final int STATE_DOWNLOADING = 4;
/*    */  public static final int STATE_EXTRACTING_PACKAGES = 5;
/*    */  public static final int STATE_UPDATING_CLASSPATH = 6;
/*    */  public static final int STATE_SWITCHING_APPLET = 7;
/*    */  public static final int STATE_INITIALIZE_REAL_APPLET = 8;
/*    */  public static final int STATE_START_REAL_APPLET = 9;
/*    */  public static final int STATE_DONE = 10;
/*    */  public int percentage;
/*    */  public int currentSizeDownload;
/*    */  public int totalSizeDownload;
public static boolean forceUpdate = false;
/*    */  public int currentSizeExtract;
/*    */  public int totalSizeExtract;
/*    */  protected URL[] urlList;
/*    */  private static ClassLoader classLoader;
/*    */  protected Thread loaderThread;
/*    */  protected Thread animationThread;
/*    */  public boolean fatalError;
          public boolean pauseAskUpdate;
          public boolean shouldUpdate;
/*    */  public String fatalErrorDescription;
/*  80 */  protected String subtaskMessage = "";
/*  81 */  protected int state = 1;
/*    */
/*  83 */  protected boolean lzmaSupported = false;
/*  84 */  protected boolean pack200Supported = false;
/*    */
/*  86 */  protected String[] genericErrorMessage = { "Une erreur est survenue lors du chargement de l'applet.", "Veuillez contacter le support technique pour résoudre ce problème.", "<placeholder for error message>" };
/*    */  protected boolean certificateRefused;
/*  90 */  protected String[] certificateRefusedMessage = { "Autorisations pour l'applet refusées.", "Veuillez accepter la boîte de dialogue des autorisations pour permettre", "l'applet va poursuivre le processus de chargement." };
/*    */
/*  92 */  protected static boolean natives_loaded = false;
/*    */  private String latestVersion;
/*    */  private String mainGameUrl;
/*    */
/*    */  public GameUpdater(String latestVersion, String mainGameUrl)
/*    */  {
/*  97 */    this.latestVersion = latestVersion;
/*  98 */    this.mainGameUrl = mainGameUrl;
/*    */  }
/*    */
/*    */  public void init() {
/* 102 */    this.state = 1;
/*    */    try
/*    */    {
/* 105 */      Class.forName("LZMA.LzmaInputStream");
/* 106 */      this.lzmaSupported = true;
/*    */    }
/*    */    catch (Throwable localThrowable) {
/*    */    }
/*    */    try {
/* 111 */      Pack200.class.getSimpleName();
/* 112 */      this.pack200Supported = true;
/*    */    } catch (Throwable localThrowable1) {
/*    */    }
/*    */  }
/*    */
/*    */  private String generateStacktrace(Exception exception) {
/* 118 */    Writer result = new StringWriter();
/* 119 */    PrintWriter printWriter = new PrintWriter(result);
/* 120 */    exception.printStackTrace(printWriter);
/* 121 */    return result.toString();
/*    */  }
/*    */
/*    */  protected String getDescriptionForState()
/*    */  {
/* 129 */    switch (this.state) {
/*    */    case 1:
/* 131 */      return "Initialisation du téléchargement";
/*    */    case 2:
/* 133 */      return "Détérmination des packs à télécharger";
/*    */    case 3:
/* 135 */      return "Vérifcation du cache pour les fichiers éxistants";
/*    */    case 4:
/* 137 */      return "Téléchargement des packs";
/*    */    case 5:
/* 139 */      return "Extraction des packs";
/*    */    case 6:
/* 141 */      return "Mise à jour du classpath";
/*    */    case 7:
/* 143 */      return "Changement d'applet";
/*    */    case 8:
/* 145 */      return "Initialisation de l'applet";
/*    */    case 9:
/* 147 */      return "Démarrage de l'applet";
/*    */    case 10:
/* 149 */      return "Chargement terminé";
/*     */   case 11:
               return "Mise à  jour Unicraft";
/*    */    }
/* 151 */    return "Phase inconnu";
/*    */  }
/*    */
/*    */  protected String trimExtensionByCapabilities(String file)
/*    */  {
/* 156 */    if (!this.pack200Supported) {
/* 157 */      file = file.replaceAll(".pack", "");
/*    */    }
/*    */
/* 160 */    if (!this.lzmaSupported) {
/* 161 */      file = file.replaceAll(".lzma", "");
/*    */    }
/* 163 */    return file;
/*    */  }
/*    */
/*     */   protected void loadJarURLs() throws Exception {
   /* 165 */     this.state = 2;
   /* 166 */     String jarList = "lwjgl.jar, jinput.jar, lwjgl_util.jar, " + this.mainGameUrl;
   /* 167 */     jarList = trimExtensionByCapabilities(jarList);
   /*     */ 
   /* 169 */     StringTokenizer jar = new StringTokenizer(jarList, ", ");
   /* 170 */     int jarCount = jar.countTokens() + 1;
   /*     */ 
   /* 172 */     this.urlList = new URL[jarCount];
   /*     */ 
   /* 174 */     URL path = new URL("http://s3.amazonaws.com/MinecraftDownload/");
   /*     */ 
   /* 176 */     for (int i = 0; i < jarCount - 1; i++) {
   /* 177 */       String nextToken = jar.nextToken();
   /* 178 */       URL oldPath = path;
   /*     */ 
   /* 181 */       if (nextToken.indexOf("craft.jar") >= 0) {
   /* 182 */         path = new URL("http://dl.dropbox.com/u/87115331/LauncherUnicraft/Client/");
   /*     */       }
   /*     */ 
   /* 185 */       System.out.println(path + nextToken.replaceAll("minecraft.jar", "unicraft.jar"));
   /* 186 */       if (nextToken.indexOf("craft.jar") >= 0) {
   /* 187 */         this.urlList[i] = new URL(path, nextToken.replaceAll("minecraft.jar", "unicraft.jar"));
   /*     */       }
   /*     */       else {
   /* 190 */         this.urlList[i] = new URL(path, nextToken);
   /*     */       }
   /*     */ 
   /* 194 */       if (nextToken.indexOf("craft.jar") >= 0) {
   /* 195 */         path = oldPath;
   /*     */       }
   /*     */     }
   /*     */ 
   /* 199 */     String osName = System.getProperty("os.name");
   /* 200 */     String nativeJar = null;
   /*     */ 
   /* 202 */     if (osName.startsWith("Win"))
   /* 203 */       nativeJar = "windows_natives.jar.lzma";
   /* 204 */     else if (osName.startsWith("Linux"))
   /* 205 */       nativeJar = "linux_natives.jar.lzma";
   /* 206 */     else if (osName.startsWith("Mac"))
   /* 207 */       nativeJar = "macosx_natives.jar.lzma";
   /* 208 */     else if ((osName.startsWith("Solaris")) || (osName.startsWith("SunOS")))
   /* 209 */       nativeJar = "solaris_natives.jar.lzma";
   /*     */     else {
   /* 211 */       fatalErrorOccured("OS (" + osName + ") not supported", null);
   /*     */     }
   /*     */ 
   /* 214 */     if (nativeJar == null) {
   /* 215 */       fatalErrorOccured("no lwjgl natives files found", null);
   /*     */     } else {
   /* 217 */       nativeJar = trimExtensionByCapabilities(nativeJar);
   /* 218 */       this.urlList[(jarCount - 1)] = new URL(path, nativeJar);
   /*     */     }
   /*     */   }
/*    */
/*     */   public void run()
/*     */   {
/* 224 */     init();
/* 225 */     this.state = 3;
/*     */ 
/* 227 */     this.percentage = 5;
/*     */     try
/*     */     {
/* 230 */       loadJarURLs();
/*     */ 
/* 232 */       String path = (String)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*     */         public Object run() throws Exception {
/* 234 */           return Util.getWorkingDirectory() + File.separator + "bin" + File.separator;
/*     */         }
/*     */       });
/* 237 */       File dir = new File(path);
/*     */ 
/* 239 */       if (!dir.exists()) {
/* 240 */         dir.mkdirs();
/*     */       }
/*     */ 
/* 243 */       if (this.latestVersion != null) {
/* 244 */         File versionFile = new File(dir, "version");


/*     */ 
/* 246 */         boolean cacheAvailable = false;
/* 247 */         if ((versionFile.exists()) && (
/* 248 */           (this.latestVersion.equals("-1")) || (this.latestVersion.equals(readVersionFile(versionFile))))) {
/* 249 */           cacheAvailable = true;
/* 250 */           this.percentage = 90;
/*     */         }
/*     */ 
/* 254 */         boolean updateunicraft = false;
/*     */         try {
/* 256 */           String version_unicraft = "";
/* 257 */           URL url_version = new URL("http://dl.dropbox.com/u/87115331/LauncherUnicraft/Client/version_unicraft.txt");
/*     */           try {
/* 259 */             BufferedReader in = new BufferedReader(new InputStreamReader(url_version.openStream()));
/* 260 */             version_unicraft = in.readLine();
/*     */           }
/*     */           catch (Exception e) {
/* 263 */             System.err.println(e);
/*     */           }
/* 265 */           File current_version_unicraft = new File(dir, "version_unicraft.txt");
/*     */ 
/* 267 */           if (!current_version_unicraft.exists()) {
/* 268 */             updateunicraft = true;
/*     */             try {
/* 270 */               BufferedWriter bw = new BufferedWriter(new FileWriter(current_version_unicraft));
/* 271 */               bw.append(version_unicraft);
/* 272 */               bw.close();
/*     */             } catch (IOException e) {
/* 274 */               System.out.println("Erreur");
/*     */             }
/*     */           }
/*     */           else
/*     */           {
/*     */             try {
/* 280 */               Scanner scanner = new Scanner(current_version_unicraft);
/* 281 */               while (scanner.hasNextLine()) {
/* 282 */                 String line = scanner.nextLine().trim();
/* 283 */                 if (!version_unicraft.equals(line)) {
/* 284 */                   updateunicraft = true;
/*     */                   try {
/* 286 */                     BufferedWriter bw = new BufferedWriter(new FileWriter(current_version_unicraft));
/* 287 */                     bw.append(version_unicraft);
/* 288 */                     bw.close();
/*     */                   } catch (IOException e) {
/* 290 */                     System.out.println("Erreur");
/*     */                   }
/*     */                 }
/*     */               }
/*     */ 
/* 295 */               scanner.close();
/*     */             } catch (IOException e) {
/* 297 */               System.out.println("Erreur" + e.getMessage());
/*     */             }
/*     */ 
/*     */           }
/*     */ 
/*     */         }
/*     */         catch (Exception localException1)
/*     */         {
/*     */         }
/*     */ 
/* 308 */         if ((!cacheAvailable) || (updateunicraft) || (forceUpdate))  {
/* 309 */           downloadJars(path);
/* 310 */           extractJars(path);
/* 311 */           extractNatives(path);
/*     */ 
/* 313 */           if (this.latestVersion != null) {
/* 314 */             this.percentage = 90;
/* 315 */             writeVersionFile(versionFile, this.latestVersion);
/*     */           }
/*     */         }
/*     */       }
/*     */ 
/* 320 */       updateClassPath(dir);
/* 321 */       this.state = 10;
/*     */     } catch (AccessControlException ace) {
/* 323 */       fatalErrorOccured(ace.getMessage(), ace);
/* 324 */       this.certificateRefused = true;
/*     */     } catch (Exception e) {
/* 326 */       fatalErrorOccured(e.getMessage(), e);
/*     */     } finally {
/* 328 */       this.loaderThread = null;
/*     */     }
/*     */   }
/*     */ 
/*     */   protected String readVersionFile(File file) throws Exception {
/* 333 */     DataInputStream dis = new DataInputStream(new FileInputStream(file));
/* 334 */     String version = dis.readUTF();
/* 335 */     dis.close();
/* 336 */     return version;
/*     */   }
/*     */ 
/*     */   protected void writeVersionFile(File file, String version) throws Exception {
/* 340 */     DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
/* 341 */     dos.writeUTF(version);
/* 342 */     dos.close();
/*     */   }
/*    */
/*    */  protected void updateClassPath(File dir)
/*    */    throws Exception
/*    */  {
/* 278 */    this.state = 6;
/*    */
/* 280 */    this.percentage = 95;
/*    */
/* 282 */    URL[] urls = new URL[this.urlList.length];
/* 283 */    for (int i = 0; i < this.urlList.length; i++) {
/* 284 */      urls[i] = new File(dir, getJarName(this.urlList[i])).toURI().toURL();
/*    */    }
/*    */
/* 287 */    if (classLoader == null) {
/* 288 */      classLoader = new URLClassLoader(urls) {
/*    */        protected PermissionCollection getPermissions(CodeSource codesource) {
/* 290 */          PermissionCollection perms = null;
/*    */          try
/*    */          {
/* 294 */            Method method = SecureClassLoader.class.getDeclaredMethod("getPermissions", new Class[] { CodeSource.class });
/* 295 */            method.setAccessible(true);
/* 296 */            perms = (PermissionCollection)method.invoke(getClass().getClassLoader(), new Object[] { codesource });
/*    */
/* 298 */            String host = "www.minecraft.net";
/*    */
/* 300 */            if ((host != null) && (host.length() > 0))
/*    */            {
/* 302 */              perms.add(new SocketPermission(host, "connect,accept"));
/*    */            } else codesource.getLocation().getProtocol().equals("file");
/*    */
/* 306 */            perms.add(new FilePermission("<<ALL FILES>>", "read"));
/*    */          }
/*    */          catch (Exception e) {
/* 309 */            e.printStackTrace();
/*    */          }
/*    */
/* 312 */          return perms;
/*    */        }
/*    */      };
/*    */    }
/* 317 */    String path = dir.getAbsolutePath();
/* 318 */    if (!path.endsWith(File.separator)) path = path + File.separator;
/* 319 */    unloadNatives(path);
/*    */
/* 321 */    System.setProperty("org.lwjgl.librarypath", path + "natives");
/* 322 */    System.setProperty("net.java.games.input.librarypath", path + "natives");
/*    */
/* 324 */    natives_loaded = true;
/*    */  }
/*    */
/*    */  private void unloadNatives(String nativePath)
/*    */  {
/* 329 */    if (!natives_loaded) {
/* 330 */      return;
/*    */    }
/*    */    try
/*    */    {
/* 334 */      Field field = ClassLoader.class.getDeclaredField("loadedLibraryNames");
/* 335 */      field.setAccessible(true);
/* 336 */      Vector libs = (Vector)field.get(getClass().getClassLoader());
/*    */
/* 338 */      String path = new File(nativePath).getCanonicalPath();
/*    */
/* 340 */      for (int i = 0; i < libs.size(); i++) {
/* 341 */        String s = (String)libs.get(i);
/*    */
/* 343 */        if (s.startsWith(path)) {
/* 344 */          libs.remove(i);
/* 345 */          i--;
/*    */        }
/*    */      }
/*    */    } catch (Exception e) {
/* 349 */      e.printStackTrace();
/*    */    }
/*    */  }
/*    */
/*    */  public Applet createApplet() throws ClassNotFoundException, InstantiationException, IllegalAccessException
/*    */  {
/* 355 */    Class appletClass = classLoader.loadClass("net.minecraft.client.MinecraftApplet");
/* 356 */    return (Applet)appletClass.newInstance();
/*    */  }
/*    */
/*    */  protected void downloadJars(String path)
/*    */    throws Exception
/*    */  {
/* 384 */    this.state = 4;
/*    */
/* 389 */    int[] fileSizes = new int[this.urlList.length];
/*    */
/* 392 */    for (int i = 0; i < this.urlList.length; i++) {
/* 393 */      System.out.println(this.urlList[i]);
/* 394 */      URLConnection urlconnection = this.urlList[i].openConnection();
/* 395 */      urlconnection.setDefaultUseCaches(false);
/* 396 */      if ((urlconnection instanceof HttpURLConnection)) {
/* 397 */        ((HttpURLConnection)urlconnection).setRequestMethod("HEAD");
/*    */      }
/* 399 */      fileSizes[i] = urlconnection.getContentLength();
/* 400 */      this.totalSizeDownload += fileSizes[i];
/*    */    }
/*    */
/* 403 */    int initialPercentage = this.percentage = 10;
/*    */
/* 406 */    byte[] buffer = new byte[65536];
/* 407 */    for (int i = 0; i < this.urlList.length; i++)
/*    */    {
/* 409 */      int unsuccessfulAttempts = 0;
/* 410 */      int maxUnsuccessfulAttempts = 3;
/* 411 */      boolean downloadFile = true;
/*    */
/* 414 */      while (downloadFile) {
/* 415 */        downloadFile = false;
/*    */
/* 417 */        URLConnection urlconnection = this.urlList[i].openConnection();
/*    */
/* 419 */        if ((urlconnection instanceof HttpURLConnection)) {
/* 420 */          urlconnection.setRequestProperty("Cache-Control", "no-cache");
/* 421 */          urlconnection.connect();
/*    */        }
/*    */
/* 424 */        String currentFile = getFileName(this.urlList[i]);
/* 425 */        InputStream inputstream = getJarInputStream(currentFile, urlconnection);
/* 426 */        FileOutputStream fos = new FileOutputStream(path + currentFile);
/*    */
/* 430 */        long downloadStartTime = System.currentTimeMillis();
/* 431 */        int downloadedAmount = 0;
/* 432 */        int fileSize = 0;
/* 433 */        String downloadSpeedMessage = "";
/*    */        int bufferSize;
/* 435 */        while ((bufferSize = inputstream.read(buffer, 0, buffer.length)) != -1)
/*    */        {
/* 436 */          fos.write(buffer, 0, bufferSize);
/* 437 */          this.currentSizeDownload += bufferSize;
/* 438 */          fileSize += bufferSize;
/* 439 */          this.percentage = (initialPercentage + this.currentSizeDownload * 45 / this.totalSizeDownload);
/* 440 */          this.subtaskMessage = ("Téléchargement de " + currentFile + " " + this.currentSizeDownload * 100 / this.totalSizeDownload + "%");
/*    */
/* 442 */          downloadedAmount += bufferSize;
/* 443 */          long timeLapse = System.currentTimeMillis() - downloadStartTime;
/*    */
/* 445 */          if (timeLapse >= 1000L)
/*    */          {
/* 447 */            float downloadSpeed = downloadedAmount / (float)timeLapse;
/*    */
/* 449 */            downloadSpeed = (int)(downloadSpeed * 100.0F) / 100.0F;
/*    */
/* 451 */            downloadSpeedMessage = " à " + downloadSpeed + " KB/sec";
/*    */
/* 453 */            downloadedAmount = 0;
/*    */
/* 455 */            downloadStartTime += 1000L;
/*    */          }
/*    */
/* 458 */          this.subtaskMessage += downloadSpeedMessage;
/*    */        }
/*    */
/* 461 */        inputstream.close();
/* 462 */        fos.close();
/*    */
/* 465 */        if ((!(urlconnection instanceof HttpURLConnection)) ||
/* 466 */          (fileSize == fileSizes[i]))
/*    */          continue;
/* 468 */        if (fileSizes[i] <= 0)
/*    */        {
/*    */          continue;
/*    */        }
/* 472 */        unsuccessfulAttempts++;
/*    */
/* 474 */        if (unsuccessfulAttempts < maxUnsuccessfulAttempts) {
/* 475 */          downloadFile = true;
/* 476 */          this.currentSizeDownload -= fileSize;
/*    */        }
/*    */        else {
/* 479 */          throw new Exception("Impossible de télécharger " + currentFile);
/*    */        }
/*    */      }
/*    */
/*    */    }
/*    */
/* 485 */    this.subtaskMessage = "";
/*    */  }
/*    */
/*    */  protected InputStream getJarInputStream(String currentFile, final URLConnection urlconnection)
/*    */    throws Exception
/*    */  {
/* 496 */    final InputStream[] is = new InputStream[1];
/*    */
/* 500 */    for (int j = 0; (j < 3) && (is[0] == null); j++) {
/* 501 */      Thread t = new Thread() {
/*    */        public void run() {
/*    */          try {
/* 504 */            is[0] = urlconnection.getInputStream();
/*    */          }
/*    */          catch (IOException localIOException)
/*    */          {
/*    */          }
/*    */        }
/*    */      };
/* 510 */      t.setName("JarInputStreamThread");
/* 511 */      t.start();
/*    */
/* 513 */      int iterationCount = 0;
/* 514 */      while ((is[0] == null) && (iterationCount++ < 5)) {
/*    */        try {
/* 516 */          t.join(1000L);
/*    */        }
/*    */        catch (InterruptedException localInterruptedException)
/*    */        {
/*    */        }
/*    */      }
/* 522 */      if (is[0] != null) continue;
/*    */      try {
/* 524 */        t.interrupt();
/* 525 */        t.join();
/*    */      }
/*    */      catch (InterruptedException localInterruptedException1)
/*    */      {
/*    */      }
/*    */    }
/*    */
/* 532 */    if (is[0] == null) {
/* 533 */      if (currentFile.equals("minecraft.jar")) {
/* 534 */        throw new Exception("Impossible de télécharger " + currentFile);
/*    */      }
/* 536 */      throw new Exception("Impossible de télécharger " + currentFile);
/*    */    }
/*    */
/* 541 */    return is[0];
/*    */  }
/*    */
/*    */  protected void extractLZMA(String in, String out)
/*    */    throws Exception
/*    */  {
/* 553 */    File f = new File(in);
/* 554 */    FileInputStream fileInputHandle = new FileInputStream(f);
/*    */
/* 557 */    Class clazz = Class.forName("LZMA.LzmaInputStream");
/* 558 */    Constructor constructor = clazz.getDeclaredConstructor(new Class[] { InputStream.class });
/* 559 */    InputStream inputHandle = (InputStream)constructor.newInstance(new Object[] { fileInputHandle });
/*    */
/* 562 */    OutputStream outputHandle = new FileOutputStream(out);
/*    */
/* 564 */    byte[] buffer = new byte[16384];
/*    */
/* 566 */    int ret = inputHandle.read(buffer);
/* 567 */    while (ret >= 1) {
/* 568 */      outputHandle.write(buffer, 0, ret);
/* 569 */      ret = inputHandle.read(buffer);
/*    */    }
/*    */
/* 572 */    inputHandle.close();
/* 573 */    outputHandle.close();
/*    */
/* 575 */    outputHandle = null;
/* 576 */    inputHandle = null;
/*    */
/* 579 */    f.delete();
/*    */  }
/*    */
/*    */  protected void extractPack(String in, String out)
/*    */    throws Exception
/*    */  {
/* 590 */    File f = new File(in);
/* 591 */    FileOutputStream fostream = new FileOutputStream(out);
/* 592 */    JarOutputStream jostream = new JarOutputStream(fostream);
/*    */
/* 594 */    Pack200.Unpacker unpacker = Pack200.newUnpacker();
/* 595 */    unpacker.unpack(f, jostream);
/* 596 */    jostream.close();
/*    */
/* 599 */    f.delete();
/*    */  }
/*    */
/*    */  protected void extractJars(String path)
/*    */    throws Exception
/*    */  {
/* 609 */    this.state = 5;
/*    */
/* 611 */    float increment = 10.0F / this.urlList.length;
/*    */
/* 613 */    for (int i = 0; i < this.urlList.length; i++) {
/* 614 */      this.percentage = (55 + (int)(increment * (i + 1)));
/* 615 */      String filename = getFileName(this.urlList[i]);
/*    */
/* 617 */      if (filename.endsWith(".pack.lzma")) {
/* 618 */        this.subtaskMessage = ("Extracting: " + filename + " to " + filename.replaceAll(".lzma", ""));
/* 619 */        extractLZMA(path + filename, path + filename.replaceAll(".lzma", ""));
/*    */
/* 621 */        this.subtaskMessage = ("Extracting: " + filename.replaceAll(".lzma", "") + " to " + filename.replaceAll(".pack.lzma", ""));
/* 622 */        extractPack(path + filename.replaceAll(".lzma", ""), path + filename.replaceAll(".pack.lzma", ""));
/* 623 */      } else if (filename.endsWith(".pack")) {
/* 624 */        this.subtaskMessage = ("Extracting: " + filename + " to " + filename.replace(".pack", ""));
/* 625 */        extractPack(path + filename, path + filename.replace(".pack", ""));
/* 626 */      } else if (filename.endsWith(".lzma")) {
/* 627 */        this.subtaskMessage = ("Extracting: " + filename + " to " + filename.replace(".lzma", ""));
/* 628 */        extractLZMA(path + filename, path + filename.replace(".lzma", ""));
/*    */      }
/*    */    }
/*    */  }
/*    */
/*    */  protected void extractNatives(String path) throws Exception
/*    */  {
/* 635 */    this.state = 5;
/*    */
/* 637 */    int initialPercentage = this.percentage;
/*    */
/* 639 */    String nativeJar = getJarName(this.urlList[(this.urlList.length - 1)]);
/*    */
/* 641 */    Certificate[] certificate = Launcher.class.getProtectionDomain().getCodeSource().getCertificates();
/*    */
/* 643 */    if (certificate == null) {
/* 644 */      URL location = Launcher.class.getProtectionDomain().getCodeSource().getLocation();
/*    */
/* 646 */      JarURLConnection jurl = (JarURLConnection)new URL("jar:" + location.toString() + "!/net/minecraft/Launcher.class").openConnection();
/* 647 */      jurl.setDefaultUseCaches(true);
/*    */      try {
/* 649 */        certificate = jurl.getCertificates();
/*    */      }
/*    */      catch (Exception localException)
/*    */      {
/*    */      }
/*    */    }
/* 655 */    File nativeFolder = new File(path + "natives");
/* 656 */    if (!nativeFolder.exists()) {
/* 657 */      nativeFolder.mkdir();
/*    */    }
/*    */
/* 660 */    JarFile jarFile = new JarFile(path + nativeJar, true);
/* 661 */    Enumeration entities = jarFile.entries();
/*    */
/* 663 */    this.totalSizeExtract = 0;
/*    */
/* 666 */    while (entities.hasMoreElements()) {
/* 667 */      JarEntry entry = (JarEntry)entities.nextElement();
/*    */
/* 671 */      if ((entry.isDirectory()) || (entry.getName().indexOf('/') != -1)) {
/*    */        continue;
/*    */      }
/* 674 */      this.totalSizeExtract = (int)(this.totalSizeExtract + entry.getSize());
/*    */    }
/*    */
/* 677 */    this.currentSizeExtract = 0;
/*    */
/* 679 */    entities = jarFile.entries();
/*    */
/* 681 */    while (entities.hasMoreElements()) {
/* 682 */      JarEntry entry = (JarEntry)entities.nextElement();
/*    */
/* 684 */      if ((entry.isDirectory()) || (entry.getName().indexOf('/') != -1))
/*    */      {
/*    */        continue;
/*    */      }
/* 688 */      File f = new File(path + "natives" + File.separator + entry.getName());
/* 689 */      if ((f.exists()) &&
/* 690 */        (!f.delete()))
/*    */      {
/*    */        continue;
/*    */      }
/*    */
/* 695 */      InputStream in = jarFile.getInputStream(jarFile.getEntry(entry.getName()));
/* 696 */      OutputStream out = new FileOutputStream(path + "natives" + File.separator + entry.getName());
/*    */
/* 699 */      byte[] buffer = new byte[65536];
/*    */      int bufferSize;
/* 701 */      while ((bufferSize = in.read(buffer, 0, buffer.length)) != -1)
/*    */      {
/* 702 */        out.write(buffer, 0, bufferSize);
/* 703 */        this.currentSizeExtract += bufferSize;
/*    */
/* 705 */        this.percentage = (initialPercentage + this.currentSizeExtract * 20 / this.totalSizeExtract);
/* 706 */        this.subtaskMessage = ("Extracting: " + entry.getName() + " " + this.currentSizeExtract * 100 / this.totalSizeExtract + "%");
/*    */      }
/*    */
/* 709 */      validateCertificateChain(certificate, entry.getCertificates());
/*    */
/* 711 */      in.close();
/* 712 */      out.close();
/*    */    }
/* 714 */    this.subtaskMessage = "";
/*    */
/* 716 */    jarFile.close();
/*    */
/* 718 */    File f = new File(path + nativeJar);
/* 719 */    f.delete();
/*    */  }
/*    */
/*    */  protected static void validateCertificateChain(Certificate[] ownCerts, Certificate[] native_certs)
/*    */    throws Exception
/*    */  {
/* 729 */    if (ownCerts == null) return;
/* 730 */    if (native_certs == null) throw new Exception("Unable to validate certificate chain. Native entry did not have a certificate chain at all");
/*    */
/* 732 */    if (ownCerts.length != native_certs.length) throw new Exception("Unable to validate certificate chain. Chain differs in length [" + ownCerts.length + " vs " + native_certs.length + "]");
/*    */
/* 734 */    for (int i = 0; i < ownCerts.length; i++)
/* 735 */      if (!ownCerts[i].equals(native_certs[i]))
/* 736 */        throw new Exception("Certificate mismatch: " + ownCerts[i] + " != " + native_certs[i]);
/*    */  }
/*    */
/*    */  protected String getJarName(URL url)
/*    */  {
/* 742 */    String fileName = url.getFile();
/*    */
/* 744 */    if (fileName.contains("?")) {
/* 745 */      fileName = fileName.substring(0, fileName.indexOf("?"));
/*    */    }
/* 747 */    if (fileName.endsWith(".pack.lzma"))
/* 748 */      fileName = fileName.replaceAll(".pack.lzma", "");
/* 749 */    else if (fileName.endsWith(".pack"))
/* 750 */      fileName = fileName.replaceAll(".pack", "");
/* 751 */    else if (fileName.endsWith(".lzma")) {
/* 752 */      fileName = fileName.replaceAll(".lzma", "");
/*    */    }
/*    */
/* 755 */    return fileName.substring(fileName.lastIndexOf('/') + 1);
/*    */  }
/*    */
/*    */  protected String getFileName(URL url) {
/* 759 */    String fileName = url.getFile();
/* 760 */    if (fileName.contains("?")) {
/* 761 */      fileName = fileName.substring(0, fileName.indexOf("?"));
/*    */    }
/* 763 */    return fileName.substring(fileName.lastIndexOf('/') + 1);
/*    */  }
/*    */
/*    */  protected void fatalErrorOccured(String error, Exception e) {
/* 767 */    e.printStackTrace();
/* 768 */    this.fatalError = true;
/* 769 */    this.fatalErrorDescription = ("Fatal error occured (" + this.state + "): " + error);
/* 770 */    System.out.println(this.fatalErrorDescription);
/* 771 */    if (e != null)
/* 772 */      System.out.println(generateStacktrace(e));
/*    */  }
/*    */
/*    */  public boolean canPlayOffline()
/*    */  {
/*    */    try
/*    */    {
/* 779 */      String path = (String)AccessController.doPrivileged(new PrivilegedExceptionAction() {
/*    */        public Object run() throws Exception {
/* 781 */          return Util.getWorkingDirectory() + File.separator + "bin" + File.separator;
/*    */        }
/*    */      });
/* 785 */      File dir = new File(path);
/* 786 */      if (!dir.exists()) return false;
/*    */
/* 788 */      dir = new File(dir, "version");
/* 789 */      if (!dir.exists()) return false;
/*    */
/* 791 */      if (dir.exists()) {
/* 792 */        String version = readVersionFile(dir);
/* 793 */        if ((version != null) && (version.length() > 0))
/* 794 */          return true;
/*    */      }
/*    */    }
/*    */    catch (Exception e) {
/* 798 */      e.printStackTrace();
/* 799 */      return false;
/*    */    }
/* 801 */    return false;
/*    */  }
/*    */ }