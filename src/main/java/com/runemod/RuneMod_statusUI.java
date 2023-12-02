package com.runemod;

import javax.swing.*;
import java.awt.*;

public class RuneMod_statusUI implements Runnable {


    public JLabel StatusHeading = new JLabel();
    public JLabel StatusDetail = new JLabel();
    public JDialog frame;
    public JProgressBar loadingBar;

    @Override
    public void run()
    {
    }

    public void close() {
        frame.dispose();
        frame.setVisible(false);
        frame.setTitle("staleWindow");
    }

    RuneMod_statusUI() {
        StatusHeading.setText("No Status");


        frame = createFrame("RuneModStatus");


        final JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        frame.add(labelPanel);

        StatusHeading.setFont(new Font("Tahoma", Font.BOLD, 13));
        StatusDetail.setFont(new Font("Tahoma", Font.PLAIN, 13));

        labelPanel.add(StatusHeading);
        labelPanel.add(StatusDetail);


/*
        loadingBar = new JProgressBar( ) ;
        loadingBar.setValue( 0 ) ;
        loadingBar.setStringPainted( true ) ;
        labelPanel.add(loadingBar) ;*/



        labelPanel.setBackground (new Color (0, 0, 0, 0));
        labelPanel.setOpaque(false);
        frame.setBackground (new Color (0, 0, 0, 0));
        frame.setVisible(false);
        frame.toFront();

        SetStatusHeading("RuneMod Status: ");
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
        }else {
            if (statusText_caseless.contains("ing")) {
                StatusDetail.setForeground(Color.yellow);
            }else {
                if(statusText_caseless.contains("fail")||statusText_caseless.contains("not ")||statusText_caseless.contains("cant")) {
                    StatusDetail.setForeground(Color.red);
                } else {
                    StatusDetail.setForeground(Color.green);
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

    public static JDialog  createFrame(String title) {
        JDialog dialog = new JDialog((JFrame)null, title, true);
        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        dialog.setUndecorated(true);
        // Using rigid area just to give the dialog size, but you
        // could put any complex GUI in a JPanel in here
        dialog.getContentPane().add(Box.createRigidArea(new Dimension(500, 20)));
        dialog.pack();
        dialog.setAutoRequestFocus(false);
        dialog.setVisible(true);
        return dialog;
    }

    public static void main(String args[]) {
        new RuneMod_statusUI();
    }
}
