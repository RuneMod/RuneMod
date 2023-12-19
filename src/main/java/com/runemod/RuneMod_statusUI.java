package com.runemod;

import javax.swing.*;
import java.awt.*;

public class RuneMod_statusUI extends JPanel {


    public JLabel StatusHeading = new JLabel();
    public JLabel StatusDetail = new JLabel();
    JLabel iconHolder = new JLabel();

    public JDialog frame;
    public JProgressBar loadingBar;

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
        frame.dispose();
        frame.setVisible(false);
        frame.setTitle("staleWindow");
    }

    RuneMod_statusUI(Frame owner) {
        StatusHeading.setText("No Status");


        frame = createFrame("RuneModStatus", owner);


        final JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        frame.add(labelPanel);

        StatusHeading.setFont(new Font("Tahoma", Font.BOLD, 13));
        StatusDetail.setFont(new Font("Tahoma", Font.PLAIN, 13));

        labelPanel.add(StatusHeading);
        labelPanel.add(StatusDetail);


        // We suppose you have already set your JFrame
        labelPanel.add(iconHolder);
/*
        loadingBar = new JProgressBar( ) ;
        loadingBar.setValue( 0 ) ;
        loadingBar.setStringPainted( true ) ;
        labelPanel.add(loadingBar) ;*/



        labelPanel.setBackground (new Color (0, 0, 0, 0));
        labelPanel.setOpaque(false);
        frame.setBackground (new Color (0, 0, 0, 0));
        frame.setVisible(true);
        frame.toFront();

        SetStatusHeading("RuneMod: ");
        SetStatus_Detail("No status");
    }


    public void setProgressBar(int percentDone)
    {
        loadingBar.setValue(percentDone);
    }

    public void SetStatus_Detail(String statusText) {
        //loadingBar.setString(statusText +" - " + loadingBar.getValue() + "%");
        StatusDetail.setText(statusText);

        String statusText_caseless =  statusText.toLowerCase();

        if (statusText_caseless.contains("updating")) {
            StatusDetail.setForeground(Color.orange);
            iconHolder.setVisible(true);
            iconHolder.setIcon(new ImageIcon("loading_shrimps_small.gif"));
        }else {
            if (statusText_caseless.contains("ing")) {
                StatusDetail.setForeground(Color.yellow);
                iconHolder.setVisible(true);
                iconHolder.setIcon(new ImageIcon("loading_shrimps_small.gif"));
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



/*        if (!statusText.toLowerCase().contains("ready")) {
            SetStatusHeading("Runemod Loading...");
        } else {
            SetStatusHeading("Runemod Ready.");
        }*/
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
        dialog.getContentPane().add(Box.createRigidArea(new Dimension(500, 20)));
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
