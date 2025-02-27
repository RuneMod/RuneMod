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

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.FontManager;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class RuneMod_LoadingScreen extends JPanel
{

	RuneModPlugin runeModPlugin = null;
	public JLabel StatusHeading = new JLabel();
	public JLabel StatusDetail = new JLabel();
	JLabel iconHolder = new JLabel();
	public JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

	public JDialog frame;

	public void close()
	{
		log.debug("Closing RuneMod loading screen");
		if (frame != null)
		{
			frame.dispose();
			frame.setVisible(false);
			frame.setTitle("staleWindow");
		}
	}

	RuneMod_LoadingScreen(Frame owner, RuneModPlugin plugin)
	{
		log.debug("Creating RuneMod loading screen");
		runeModPlugin = plugin;

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

		labelPanel.add(StatusHeading);
		labelPanel.add(StatusDetail);

		labelPanel.add(iconHolder);

		labelPanel.setBackground(new Color(0, 0, 0, 0));
		labelPanel.setOpaque(false);

		SetStatus_HeadingText("RuneMod: ");
		SetStatus_DetailText(".", false);
	}

	public void SetStatus_DetailText(String statusText, boolean print)
	{
		if (statusText.length() > 100) { return; }
		StatusDetail.setText(statusText);

		String statusText_caseless = statusText.toLowerCase();


		if (statusText_caseless.contains("updating"))
		{
			StatusDetail.setForeground(Color.orange);
			iconHolder.setVisible(true);
			iconHolder.setIcon(new ImageIcon(getClass().getResource("/loading_shrimps_small.gif")));
		}
		else
		{
			if (statusText_caseless.contains("ing") || statusText_caseless.contains("logged"))
			{
				StatusDetail.setForeground(Color.yellow);
				iconHolder.setVisible(true);
				iconHolder.setIcon(new ImageIcon(getClass().getResource("/loading_shrimps_small.gif")));
			}
			else
			{
				if (statusText_caseless.contains("fail") || statusText_caseless.contains("not ") || statusText_caseless.contains("cant"))
				{
					StatusDetail.setForeground(Color.red);
					iconHolder.setVisible(false);
				}
				else
				{
					StatusDetail.setForeground(Color.green);
					iconHolder.setVisible(false);
				}
			}
		}

		if (statusText_caseless.contains("you must"))
		{
			StatusDetail.setForeground(Color.red);
			iconHolder.setVisible(false);
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
	}

	public void SetStatus_HeadingText(String statusText)
	{
		StatusHeading.setText(statusText);
	}
}
