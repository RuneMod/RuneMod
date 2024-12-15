package com.runemod;

import com.runemod.cache.fs.flat.FlatStorage;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class RuneMod_statusUI extends JPanel {


    public JLabel StatusHeading = new JLabel();
    public JLabel StatusDetail = new JLabel();
    JLabel iconHolder = new JLabel();
    public JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    public JDialog frame;

/*    @Override
    public void run()
    {
        if (dotdotdot.getText().length() > 4) {
            dotdotdot.setText("");
        } else {
            dotdotdot.setText(dotdotdot.getText()+".");
        }
    }*/

    public void close() {
        if(frame!=null) {
            frame.dispose();
            frame.setVisible(false);
            frame.setTitle("staleWindow");
        }
    }

    RuneMod_statusUI(Frame owner) {
        StatusHeading.setText("");


/*        frame = createFrame("RuneModStatus", owner);

        frame.add(labelPanel);*/
        //frame.setMaximumSize(new Dimension(500,20));
        InputStream is = FontManager.class.getResourceAsStream("runescape.ttf");

        Font TITLEFONT = null;

        try {
            TITLEFONT = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.PLAIN, 16f);
        } catch (FontFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        StatusDetail.setFont(TITLEFONT);
        StatusHeading.setFont(TITLEFONT);

        labelPanel.add(StatusHeading);
        labelPanel.add(StatusDetail);

        labelPanel.add(iconHolder);
/*
        loadingBar = new JProgressBar( ) ;
        loadingBar.setValue( 0 ) ;
        loadingBar.setStringPainted( true ) ;
        labelPanel.add(loadingBar) ;*/



        labelPanel.setBackground (new Color (0, 0, 0, 0));
        labelPanel.setOpaque(false);
/*        frame.setBackground (new Color (0, 0, 0, 0));
        frame.setVisible(true);
        frame.toFront();*/

        SetStatusHeading("RuneMod: ");
        SetStatus_Detail("", false);
    }

    public void SetStatus_Detail(String statusText, boolean print) {
        if(statusText.length()>100) { return; };
        //loadingBar.setString(statusText +" - " + loadingBar.getValue() + "%");
        StatusDetail.setText(statusText);

        String statusText_caseless =  statusText.toLowerCase();

        //Image iconPath = ImageUtil.loadImageResource(RuneModPlugin.class, "/loading_shrimps_small.gif");
/*        String iconPath = "loading_shrimps_small.gif";

        String baseName = RuneModPlugin.class.getPackageName();
        if (baseName != null && !baseName.isEmpty()) {
            iconPath = baseName.replace('.', '/') + "/" + iconPath;
        }*/

        //String iconPath = RuneModPlugin.class.getResource("loading_shrimps_small.gif").getPath();

        //System.out.println(iconPath);

        //String iconPath = RuneModPlugin.class.getResource("loading_shrimps_small.gif").getPath();
        //String icon = RuneModPlugin.class.;

        if (statusText_caseless.contains("updating")) {
            StatusDetail.setForeground(Color.orange);
            iconHolder.setVisible(true);
            iconHolder.setIcon(new ImageIcon(getClass().getResource("/loading_shrimps_small.gif")));
        }else {
            if (statusText_caseless.contains("ing")||statusText_caseless.contains("logged")) {
                StatusDetail.setForeground(Color.yellow);
                iconHolder.setVisible(true);
                iconHolder.setIcon(new ImageIcon(getClass().getResource("/loading_shrimps_small.gif")));
            }else {
                if(statusText_caseless.contains("fail")||statusText_caseless.contains("not ")||statusText_caseless.contains("cant")) {
                    StatusDetail.setForeground(Color.red);
                    iconHolder.setVisible(false);
                } else {
                    StatusDetail.setForeground(Color.green);
                    iconHolder.setVisible(false);
                }
            }
        }

        if(statusText_caseless.contains("you must")) {
            StatusDetail.setForeground(Color.red);
            iconHolder.setVisible(false);
        }

        //System.out.println("status:"+statusText);

        if (statusText.equalsIgnoreCase("ready")) {
            if(!RuneModPlugin.unrealIsReady) {
                RuneModPlugin.toggleRuneModLoadingScreen(false);
                RuneModPlugin.unrealIsReady = true;
            }
        }

        if(print) {
            System.out.println("..." + statusText);
        }
    }

    public void SetStatusHeading(String statusText) {
        StatusHeading.setText(statusText);
    }

    public static JDialog  createFrame(String title, Frame owner) {
        JDialog dialog = new JDialog(owner, title, true);
        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        dialog.setUndecorated(true);
        // Using rigid area just to give the dialog size, but you
        // could put any complex GUI in a JPanel in here
        dialog.getContentPane().add(Box.createRigidArea(new Dimension(800, 20)));
        dialog.pack();
        dialog.setAutoRequestFocus(false);
        dialog.setFocusable(false);
        dialog.setVisible(true);
        //dialog.setLocationRelativeTo(owner);
        return dialog;
    }

    public static void main(String args[]) {
        //new RuneMod_statusUI();
    }
}
