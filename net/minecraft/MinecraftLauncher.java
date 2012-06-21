/*    */ package net.minecraft;
/*    */ 
/*    */ import java.net.URI;
/*    */ import java.net.URL;
/*    */ import java.security.CodeSource;
/*    */ import java.security.ProtectionDomain;
/*    */ import java.util.ArrayList;
/*    */ 
/*    */ public class MinecraftLauncher
/*    */ {
/*    */   private static final int MIN_HEAP = 511;
/*    */   private static final int RECOMMENDED_HEAP = 2048;
/*    */ 
/*    */   public static void main(String[] args)
/*    */     throws Exception
/*    */   {
/* 10 */     float heapSizeMegs = (float)(Runtime.getRuntime().maxMemory() / 2048L / 2048L);
/*    */ 
/* 12 */     if (heapSizeMegs > 511.0F)
/* 13 */       LauncherFrame.main(args);
/*    */     else
/*    */       try {
/* 16 */         String pathToJar = MinecraftLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
/*    */ 
/* 18 */         ArrayList params = new ArrayList();
/*    */ 
/* 20 */         params.add("javaw");
/* 21 */         params.add("-Xmx2048m");
/* 22 */         params.add("-Dsun.java2d.noddraw=true");
/* 23 */         params.add("-Dsun.java2d.d3d=false");
/* 24 */         params.add("-Dsun.java2d.opengl=false");
/* 25 */         params.add("-Dsun.java2d.pmoffscreen=false");
/*    */ 
/* 27 */         params.add("-classpath");
/* 28 */         params.add(pathToJar);
/* 29 */         params.add("net.minecraft.LauncherFrame");
/* 30 */         ProcessBuilder pb = new ProcessBuilder(params);
/* 31 */         Process process = pb.start();
/* 32 */         if (process == null) throw new Exception("!");
/* 33 */         System.exit(0);
/*    */       } catch (Exception e) {
/* 35 */         e.printStackTrace();
/* 36 */         LauncherFrame.main(args);
/*    */       }
/*    */   }
/*    */ }

/* Location:           C:\Documents and Settings\FLEJA\Bureau\minecraft.jar
 * Qualified Name:     net.minecraft.MinecraftLauncher
 * JD-Core Version:    0.6.0
 */