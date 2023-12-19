import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main1 extends JFrame implements ActionListener {

    private JLabel fromDateLabel;
    private JLabel toDateLabel;
    private JSpinner fromDateSpinner;
    private JSpinner toDateSpinner;
    private JLabel topicsLabel;
    private JComboBox<String> topicsComboBox;
    private JButton filterButton;
    private JTable topicsTable;
    private DefaultTableModel topicsTableModel;
    private List<Topic> allTopics;

    public Main1() throws IOException, InterruptedException {
        super("Filtered Topics App");

        JPanel firstScreenPanel = new JPanel(new GridLayout(3, 2));

        fromDateLabel = new JLabel("From Date:");
        firstScreenPanel.add(fromDateLabel);

        fromDateSpinner = new JSpinner(new SpinnerDateModel());
        fromDateSpinner.setEditor(new JSpinner.DateEditor(fromDateSpinner, "yyyy-MM-dd"));
        firstScreenPanel.add(fromDateSpinner);

        toDateLabel = new JLabel("To Date:");
        firstScreenPanel.add(toDateLabel);

        toDateSpinner = new JSpinner(new SpinnerDateModel());
        toDateSpinner.setEditor(new JSpinner.DateEditor(toDateSpinner, "yyyy-MM-dd"));
        firstScreenPanel.add(toDateSpinner);

        topicsLabel = new JLabel("Topics:");
        firstScreenPanel.add(topicsLabel);
        allTopics =  Utils.getTopics(null, null);

        String[] topicNames = allTopics.stream()
                .map(t->{

                    String[] tt = t.getName().split(" ");
                    t.setName(tt[0]);
                    return tt[0];
                }).distinct().toArray(String[]::new);
        topicsComboBox = new JComboBox<>(topicNames);
        firstScreenPanel.add(topicsComboBox);

        filterButton = new JButton("Filter");
        filterButton.addActionListener(this);
        firstScreenPanel.add(filterButton);

        topicsTableModel = new DefaultTableModel(new String[]{"Topic Name", "Date"}, 0);
        topicsTable = new JTable(topicsTableModel);
        topicsTable.setFillsViewportHeight(true);

        JScrollPane topicsScrollPane = new JScrollPane(topicsTable);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Filter Topics", firstScreenPanel);
        tabbedPane.addTab("Filtered Topics", topicsScrollPane);

        getContentPane().add(tabbedPane);



        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == filterButton) {
            Date fromDate = (Date) fromDateSpinner.getValue();
            Date toDate = (Date) toDateSpinner.getValue();
            String selectedTopic = (String) topicsComboBox.getSelectedItem();
            SimpleDateFormat dateFormat = new SimpleDateFormat();
            dateFormat.applyPattern("yyyy-MM-dd");


            ArrayList<Topic> filteredTopics = new ArrayList<>();
            try {
                for (Topic topic : Utils.getAllFromRange(fromDate, toDate)) {
                    System.out.println("topic: " + topic.getName());
                    System.out.println("topic date: " + topic.getDate());
                    System.out.println("topic searched: " + selectedTopic);
                    if (topic.getName().contains(selectedTopic) &&
                            topic.getDate().after(fromDate) &&
                            topic.getDate().before(toDate)) {
                        filteredTopics.add(topic);
                    }
                }
                System.out.println("filtered : " + filteredTopics);
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
            }

            topicsTableModel.setRowCount(0);
            for (Topic topic : filteredTopics) {
                String dateString = dateFormat.format(topic.getDate());
                topicsTableModel.addRow(new Object[]{topic.getName(), dateString});
            }

            JTabbedPane tabbedPane = (JTabbedPane) getContentPane().getComponent(0);
            tabbedPane.setSelectedIndex(1);
        }
    }
    public static void main(String[] args) throws IOException, InterruptedException
    {
        new Main1();
    }
}