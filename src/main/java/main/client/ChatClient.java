package main.client;

import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

class ChatClient {
    private JTextArea incoming;
    private JTextField outgoing;
    private JLabel nameLabel;
    private JFrame frame;
    private JList incomingList;

    private Socket sock;
    private BufferedReader reader;
    private PrintWriter writer;
    private Thread readerThread;

    private boolean lostConnect = true;
    private boolean nicknameCheck = false;
    private boolean ipCheck = true;         //!!!!!

    private String ip = "127.0.0.1";        //!!!!!
    private String name = "";
    private String newName = "";
    private String currentRoom = "main";
    private Set<String> listOfRoom = new HashSet<>();
    private Set<String> listOfUsers = new HashSet<>();

    public static void main(String[] args) {
        new ChatClient().go();
    }

    private void go() {
        frame = new JFrame("Simple Chat Client");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel mainPanel = new JPanel();

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Main");
        JMenuItem ipMenu = new JMenuItem("Change ip");
        JMenuItem nickMenu = new JMenuItem("Change nickname");
        JMenuItem roomMenu = new JMenuItem("Change room");
        JMenuItem conMenu = new JMenuItem("Connect");
        JMenuItem disMenu = new JMenuItem("Disconnect");
        JMenuItem exitMenu = new JMenuItem("Exit");
        menu.add(ipMenu);
        ipMenu.addActionListener(new IpButtonListener());
        menu.add(nickMenu);
        nickMenu.addActionListener(new NicknameButtonListener());
        menu.add(roomMenu);
        roomMenu.addActionListener(new RoomButtonListener());
        menu.add(conMenu);
        conMenu.addActionListener(new ConnectButtonListener());
        menu.add(disMenu);
        disMenu.addActionListener(new DisconnectButtonListener());
        menu.add(exitMenu);
        exitMenu.addActionListener(new ExitButtonListener());
        menuBar.add(menu);

        Box chatBox = new Box(BoxLayout.Y_AXIS);
        Box sendBox = new Box(BoxLayout.LINE_AXIS);
        Box listBox = new Box(BoxLayout.Y_AXIS);

        incoming = new JTextArea(15, 40);
        incoming.setLineWrap(true);
        incoming.setWrapStyleWord(true);
        incoming.setEditable(false);


        incomingList = new JList();
        incomingList.addListSelectionListener(new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        theList.setPreferredSize(new Dimension(60, 243));
        theList.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        theList.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JScrollPane qScroller = new JScrollPane(incoming);
        qScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        qScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        SendButtonListener sendButtonListener = new SendButtonListener();
        outgoing = new JTextField(20);
        outgoing.addActionListener(sendButtonListener);

        JButton sendButton = new JButton("Отправить");
        nameLabel = new JLabel(name);
        sendButton.addActionListener(sendButtonListener);

        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(new RefreshButtonListener());

        listBox.add(theList);
        listBox.add(Box.createVerticalStrut(10));
        listBox.add(refreshButton);

        sendBox.add(nameLabel);
        sendBox.add(Box.createHorizontalStrut(10));
        sendBox.add(outgoing);
        sendBox.add(Box.createHorizontalStrut(10));
        sendBox.add(sendButton);

        chatBox.add(qScroller);
        chatBox.add(Box.createVerticalStrut(10));
        chatBox.add(sendBox);

        mainPanel.add(BorderLayout.WEST, chatBox);
        mainPanel.add(BorderLayout.EAST, listBox);

        frame.setJMenuBar(menuBar);
        frame.getContentPane().add(mainPanel);
        frame.setSize(600, 350);
        frame.setResizable(false);
        frame.setVisible(true);

        incoming.append("Введите ваш никнейм и IP адрес" + "\n");
    }

    private void setUpNetworking() {
        try {
            sock = new Socket(ip, 5000);
            InputStreamReader streamReader = new InputStreamReader(sock.getInputStream());
            reader = new BufferedReader(streamReader);
            writer = new PrintWriter(sock.getOutputStream());
            lostConnect = false;
            readerThread = new Thread(new IncomingReader());
            readerThread.start();
            if (!lostConnect) {
                writer.println(name);
                writer.flush();
            }
            getHistory();
            listOfRoom = getUsersOrRoom();
        } catch (Exception ex) {
            System.out.println("server is not available");
            incoming.append("server is not available" + "\n");
            lostConnect = true;
        }
    }

    private void setName(String name) {
        this.name = name;
        nameLabel.setText(this.name);
    }

    private void setIp(String ip) {
        this.ip = ip;
        incoming.append("IP адрес теперь: " + this.ip + "\n");
    }

    private void getHistory() {
        try {
            Socket secondSock = new Socket(ip, 5050);
            ObjectInputStream objectInputStream = new ObjectInputStream(secondSock.getInputStream());
            String[] history = (String[]) objectInputStream.readObject();
            for (String aHistory : history) {
                if (aHistory != null) {
                    incoming.append(aHistory + "\n");
                }
            }
            secondSock.close();
            objectInputStream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Set<String> getUsersOrRoom() {
        Set<String> coll = null;
        try {
            Socket secondSock = new Socket(ip, 5050);
            ObjectInputStream objectInputStream = new ObjectInputStream(secondSock.getInputStream());
            coll = (Set<String>) objectInputStream.readObject();
            secondSock.close();
            objectInputStream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return coll;
    }

    private void refreshList() {
        if (!lostConnect) {
            listOfUsers.clear();
            writer.println("/refreshButton");
            writer.flush();
            listOfUsers = getUsersOrRoom();
            Vector<String> vector = new Vector<>(listOfUsers);
            incomingList.setListData(vector);
        } else {
            incoming.append("Вы не подключены!" + "\n");
        }
    }

    class IncomingReader implements Runnable {
        public void run() {
            String message;
            try {
                while ((message = reader.readLine()) != null) {
                    switch (message) {
                        case "/refresh":
                            refreshList();
                            break;
                        case "/disconnect":
                            sock.close();
                            writer.close();
                            reader.close();
                            readerThread.interrupt();
                            throw new Exception();
                        case "/GOOD":
                            setName(newName);
                            nicknameCheck = true;
                            break;
                        default:
                            System.out.println("[client read]: " + message);
                            incoming.append(message + "\n");
                    }
                }
            } catch (Exception ex) {
                System.out.println("connection lost");
                incoming.append("connection lost" + "\n");
                lostConnect = true;
                listOfRoom.clear();
                currentRoom = "main";
            }
        }
    }

    class RefreshButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent ev) {
            refreshList();
        }
    }

    class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                String selected = (String) incomingList.getSelectedValue();
                if (selected != null) {
                    if (selected.equals(name)) {
                        incomingList.clearSelection();
                    } else {
                        writer.println("/privateMessage");
                        writer.flush();
                        writer.println(selected);
                        writer.flush();
                        String message = JOptionPane.showInputDialog(frame, "Введите личное сообщение");
                        if (message != null && !message.isEmpty()) {
                            writer.println("OK");
                            writer.flush();
                            writer.println("From " + name + " to " + selected + ": " + message);
                            writer.flush();
                        } else {
                            writer.println("nope");
                            writer.flush();
                        }
                        incomingList.clearSelection();
                    }
                }

            }
        }
    }

    class SendButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent ev) {
            if (!outgoing.getText().isEmpty() && !lostConnect) {
                writer.println("[" + name + "]: " + outgoing.getText());
                writer.flush();
            } else {
                if (lostConnect) {
                    System.out.println("server is not available");
                    incoming.append("server is not available" + "\n");
                }
            }
            outgoing.setText("");
            outgoing.requestFocus();
        }
    }

    class ConnectButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (nicknameCheck && ipCheck) {
                if (lostConnect) {
                    incoming.setText("");
                    setUpNetworking();
                } else {
                    incoming.append("Вы уже в сети" + "\n");
                }
            } else {
                incoming.append("Введите никнейм и IP адрес" + "\n");
            }
        }
    }

    class DisconnectButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (!lostConnect) {
                try {
                    if (readerThread != null && writer != null && sock != null && reader != null) {
                        writer.println("/disconnect");
                        writer.flush();
                        sock.close();
                        writer.close();
                        reader.close();
                        readerThread.interrupt();
                        lostConnect = true;
                        listOfRoom.clear();
                        currentRoom = "main";
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                incoming.append("Вы не подключены!" + "\n");
            }
        }
    }

    class IpButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent ev) {
            String newIp = JOptionPane.showInputDialog(frame, "Введите IP адрес:", ip);
            if (newIp != null && !newIp.isEmpty() && !newIp.equals(ip)) {
                setIp(newIp);
                ipCheck = true;
            }
        }
    }

    class NicknameButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent ev) {
            newName = JOptionPane.showInputDialog(frame, "Введите ваш ник:", name);
            if (newName != null && !newName.isEmpty() && !newName.equals(name)) {
                if (newName.length() < 3 || newName.length() > 14) {
                    if (newName.length() < 3) incoming.append("Введите никнейм не менее чем из 3 символов!" + "\n");
                    if (newName.length() > 14) incoming.append("Введите никнейм не более чем из 14 символов!" + "\n");
                } else {
                    if (lostConnect) {
                        setName(newName);
                        nicknameCheck = true;
                    } else {
                        writer.println("/name");
                        writer.flush();
                        writer.println(newName);
                        writer.flush();
                    }

                }
            }
        }
    }

    class RoomButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (!listOfRoom.isEmpty()) {
                String[] listOfRoomArr = listOfRoom.toArray(new String[listOfRoom.size()]);
                String newCurrentRoom = (String) JOptionPane.showInputDialog(frame, "Выберите комнату:", "Выбор комнаты", JOptionPane.PLAIN_MESSAGE, null, listOfRoomArr, currentRoom);
                if (newCurrentRoom != null && !newCurrentRoom.equals(currentRoom)) {
                    currentRoom = newCurrentRoom;
                    writer.println("/changeRoom");
                    writer.flush();
                    writer.println(currentRoom);
                    writer.flush();
                }
            } else {
                incoming.append("Вы не подключены!" + "\n");
            }
        }
    }

    class ExitButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }

}