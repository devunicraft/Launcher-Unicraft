/*    */ package net.minecraft;
/*    */ 
/*    */ import javax.swing.JButton;
/*    */ 
/*    */ public class TransparentButton extends JButton
/*    */ {
/*    */   private static final long serialVersionUID = 1L;
/*    */ 
/*    */   public TransparentButton(String string)
/*    */   {
/*  9 */     super(string);
/*    */   }
/*    */ 
/*    */   public boolean isOpaque() {
/* 13 */     return false;
/*    */   }
/*    */ }

/* Location:           C:\Documents and Settings\FLEJA\Bureau\minecraft.jar
 * Qualified Name:     net.minecraft.TransparentButton
 * JD-Core Version:    0.6.0
 */