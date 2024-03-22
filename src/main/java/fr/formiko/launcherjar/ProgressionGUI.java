package fr.formiko.launcherjar;

import fr.formiko.usual.Progression;
import fr.formiko.usual.fichier;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * {@summary Simple GUI view that are update as a progression for downloading files.}<br>
 */
class ProgressionGUI implements Progression {

    private JLabel messageLabel;
    private JProgressBar progressBar;
    private JFrame frame;
    private JPanel panel;

    /**
     * {@summary Create a simple JFrame with a label &#38; a progress bar.}<br>
     */
    public ProgressionGUI() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                frame = new JFrame("Downloading");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                panel = new JPanel(new GridLayout(2, 1));
                frame.setContentPane(panel);

                messageLabel = new JLabel("");
                messageLabel.setPreferredSize(new Dimension(500, 20));
                frame.getContentPane().add(messageLabel);

                progressBar = new JProgressBar();
                progressBar.setMaximum(100);
                progressBar.setValue(0);
                progressBar.setPreferredSize(new Dimension(500, 20));
                progressBar.setForeground(Color.GREEN);
                frame.getContentPane().add(progressBar);

                frame.pack();
                frame.setAlwaysOnTop(true);
                frame.toFront();
                frame.requestFocus();
                frame.setAlwaysOnTop(false);
                frame.setLocationRelativeTo(null);

                frame.setVisible(true);
            }
        });
    }
    @Override
    public void iniLauncher() { fichier.setProgression(this); }
    @Override
    public void setDownloadingMessage(String message) { messageLabel.setText(message); }
    /**
     * {@summary update progressBar & close frame if download is over.}<br>
     */
    @Override
    public void setDownloadingValue(int value) {
        progressBar.setValue(value);
        if (value > 99) {
            frame.setVisible(false);
            frame.dispose();
        }
    }
}