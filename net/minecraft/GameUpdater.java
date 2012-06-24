package net.minecraft;

import java.applet.Applet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;

public class GameUpdater implements Runnable
{
	public static final int STATE_INIT = 1;
	public static final int STATE_DETERMINING_PACKAGES = 2;
	public static final int STATE_CHECKING_CACHE = 3;
	public static final int STATE_DOWNLOADING = 4;
	public static final int STATE_EXTRACTING_PACKAGES = 5;
	public static final int STATE_UPDATING_CLASSPATH = 6;
	public static final int STATE_SWITCHING_APPLET = 7;
	public static final int STATE_INITIALIZE_REAL_APPLET = 8;
	public static final int STATE_START_REAL_APPLET = 9;
	public static final int STATE_DONE = 10;
	public static final int STABLE = 11;
	public static final int BETA = 12;
	public int percentage;
	public int currentSizeDownload;
	public int totalSizeDownload;
	public static boolean forceUpdate = false;
	public static int releaseType = STABLE;
	public int currentSizeExtract;
	public int totalSizeExtract;
	protected URL[] urlList;
	private static ClassLoader classLoader;
	protected Thread loaderThread;
	protected Thread animationThread;
	public boolean fatalError;
    public boolean pauseAskUpdate;
    public boolean shouldUpdate;
    public String fatalErrorDescription;
    protected String subtaskMessage = "";
    protected int state = 1;
    
    protected boolean lzmaSupported = false;
    protected boolean pack200Supported = false;

    protected String[] genericErrorMessage = { "Une erreur est survenue lors du chargement de l'applet.", "Veuillez contacter le support technique pour r�soudre ce probl�me.", "<placeholder for error message>" };
    protected boolean certificateRefused;
    protected String[] certificateRefusedMessage = { "Autorisations pour l'applet refus�es.", "Veuillez accepter la bo�te de dialogue des autorisations pour permettre", "l'applet va poursuivre le processus de chargement." };
    protected static boolean natives_loaded = false;
	private String latestVersion;
	private String mainGameUrl;
	
	public GameUpdater(String latestVersion, String mainGameUrl)
	{
		this.latestVersion = latestVersion;
		this.mainGameUrl = mainGameUrl;
	}
	
	public void init()
	{
		this.state = 1;
		try
		{
			Class.forName("LZMA.LzmaInputStream");
			this.lzmaSupported = true;
		}
		catch (Throwable localThrowable)
		{}
		try
		{
			Pack200.class.getSimpleName();
			this.pack200Supported = true;
		}
		catch (Throwable localThrowable1)
		{}
	}
	
	private String generateStacktrace(Exception exception)
	{
		Writer result = new StringWriter();
		PrintWriter printWriter = new PrintWriter(result);
		exception.printStackTrace(printWriter);
		return result.toString();
	}
	
	protected String getDescriptionForState()
	{
		switch (this.state)
		{
		case 1:
			return "Initialisation du t�l�chargement";
			
		case 2:
			return "D�t�rmination des packs � t�l�charger";
			
		case 3:
			return "V�rifcation du cache pour les fichiers �xistants";
			
		case 4:
			return "T�l�chargement des packs";
			
		case 5:
			return "Extraction des packs";
			
		case 6:
			return "Mise � jour du classpath";
			
		case 7:
			return "Changement d'applet";
			
		case 8:
			return "Initialisation de l'applet";
			
		case 9:
			return "D�marrage de l'applet";
			
		case 10:
			return "Chargement termin�";
			
		case 11:
			return "Mise �  jour Unicraft";
		}
		return "Phase inconnu";
	}
	
	protected String trimExtensionByCapabilities(String file)
	{
		if (!this.pack200Supported)
		{
			file = file.replaceAll(".pack", "");
		}
		
		if (!this.lzmaSupported)
		{
			file = file.replaceAll(".lzma", "");
		}
		return file;
	}
	
