
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Actualizer implements ActionListener {

    private Map<Integer, Standard> GOSTs = new HashMap<>();
    private JEditorPane textField;

    public static void main(String[] args) {
        Actualizer actualizer = new Actualizer();
        actualizer.init();
    }

    private void init() {
        JFrame frame = new JFrame();
        //frame.setLayout(new GridBagLayout());

        textField = new JEditorPane();
        //textField.addActionListener(this);
        JScrollPane scroll = new JScrollPane(textField);
        frame.add(scroll, BorderLayout.CENTER);

        JButton button = new JButton("Проверить");
        button.setBounds(frame.getX() / 2, 0, frame.getWidth() / 4, frame.getHeight() / 15);
        button.addActionListener(this);
        frame.add(button, BorderLayout.NORTH);

        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(e + " action performed");
        index();
        checkStds();

    }

    private void checkStds() {
        JFrame progressFrame = new JFrame("Прогресс");
        progressFrame.setSize(500, 80);
        JProgressBar pBar = new JProgressBar(0, GOSTs.size());
        pBar.setStringPainted(true);
        pBar.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        progressFrame.add(pBar);
//
//        JLabel currentNum = new JLabel();
//        //progressFrame.add(currentNum);
        progressFrame.setResizable(false);
        progressFrame.setVisible(true);

        ProgressWorker pt = new ProgressWorker(GOSTs);
        pt.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String name = evt.getPropertyName();
                if (name.equals("progress")) {
                    int progress = (int) evt.getNewValue();
                    pBar.setValue(progress);
                    progressFrame.repaint();
                } else if (name.equals("state")) {
                    SwingWorker.StateValue state = (SwingWorker.StateValue) evt.getNewValue();
                    switch (state) {
                        case DONE:
                            highlight();
                            progressFrame.setVisible(false);
                            break;
                    }
                }
            }
        });
        pt.execute();
    }

    private void highlight() {
        removeHighlights();
        Highlighter hl = textField.getHighlighter();

        Highlighter.HighlightPainter red = new DefaultHighlighter.DefaultHighlightPainter(Color.RED);
        Highlighter.HighlightPainter yellow = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
        Highlighter.HighlightPainter green = new DefaultHighlighter.DefaultHighlightPainter(Color.GREEN);
        Highlighter.HighlightPainter orange = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE);

        for (Map.Entry<Integer, Standard> e : GOSTs.entrySet()) {
            try {
                Standard std = e.getValue();
                int start = e.getKey();
                int end = start + std.number.length();
                switch (std.status) {
                    case ERROR:
                    case OBSOLETE: hl.addHighlight(start, end, red); break;
                    case NOT_FOUND: hl.addHighlight(start, end, orange); break;
                    case UNKNOWN: hl.addHighlight(start, end, yellow); break;
                    case OK: hl.addHighlight(start, end, green); break;
                }
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            } catch (NullPointerException e1) {
                e1.printStackTrace();
            }
        }
    }
    private void removeHighlights() {
        Highlighter hilite = textField.getHighlighter();
        Highlighter.Highlight[] hilites = hilite.getHighlights();

        for (int i = 0; i < hilites.length; i++) {
            hilite.removeHighlight(hilites[i]);
        }
    }

    private void index() {
        String ls = System.lineSeparator();
        Pattern stdPattern = Pattern.compile("(?:ГОСТ (?:Р )?(?:МЭК |ИСО |ИСО\\/МЭК |IEC )?[0-9]+(?:\\.[0-9]+){0,2}(?:-[0-9]{1,2})?-[0-9]{2,4})|" +
                                                "(?:(?:СП |СНиП )[0-9]+(?:\\.|-)[0-9]+(?:\\.|-)[0-9]+(?:-[0-9]+)?\\*?)|" +
                                                "(?:ВСН [0-9]+-[0-9]{2})|" +
                                                "(?:МДС [0-9]+-[0-9]+.[0-9]+)|" +
                                                "(?:(?:ОДН|ОДМ) [0-9А-Я]+(?:\\.|-)[0-9]+(?:\\.|\\/)[0-9]+-(?:[0-9]{4}|ис))|" +
                                                "(?:[^Г]ОСТ [0-9]+\\.[0-9]+-[0-9]+)|" +
                                                "(?:РД [0-9]{2}-[0-9]{2}-[0-9]{4})|" +
                                                ls);
        Matcher matcher = stdPattern.matcher(textField.getText());
        GOSTs = new HashMap<>();
        int line = 0;
        while (matcher.find()) {
            if (matcher.group().equals(ls)) {
                line++;
            } else {
                GOSTs.put(matcher.start() - line, new Standard(matcher.group()));
            }
        }
    }
}
