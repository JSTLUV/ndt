import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;

import java.awt.GridLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SnaplogFrame extends JFrame
{
    private JAnalyze mainWindow;
    private static final long serialVersionUID = 1L;
    private JTextField genplot = new JTextField("/usr/local/bin/genplot", 20);
    private JTextField xplot = new JTextField("/usr/local/bin/xplot", 20);
    private JTextField tcptrace = new JTextField("/usr/local/bin/tcptrace", 20);
    private JTextField snaplogs = new JTextField(20);
    private JTextField variables =
        new JTextField("CurCwnd,SampleRTT,SlowStart,MaxRTT,CurSsthresh,CongestionSignals", 40);

    public SnaplogFrame(JAnalyze mainWindow) {
        this.mainWindow = mainWindow;

        Container cp = getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        JPanel panel = new JPanel();
        JPanel tmpPanel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(new JLabel("Genplot:"));
        panel.add(genplot);
        tmpPanel.add(panel);
        cp.add(tmpPanel);
        panel = new JPanel();
        tmpPanel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(new JLabel("Xplot:"));
        panel.add(xplot);
        tmpPanel.add(panel);
        cp.add(tmpPanel);
        panel = new JPanel();
        tmpPanel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(new JLabel("Tcptrace:"));
        panel.add(tcptrace);
        tmpPanel.add(panel);
        cp.add(tmpPanel);
        panel = new JPanel();
        tmpPanel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(new JLabel("Snaplog directory:"));
        snaplogs.setText(mainWindow.getProperties().getProperty("snaplogs", ""));
        snaplogs.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                try {
                    SnaplogFrame.this.mainWindow.getProperties().setProperty("snaplogs",
                        e.getDocument().getText(0, e.getDocument().getLength()));
                }
                catch (BadLocationException exc) {
                    // do nothing
                }
            }
            public void insertUpdate(DocumentEvent e) {
                try {
                    SnaplogFrame.this.mainWindow.getProperties().setProperty("snaplogs",
                        e.getDocument().getText(0, e.getDocument().getLength()));
                }
                catch (BadLocationException exc) {
                    // do nothing
                }
            }
            public void removeUpdate(DocumentEvent e) {
                try {
                    SnaplogFrame.this.mainWindow.getProperties().setProperty("snaplogs",
                            e.getDocument().getText(0, e.getDocument().getLength()));
                }
                catch (BadLocationException exc) {
                    // do nothing
                }
            }
        });
        panel.add(snaplogs);
        tmpPanel.add(panel);
        cp.add(tmpPanel);
        panel = new JPanel();
        tmpPanel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(new JLabel("Variables:"));
        panel.add(variables);
        tmpPanel.add(panel);
        cp.add(tmpPanel);

        setSize(515, 180);
    }

    public String getGenplot() {
        return genplot.getText();
    }

    public String getXplot() {
        return xplot.getText();
    }

    public String getTcptrace() {
        return tcptrace.getText();
    }

    public String getSnaplogs() {
        return snaplogs.getText();
    }

    public String getVariables() {
        return variables.getText();
    }
}
