package net.minecraft;

import java.util.ArrayList;

public class MinecraftLauncher
{
	private static final float MIN_HEAP = 511.0F;
	private static final long RECOMMENDED_HEAP = 2048L;
	
	public static void main(String[] args) throws Exception
	{
		float heapSizeMegs = (float)(Runtime.getRuntime().maxMemory() / RECOMMENDED_HEAP / RECOMMENDED_HEAP);
		
		if (heapSizeMegs > MIN_HEAP)
			LauncherFrame.main(args);
		else
		{
			try
			{
				String pathToJar = MinecraftLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
				
				ArrayList<String> params = new ArrayList<String>();
				
				params.add("javaw");
				params.add("-Xmx2048m");
				params.add("-Dsun.java2d.noddraw=true");
				params.add("-Dsun.java2d.d3d=false");
				params.add("-Dsun.java2d.opengl=false");
				params.add("-Dsun.java2d.pmoffscreen=false");
				
				params.add("-classpath");
				params.add(pathToJar);
				params.add("net.minecraft.LauncherFrame");
				
				ProcessBuilder pb = new ProcessBuilder(params);
				Process process = pb.start();
				if (process == null) throw new Exception("!");
				System.exit(0);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				LauncherFrame.main(args);
			}
		}
	}
}

/* Location:           C:\Documents and Settings\FLEJA\Bureau\minecraft.jar
 * Qualified Name:     net.minecraft.MinecraftLauncher
 * JD-Core Version:    0.6.0
 */