	protected void loadJarURLs() throws Exception
	{
		this.state = 2;
		String jarList = "lwjgl.jar, jinput.jar, lwjgl_util.jar, " + this.mainGameUrl;
		jarList = trimExtensionByCapabilities(jarList);
		
		StringTokenizer jar = new StringTokenizer(jarList, ", ");
		int jarCount = jar.countTokens() + 1;
		
		this.urlList = new URL[jarCount];
		
		URL path = new URL("http://s3.amazonaws.com/MinecraftDownload/");
		
		for (int i = 0; i < jarCount - 1; i++)
		{
			String nextToken = jar.nextToken();
			URL oldPath = path;
			
			if (nextToken.indexOf("craft.jar") >= 0)
			{
				path = new URL("http://dl.dropbox.com/u/87115331/LauncherUnicraft/Client/");
			}
			
			System.out.println(path + nextToken.replaceAll("minecraft.jar", "unicraft.jar"));
			
			if (nextToken.indexOf("craft.jar") >= 0)
			{
				this.urlList[i] = new URL(path, nextToken.replaceAll("minecraft.jar", "unicraft.jar"));
			}
			
			else
			{
				this.urlList[i] = new URL(path, nextToken);
			}
			
			if (nextToken.indexOf("craft.jar") >= 0)
			{
				path = oldPath;
			}
		}
		
		String osName = System.getProperty("os.name");
		String nativeJar = null;
		
		if (osName.startsWith("Win"))
			nativeJar = "windows_natives.jar.lzma";
		else if (osName.startsWith("Linux"))
			nativeJar = "linux_natives.jar.lzma";
		else if (osName.startsWith("Mac"))
			nativeJar = "macosx_natives.jar.lzma";
		else if ((osName.startsWith("Solaris")) || (osName.startsWith("SunOS")))
			nativeJar = "solaris_natives.jar.lzma";
		else
			fatalErrorOccured("OS (" + osName + ") not supported", null);
		
		if (nativeJar == null)
			fatalErrorOccured("no lwjgl natives files found", null);
		else
		{
			nativeJar = trimExtensionByCapabilities(nativeJar);
			this.urlList[(jarCount - 1)] = new URL(path, nativeJar);
		}
	}
	
	public void run()
	{
		init();
		this.state = 3;
		
		this.percentage = 5;
		try
		{
			loadJarURLs();
			
			String path = (String)AccessController.doPrivileged(new PrivilegedExceptionAction<Object>()
			{
				public Object run() throws Exception
				{
					return Util.getWorkingDirectory() + File.separator + "bin" + File.separator;
				}
			});
			File dir = new File(path);
			
			if (!dir.exists())
			{
				dir.mkdirs();
			}
			
			if (this.latestVersion != null)
			{
				File versionFile = new File(dir, "version");
				
				boolean cacheAvailable = false;
				if ((versionFile.exists()) && ((this.latestVersion.equals("-1")) || (this.latestVersion.equals(readVersionFile(versionFile)))))
				{
					cacheAvailable = true;
					this.percentage = 90;
				}
				
				boolean updateunicraft = false;
				try
				{
					String version_unicraft = "";
					URL url_version = new URL("http://dl.dropbox.com/u/87115331/LauncherUnicraft/Client/version_unicraft.txt");
					try
					{
						BufferedReader in = new BufferedReader(new InputStreamReader(url_version.openStream()));
						version_unicraft = in.readLine();
					}
					catch (Exception e)
					{
						System.err.println(e);
					}
					File current_version_unicraft = new File(dir, "version_unicraft.txt");
					
					if (!current_version_unicraft.exists())
					{
						updateunicraft = true;
						try
						{
							BufferedWriter bw = new BufferedWriter(new FileWriter(current_version_unicraft));
							bw.append(version_unicraft);
							bw.close();
						}
						catch (IOException e)
						{
							System.out.println("Erreur");
						}
					}
					else
					{
						try
						{
							Scanner scanner = new Scanner(current_version_unicraft);
							while (scanner.hasNextLine())
							{
								String line = scanner.nextLine().trim();
								if (!version_unicraft.equals(line))
								{
									updateunicraft = true;
									try
									{
										BufferedWriter bw = new BufferedWriter(new FileWriter(current_version_unicraft));
										bw.append(version_unicraft);
										bw.close();
									}
									catch (IOException e)
									{
										System.out.println("Erreur");
									}
								}
							}
							scanner.close();
						}
						catch (IOException e)
						{
							System.out.println("Erreur" + e.getMessage());
						}
					}
				}
				catch (Exception localException1)
				{}
				
				if ((!cacheAvailable) || (updateunicraft) || (forceUpdate))
				{
					downloadJars(path);
					extractJars(path);
					extractNatives(path);
					
					if (this.latestVersion != null)
					{
						this.percentage = 90;
						writeVersionFile(versionFile, this.latestVersion);
					}
				}
			}
			
			updateClassPath(dir);
			this.state = 10;
		} 
		catch (AccessControlException ace)
		{
			fatalErrorOccured(ace.getMessage(), ace);
			this.certificateRefused = true;
		}
		catch (Exception e)
		{
			fatalErrorOccured(e.getMessage(), e);
		}
		finally
		{
			this.loaderThread = null;
		}
	}
	
