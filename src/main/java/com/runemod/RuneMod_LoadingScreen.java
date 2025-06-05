/*
 * Copyright (c) 2025, RuneFace <RuneFace@proton.me>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.runemod;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.FontManager;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class RuneMod_LoadingScreen extends JPanel
{

	public JLabel StatusHeading = new JLabel();
	public JLabel StatusDetail = new JLabel();

	boolean showProgressIndicator = false;
	//public JDialog frame;
	RuneModPlugin runeModPlugin = null;

	RuneMod_LoadingScreen(Frame owner, RuneModPlugin plugin)
	{
		log.debug("Creating RuneMod loading screen");
		runeModPlugin = plugin;

		//this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		this.setLayout (new BorderLayout()) ;
		JTextPane pane = new JTextPane();
		pane.setBackground(this.getBackground());
// or try making it opaque
		pane.setText("long text here");

		InputStream is = FontManager.class.getResourceAsStream("runescape.ttf");

		Font TITLEFONT = null;

		try
		{
			TITLEFONT = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.PLAIN, 16f);
		}
		catch (FontFormatException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		StatusDetail.setFont(TITLEFONT);
		StatusHeading.setFont(TITLEFONT);

/*		JLabel iconHolder = new JLabel();
		iconHolder.setOpaque(true); // Test value
		iconHolder.setBackground(Color.BLACK); // Just to test, not needed in final
		iconHolder.setIcon(new ImageIcon(getClass().getResource("/loading_shrimps_small.gif")));

		this.add(progressIndicator);*/
		this.add(StatusHeading);
		this.add(StatusDetail);

		new Thread(new Runnable() {
			@SneakyThrows
			public void run(){
				while (true) {
					if(StatusDetail != null) {
						//if(showProgressIndicator = true)
						//{
							StatusDetail.setText(StatusDetail.getText()+".");
						//}
						Thread.sleep(1000);
					} else {
						break;
					}
				}
			}
		}).start();

		this.setBackground(Color.BLACK);
		this.setOpaque(true);
		this.setBackground(Color.BLACK);
		this.setOpaque(true);

		SetStatus_HeadingText("RuneMod: ");
		SetStatus_DetailText("...", false);
	}

	public void close()
	{
		log.debug("Closing RuneMod loading screen");
		// Remove and nullify the components
		if (StatusHeading != null)
		{
			this.remove(StatusHeading);
			StatusHeading = null;
		}

		if (StatusDetail != null)
		{
			this.remove(StatusDetail);
			StatusDetail = null;
		}

/*		if (iconHolder != null)
		{
			this.remove(iconHolder);
			iconHolder = null;
		}*/

		// Dispose of the frame if it exists
/*		if (frame != null)
		{
			frame.setVisible(false);
			frame.dispose();
			frame = null;
		}*/

		// Nullify the plugin reference
		runeModPlugin = null;

		// Optionally, remove this panel from its parent if needed
		if (this.getParent() != null)
		{
			this.getParent().remove(this);
		}

		// Optionally revalidate and repaint the parent container
/*		this.revalidate();
		this.repaint();
		StatusDetail.repaint();
		StatusHeading.repaint();*/
	}

	public void SetStatus_DetailText(String statusText, boolean print)
	{
		if (statusText.length() > 100)
		{
			return;
		}

		if (statusText.length() < 1)
		{
			return;
		}

		SwingUtilities.invokeLater(() -> {
			StatusDetail.setText(statusText);

			String statusText_caseless = statusText.toLowerCase();


			if (statusText_caseless.contains("updating"))
			{
				StatusDetail.setForeground(Color.orange);
				showProgressIndicator = true;
				//iconHolder.setVisible(true);
				//iconHolder.setIcon(new ImageIcon(getClass().getResource("/loading_shrimps_small.gif")));
			}
			else
			{
				if (statusText_caseless.contains("ing") || statusText_caseless.contains("logged"))
				{
					StatusDetail.setForeground(Color.yellow);
					showProgressIndicator = true;
					//iconHolder.setVisible(true);
				}
				else
				{
					if (statusText_caseless.contains("fail") || statusText_caseless.contains("not ") || statusText_caseless.contains("cant"))
					{
						StatusDetail.setForeground(Color.red);
						showProgressIndicator = false;
						//iconHolder.setVisible(false);
					}
					else
					{
						StatusDetail.setForeground(Color.green);
						showProgressIndicator = false;
						//iconHolder.setVisible(false);
					}
				}
			}

			if (statusText_caseless.contains("you must"))
			{
				StatusDetail.setForeground(Color.red);
				showProgressIndicator = false;
				//iconHolder.setVisible(false);
			}

			//log.debug("status:"+statusText);

			if (statusText.equalsIgnoreCase("ready"))
			{
				if (!RuneModPlugin.unrealIsReady)
				{
					//RuneModPlugin.toggleRuneModLoadingScreen(false);
					RuneModPlugin.unrealIsReady = true;
					runeModPlugin.appSettings = runeModPlugin.loadAppSettings();
					runeModPlugin.resendGameStateChanged();
				}
			}

			if (print)
			{
				RuneModPlugin.log_Timed_Heavy("..." + statusText);
			}

/*			StatusDetail.revalidate();
			StatusDetail.repaint();
			this.revalidate();
			this.repaint();*/
		});
	}

	public void SetStatus_HeadingText(String statusText)
	{
		if(true){return;}
		if (statusText.length() > 100)
		{
			return;
		}

		if (statusText.length() < 1)
		{
			return;
		}

		SwingUtilities.invokeLater(() -> {
			StatusHeading.setText(statusText);

/*			this.revalidate();
			this.repaint();
			StatusDetail.repaint();
			StatusHeading.repaint();*/
		});
	}
}
