package com.foodbridge.gui.components;

import com.foodbridge.gui.Theme;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Utility for creating pure AWT modal dialogs (replacing Swing JOptionPane).
 */
public class DialogHelper {

    private DialogHelper() {
    }

    /**
     * Displays a modal information dialog.
     */
    public static void showInfo(Frame parent, String title, String message) {
        showCustomDialog(parent, title, message, false, false);
    }

    /**
     * Displays a modal error dialog.
     */
    public static void showError(Frame parent, String title, String message) {
        showCustomDialog(parent, title, message, true, false);
    }

    /**
     * Displays a modal confirmation dialog and returns true if user clicked OK/Confirm.
     */
    public static boolean showConfirm(Frame parent, String title, String message) {
        return showCustomDialog(parent, title, message, false, true);
    }

    private static boolean showCustomDialog(Frame parent, String title, String message, boolean isError, boolean isConfirm) {
        final Dialog dialog = new Dialog(parent, title, true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setBackground(Theme.BG_LIGHT);

        // Header / icon label
        Panel topPanel = new Panel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        topPanel.setBackground(isError ? Theme.STATUS_EXPIRED : Theme.PRIMARY_DARK);
        Label headerLabel = new Label(isError ? "[!] " + title : "[*] " + title);
        headerLabel.setFont(Theme.FONT_SUBTITLE);
        headerLabel.setForeground(Theme.TEXT_LIGHT);
        topPanel.add(headerLabel);
        dialog.add(topPanel, BorderLayout.NORTH);

        // Body message panel with multi-line support
        String[] lines = message.split("\n");
        Panel msgPanel = new Panel(new GridLayout(Math.max(lines.length, 1), 1, 4, 4));
        msgPanel.setBackground(Theme.BG_LIGHT);
        for (String line : lines) {
            Label lbl = new Label(line);
            lbl.setFont(Theme.FONT_BODY);
            lbl.setForeground(Theme.TEXT_DARK);
            msgPanel.add(lbl);
        }

        Panel centerWrapper = new Panel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        centerWrapper.add(msgPanel);
        dialog.add(centerWrapper, BorderLayout.CENTER);

        // Action Buttons panel
        Panel buttonPanel = new Panel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
        buttonPanel.setBackground(Theme.BG_LIGHT);

        final boolean[] result = new boolean[]{false};

        if (isConfirm) {
            Button cancelBtn = new Button("Cancel");
            Theme.styleSecondaryButton(cancelBtn);
            cancelBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    result[0] = false;
                    dialog.dispose();
                }
            });
            buttonPanel.add(cancelBtn);
        }

        Button okBtn = new Button(isConfirm ? "Confirm" : "OK");
        if (isError) {
            Theme.styleDangerButton(okBtn);
        } else {
            Theme.stylePrimaryButton(okBtn);
        }
        okBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                result[0] = true;
                dialog.dispose();
            }
        });
        buttonPanel.add(okBtn);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dialog.dispose();
            }
        });

        // Set size & center over parent
        dialog.pack();
        Dimension size = dialog.getSize();
        int width = Math.max(380, size.width + 40);
        int height = Math.max(180, size.height + 20);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return result[0];
    }
}
