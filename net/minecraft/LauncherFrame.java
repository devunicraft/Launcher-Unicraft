package net.minecraft;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class LauncherFrame extends Frame
{
	public static final int VERSION = 13;
	private static final long serialVersionUID = 1L;
	public Map<String, String> customParameters = new HashMap<String, String>();
	public Launcher launcher;
	public LoginForm loginForm;

	public LauncherFrame()
	{
		super("Unicraft");
		
		setBackground(Color.BLACK);
		this.loginForm = new LoginForm(this);
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(this.loginForm, "Center");
		
		p.setPreferredSize(new Dimension(854, 480));
		
		setLayout(new BorderLayout());
		add(p, "Center");
		
		pack();
		setLocationRelativeTo(null);
		try
		{
			setIconImage(ImageIO.read(LauncherFrame.class.getResource("favicon.png")));
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent arg0)
			{
				new Thread()
				{
					public void run()
					{
						try
						{
							Thread.sleep(30000L);
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
						System.out.println("FORCING EXIT!");
						System.exit(0);
					}
				}.start();
				
				if (LauncherFrame.this.launcher != null)
				{
					LauncherFrame.this.launcher.stop();
					LauncherFrame.this.launcher.destroy();
				}
				System.exit(0);
			}
		} );
	}
	
	public void playCached(String userName)
	{
		try
		{
			if ((userName == null) || (userName.length() <= 0))
			{
				userName = "Player";
			}
			this.launcher = new Launcher();
			this.launcher.customParameters.putAll(this.customParameters);
			this.launcher.customParameters.put("userName", userName);
			this.launcher.init();
			removeAll();
			add(this.launcher, "Center");
			validate();
			this.launcher.start();
			this.loginForm = null;
			setTitle("Unicraft");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			showError(e.toString());
		}
	}

	private void showError(String string)
	{}
	
	public String getFakeResult(String userName)
	{
		return MinecraftUtil.getFakeLatestVersion() + ":35b9fd01865fda9d70b157e244cf801c:" + userName + ":12345:";
	}
	
	public void login(String userName)
	{
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
	
	public boolean canPlayOffline(String userName)
	{
		Launcher launcher = new Launcher();
		launcher.customParameters.putAll(this.customParameters);
		launcher.init(userName, null, null, null);
		return launcher.canPlayOffline();
	}
	
	public static void main(String[] args)
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception localException)
		{}
		
		LauncherFrame launcherFrame = new LauncherFrame();
		launcherFrame.setVisible(true);
		launcherFrame.customParameters.put("stand-alone", "true");
		
		if (args.length >= 3)
		{
			String ip = args[2];
			String port = "25565";
			if (ip.contains(":"))
			{
				String[] parts = ip.split(":");
				ip = parts[0];
				port = parts[1];
			}
			launcherFrame.customParameters.put("server", ip);
			launcherFrame.customParameters.put("port", port);
		}
		if (args.length >= 1)
		{
			launcherFrame.loginForm.userName.setText(args[0]);
			if (args.length >= 2)
				launcherFrame.loginForm.doLogin();
		}
	}
}