	protected String readVersionFile(File file) throws Exception
	{
		DataInputStream dis = new DataInputStream(new FileInputStream(file));
		String version = dis.readUTF();
		dis.close();
		return version;
	}
	
	protected void writeVersionFile(File file, String version) throws Exception
	{
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
		dos.writeUTF(version);
		dos.close();
	}
	
	protected void updateClassPath(File dir) throws Exception
	{
		this.state = 6;
		
		this.percentage = 95;
		
		URL[] urls = new URL[this.urlList.length];
		
		for (int i = 0; i < this.urlList.length; i++)
		{
			urls[i] = new File(dir, getJarName(this.urlList[i])).toURI().toURL();
		}
		
		if (classLoader == null)
		{
			classLoader = new URLClassLoader(urls)
			{
				protected PermissionCollection getPermissions(CodeSource codesource) 
				{
					PermissionCollection perms = null;
					try
					{
						Method method = SecureClassLoader.class.getDeclaredMethod("getPermissions", new Class[] { CodeSource.class });
						method.setAccessible(true);
						perms = (PermissionCollection)method.invoke(getClass().getClassLoader(), new Object[] { codesource });

						String host = "www.minecraft.net";

						if ((host != null) && (host.length() > 0))
						{
							perms.add(new SocketPermission(host, "connect,accept"));
						}
						else
						{
							codesource.getLocation().getProtocol().equals("file");
						}
						
						perms.add(new FilePermission("<<ALL FILES>>", "read"));
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					
					return perms;
				}
			};
		}
		String path = dir.getAbsolutePath();
		if (!path.endsWith(File.separator))
		{
			path = path + File.separator;
		}
		unloadNatives(path);
		
		System.setProperty("org.lwjgl.librarypath", path + "natives");
		System.setProperty("net.java.games.input.librarypath", path + "natives");
		
		natives_loaded = true;
	}
	
	private void unloadNatives(String nativePath)
	{
		if (!natives_loaded)
		{
			return;
		}
		try
		{
			Field field = ClassLoader.class.getDeclaredField("loadedLibraryNames");
			field.setAccessible(true);
			Vector<?> libs = (Vector<?>)field.get(getClass().getClassLoader());
			
			String path = new File(nativePath).getCanonicalPath();

			for (int i = 0; i < libs.size(); i++)
			{
				String s = (String)libs.get(i);
				
				if (s.startsWith(path))
				{
					libs.remove(i);
					i--;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public Applet createApplet() throws ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		Class<?> appletClass = classLoader.loadClass("net.minecraft.client.MinecraftApplet");
		return (Applet)appletClass.newInstance();
	}
	
	protected void downloadJars(String path) throws Exception
	{
		this.state = 4;
		
		int[] fileSizes = new int[this.urlList.length];
		
		for (int i = 0; i < this.urlList.length; i++)
		{
			System.out.println(this.urlList[i]);
			URLConnection urlconnection = this.urlList[i].openConnection();
			urlconnection.setDefaultUseCaches(false);
			if ((urlconnection instanceof HttpURLConnection))
			{
				((HttpURLConnection)urlconnection).setRequestMethod("HEAD");
			}
			fileSizes[i] = urlconnection.getContentLength();
			this.totalSizeDownload += fileSizes[i];
		}
		
		int initialPercentage = this.percentage = 10;
		
		byte[] buffer = new byte[65536];
		for (int i = 0; i < this.urlList.length; i++)
		{
			int unsuccessfulAttempts = 0;
			int maxUnsuccessfulAttempts = 3;
			boolean downloadFile = true;
			
			while (downloadFile)
			{
				downloadFile = false;
				
				URLConnection urlconnection = this.urlList[i].openConnection();
				
				if ((urlconnection instanceof HttpURLConnection))
				{
					urlconnection.setRequestProperty("Cache-Control", "no-cache");
					urlconnection.connect();
				}
				
				String currentFile = getFileName(this.urlList[i]);
				InputStream inputstream = getJarInputStream(currentFile, urlconnection);
				FileOutputStream fos = new FileOutputStream(path + currentFile);
				
				long downloadStartTime = System.currentTimeMillis();
				int downloadedAmount = 0;
				int fileSize = 0;
				String downloadSpeedMessage = "";
				int bufferSize;
				while ((bufferSize = inputstream.read(buffer, 0, buffer.length)) != -1)
				{
					fos.write(buffer, 0, bufferSize);
					this.currentSizeDownload += bufferSize;
					fileSize += bufferSize;
					this.percentage = (initialPercentage + this.currentSizeDownload * 45 / this.totalSizeDownload);
					this.subtaskMessage = ("T�l�chargement de " + currentFile + " " + this.currentSizeDownload * 100 / this.totalSizeDownload + "%");

					downloadedAmount += bufferSize;
					long timeLapse = System.currentTimeMillis() - downloadStartTime;
					
					if (timeLapse >= 1000L)
					{
						float downloadSpeed = downloadedAmount / (float)timeLapse;
						
						downloadSpeed = (int)(downloadSpeed * 100.0F) / 100.0F;

						downloadSpeedMessage = " � " + downloadSpeed + " KB/sec";
						
						downloadedAmount = 0;
						
						downloadStartTime += 1000L;
					}
					
					this.subtaskMessage += downloadSpeedMessage;
				}
				
				inputstream.close();
				fos.close();
				
				if ((!(urlconnection instanceof HttpURLConnection)) || (fileSize == fileSizes[i]))
					continue;
				
				if (fileSizes[i] <= 0)
					continue;
				
				unsuccessfulAttempts++;
				
				if (unsuccessfulAttempts < maxUnsuccessfulAttempts)
				{
					downloadFile = true;
					this.currentSizeDownload -= fileSize;
				}
				else
				{
					throw new Exception("Impossible de t�l�charger " + currentFile);
				}
			}
		}
		this.subtaskMessage = "";
	}
	
	protected InputStream getJarInputStream(String currentFile, final URLConnection urlconnection)  throws Exception
	{
		final InputStream[] is = new InputStream[1];
		
		for (int j = 0; (j < 3) && (is[0] == null); j++)
		{
			Thread t = new Thread()
			{
				public void run()
				{
					try
					{
						is[0] = urlconnection.getInputStream();
					}
					catch (IOException localIOException)
					{}
				}
			};
			
			t.setName("JarInputStreamThread");
			t.start();
			
			int iterationCount = 0;
			while ((is[0] == null) && (iterationCount++ < 5))
			{
				try
				{
					t.join(1000L);
				}
				catch (InterruptedException localInterruptedException)
				{}
			}
			if (is[0] != null) continue;
			try
			{
				t.interrupt();
				t.join();
			}
			catch (InterruptedException localInterruptedException1)
			{}
		}
		
		if (is[0] == null)
		{
			if (currentFile.equals("minecraft.jar"))
			{
				throw new Exception("Impossible de t�l�charger " + currentFile);
			}
			throw new Exception("Impossible de t�l�charger " + currentFile);
		}
		
		return is[0];
	}
	
	protected void extractLZMA(String in, String out) throws Exception
	{
		File f = new File(in);
		FileInputStream fileInputHandle = new FileInputStream(f);
		
		Class<?> clazz = Class.forName("LZMA.LzmaInputStream");
		Constructor<?> constructor = clazz.getDeclaredConstructor(new Class[] { InputStream.class });
		InputStream inputHandle = (InputStream)constructor.newInstance(new Object[] { fileInputHandle });

		OutputStream outputHandle = new FileOutputStream(out);
		
		byte[] buffer = new byte[16384];
		
		int ret = inputHandle.read(buffer);
		while (ret >= 1)
		{
			outputHandle.write(buffer, 0, ret);
			ret = inputHandle.read(buffer);
		}
		
		inputHandle.close();
		outputHandle.close();
		
		outputHandle = null;
		inputHandle = null;
		
		f.delete();
	}
	
	protected void extractPack(String in, String out) throws Exception
	{
		File f = new File(in);
		FileOutputStream fostream = new FileOutputStream(out);
		JarOutputStream jostream = new JarOutputStream(fostream);
		
		Pack200.Unpacker unpacker = Pack200.newUnpacker();
		unpacker.unpack(f, jostream);
		jostream.close();
		
		f.delete();
	}
	
	protected void extractJars(String path) throws Exception
	{
		this.state = 5;
		
		float increment = 10.0F / this.urlList.length;
		
		for (int i = 0; i < this.urlList.length; i++)
		{
			this.percentage = (55 + (int)(increment * (i + 1)));
			String filename = getFileName(this.urlList[i]);
			
			if (filename.endsWith(".pack.lzma"))
			{
				this.subtaskMessage = ("Extracting: " + filename + " to " + filename.replaceAll(".lzma", ""));
				extractLZMA(path + filename, path + filename.replaceAll(".lzma", ""));

				this.subtaskMessage = ("Extracting: " + filename.replaceAll(".lzma", "") + " to " + filename.replaceAll(".pack.lzma", ""));
				extractPack(path + filename.replaceAll(".lzma", ""), path + filename.replaceAll(".pack.lzma", ""));
			}
			else if (filename.endsWith(".pack"))
			{
				this.subtaskMessage = ("Extracting: " + filename + " to " + filename.replace(".pack", ""));
				extractPack(path + filename, path + filename.replace(".pack", ""));
			}
			else if (filename.endsWith(".lzma"))
			{
				this.subtaskMessage = ("Extracting: " + filename + " to " + filename.replace(".lzma", ""));
				extractLZMA(path + filename, path + filename.replace(".lzma", ""));
			}
		}
	}
	
	protected void extractNatives(String path) throws Exception
	{
		this.state = 5;
		
		int initialPercentage = this.percentage;
		
		String nativeJar = getJarName(this.urlList[(this.urlList.length - 1)]);
		
		Certificate[] certificate = Launcher.class.getProtectionDomain().getCodeSource().getCertificates();
		
		if (certificate == null)
		{
			URL location = Launcher.class.getProtectionDomain().getCodeSource().getLocation();
			
			JarURLConnection jurl = (JarURLConnection)new URL("jar:" + location.toString() + "!/net/minecraft/Launcher.class").openConnection();
			jurl.setDefaultUseCaches(true);
			try
			{
				certificate = jurl.getCertificates();
			}
			catch (Exception localException)
			{}
		}
		File nativeFolder = new File(path + "natives");
		if (!nativeFolder.exists())
		{
			nativeFolder.mkdir();
		}
		
		JarFile jarFile = new JarFile(path + nativeJar, true);
		Enumeration<?> entities = jarFile.entries();
		
		this.totalSizeExtract = 0;
		
		while (entities.hasMoreElements())
		{
			JarEntry entry = (JarEntry)entities.nextElement();
			
			if ((entry.isDirectory()) || (entry.getName().indexOf('/') != -1))
			{
				continue;
			}
			this.totalSizeExtract = (int)(this.totalSizeExtract + entry.getSize());
		}
		
		this.currentSizeExtract = 0;
		
		entities = jarFile.entries();
		
		while (entities.hasMoreElements())
		{
			JarEntry entry = (JarEntry)entities.nextElement();
			
			if ((entry.isDirectory()) || (entry.getName().indexOf('/') != -1))
			{
				continue;
			}
			File f = new File(path + "natives" + File.separator + entry.getName());
			if ((f.exists()) && (!f.delete()))
			{
				continue;
			}
			
			InputStream in = jarFile.getInputStream(jarFile.getEntry(entry.getName()));
			OutputStream out = new FileOutputStream(path + "natives" + File.separator + entry.getName());

			byte[] buffer = new byte[65536];
			int bufferSize;
			while ((bufferSize = in.read(buffer, 0, buffer.length)) != -1)
			{
				out.write(buffer, 0, bufferSize);
				this.currentSizeExtract += bufferSize;
				
				this.percentage = (initialPercentage + this.currentSizeExtract * 20 / this.totalSizeExtract);
				this.subtaskMessage = ("Extracting: " + entry.getName() + " " + this.currentSizeExtract * 100 / this.totalSizeExtract + "%");
			}
			
			validateCertificateChain(certificate, entry.getCertificates());
			
			in.close();
			out.close();
		}
		this.subtaskMessage = "";
		
		jarFile.close();
		
		File f = new File(path + nativeJar);
		f.delete();
	}
	
	protected static void validateCertificateChain(Certificate[] ownCerts, Certificate[] native_certs) throws Exception
	{
		if (ownCerts == null)
			return;
		if (native_certs == null)
			throw new Exception("Unable to validate certificate chain. Native entry did not have a certificate chain at all");
		if (ownCerts.length != native_certs.length)
			throw new Exception("Unable to validate certificate chain. Chain differs in length [" + ownCerts.length + " vs " + native_certs.length + "]");
		
		for (int i = 0; i < ownCerts.length; i++)
			if (!ownCerts[i].equals(native_certs[i]))
				throw new Exception("Certificate mismatch: " + ownCerts[i] + " != " + native_certs[i]);
	}
	
	protected String getJarName(URL url)
	{
		String fileName = url.getFile();
		
		if (fileName.contains("?"))
		{
			fileName = fileName.substring(0, fileName.indexOf("?"));
		}
		if (fileName.endsWith(".pack.lzma"))
			fileName = fileName.replaceAll(".pack.lzma", "");
		else if (fileName.endsWith(".pack"))
			fileName = fileName.replaceAll(".pack", "");
		else if (fileName.endsWith(".lzma"))
		{
			fileName = fileName.replaceAll(".lzma", "");
		}
		
		return fileName.substring(fileName.lastIndexOf('/') + 1);
	}
	
	protected String getFileName(URL url)
	{
		String fileName = url.getFile();
		if (fileName.contains("?"))
		{
			fileName = fileName.substring(0, fileName.indexOf("?"));
		}
		return fileName.substring(fileName.lastIndexOf('/') + 1);
	}
	
	protected void fatalErrorOccured(String error, Exception e)
	{
		e.printStackTrace();
		this.fatalError = true;
		this.fatalErrorDescription = ("Fatal error occured (" + this.state + "): " + error);
		System.out.println(this.fatalErrorDescription);
		if (e != null)
			System.out.println(generateStacktrace(e));
	}

	public boolean canPlayOffline()
	{
		try
		{
			String path = (String)AccessController.doPrivileged(new PrivilegedExceptionAction<Object>()
			{
				public Object run() throws Exception
				{
					return Util.getWorkingDirectory() + File.separator + "bin" + File.separator;
				}
			});
			
			File dir = new File(path);
			if (!dir.exists())
				return false;
			
			dir = new File(dir, "version");
			if (!dir.exists())
				return false;
			
			if (dir.exists())
			{
				String version = readVersionFile(dir);
				if ((version != null) && (version.length() > 0))
					return true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return false;
	}
}