package net.minecraft;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

public class OptionsPanel extends JDialog
{
	private static final long serialVersionUID = 1L;
	private JCheckBox forceButton;
	private JRadioButton stableReleaseRButton;
	private JRadioButton betaReleaseRButton;

	public OptionsPanel(Frame parent)
	{
		super(parent);
		
		setModal(true);

		JPanel panel = new JPanel(new BorderLayout());
		JLabel label = new JLabel("Options", 0);
		label.setBorder(new EmptyBorder(0, 0, 16, 0));
		label.setFont(new Font("Default", 1, 16));
		panel.add(label, "North");

		JPanel optionsPanel = new JPanel(new BorderLayout());
		JPanel labelPanel = new JPanel(new GridLayout(0, 1));
		JPanel fieldPanel = new JPanel(new GridLayout(0, 1));
		optionsPanel.add(labelPanel, "West");
		optionsPanel.add(fieldPanel, "Center");
		
		// Bouton Forcer la mise à jour
		forceButton = new JCheckBox("Forcer la Mise à jour!");
		forceButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				if(forceButton.isSelected())
				{
					GameUpdater.forceUpdate = true;
				}
				else
				{
					GameUpdater.forceUpdate = false;
				}
			}
		});
		forceButton.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		labelPanel.add(new JLabel("Forcer la mise à jour: ", 4));
		fieldPanel.add(forceButton);
		
		// Bouton à sélectionner pour choisir le type de version
		ButtonGroup releaseTypeBG = new ButtonGroup();
		stableReleaseRButton = new JRadioButton("Stable");
		betaReleaseRButton = new JRadioButton("Beta");
		JLabel getBetaLabel =  new JLabel("Choisir votre type de version : ");
		JPanel releaseTypePanel = new JPanel(new GridLayout(1, 0));
		stableReleaseRButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				if(stableReleaseRButton.isSelected())
				{
					GameUpdater.setBeta(false);
				}
			}
		});
		betaReleaseRButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				if(betaReleaseRButton.isSelected())
				{
					GameUpdater.setBeta(true);
				}
			}
		});
		if(GameUpdater.isBeta())
		{
			betaReleaseRButton.setSelected(true);
		}
		else
		{
			stableReleaseRButton.setSelected(true);
		}
		releaseTypeBG.add(stableReleaseRButton);
		releaseTypeBG.add(betaReleaseRButton);
		releaseTypePanel.add(stableReleaseRButton);
		releaseTypePanel.add(betaReleaseRButton);
		stableReleaseRButton.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		betaReleaseRButton.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		labelPanel.add(getBetaLabel);
		fieldPanel.add(releaseTypePanel);
		
		// Lien vers l'emplacement du dossier .unicraft
		labelPanel.add(new JLabel("Emplacement de Unicraft: ", 4));
		TransparentLabel dirLink = new TransparentLabel(Util.getWorkingDirectory().toString())
		{
			private static final long serialVersionUID = 0L;
			
			public void paint(Graphics g)
			{
				super.paint(g);
				
				int x = 0;
				int y = 0;
				
				FontMetrics fm = g.getFontMetrics();
				int width = fm.stringWidth(getText());
				int height = fm.getHeight();
  
				if (getAlignmentX() == 2.0F)
					x = 0;
				else if (getAlignmentX() == 0.0F)
					x = getBounds().width / 2 - width / 2;
				else if (getAlignmentX() == 4.0F)
					x = getBounds().width - width;
				y = getBounds().height / 2 + height / 2 - 1;
				
				g.drawLine(x + 2, y, x + width - 2, y);
			}

			public void update(Graphics g)
			{
				paint(g);
			}
		};
  
		dirLink.setCursor(Cursor.getPredefinedCursor(12));
		dirLink.addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent arg0)
			{
				try
				{
					Util.openLink(new URL("file://" + Util.getWorkingDirectory().getAbsolutePath()).toURI());
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
		dirLink.setForeground(new Color(2105599));

		fieldPanel.add(dirLink);

		panel.add(optionsPanel, "Center");

		JPanel buttonsPanel = new JPanel(new BorderLayout());
		buttonsPanel.add(new JPanel(), "Center");
		JButton doneButton = new JButton("Terminé");
		doneButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				OptionsPanel.this.setVisible(false);
			}
		});
		buttonsPanel.add(doneButton, "East");
		buttonsPanel.setBorder(new EmptyBorder(16, 0, 0, 0));

		panel.add(buttonsPanel, "South");
  
		add(panel);
		panel.setBorder(new EmptyBorder(16, 24, 24, 24));
		pack();
		setLocationRelativeTo(parent);
	}
}