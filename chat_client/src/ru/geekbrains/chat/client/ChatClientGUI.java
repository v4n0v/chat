package ru.geekbrains.chat.client;

import ru.geekbrains.chat.library.Messages;
import ru.geekbrains.network.SocketThread;
import ru.geekbrains.network.SocketThreadListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import static java.lang.Thread.sleep;

public class ChatClientGUI extends JFrame implements ActionListener, Thread.UncaughtExceptionHandler, SocketThreadListener {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChatClientGUI();
            }
        });
    }

    private static final int WIDTH = 800;
    private static final int HEIGHT = 300;
    private static final String TITLE = "Chat client";

    private final JPanel upperPanel = new JPanel(new GridLayout(2, 3));
    private final JTextField fieldIPAddr = new JTextField("localhost");
 //   private final JTextField fieldIPAddr = new JTextField("89.222.249.131");
    private final JTextField fieldPort = new JTextField("8189");
    private final JCheckBox chkAlwaysOnTop = new JCheckBox("Always on top", true);
    private final JTextField fieldLogin = new JTextField("v4n0v");
    private final JPasswordField fieldPass = new JPasswordField("qazwsx123");
    private final JButton btnLogin = new JButton("Login");

    private final JTextArea log = new JTextArea();

    DefaultListModel listModel = new DefaultListModel();
    private final JList<String> userList = new JList<>(listModel);

    private boolean tipFieldInput=false;
    private final JPanel bottomPanel = new JPanel(new BorderLayout());
    private final JButton btnDisconnect = new JButton("Disconnect");
    private final JTextField fieldInput = new JTextField();
    private final JButton btnSend = new JButton("Send");
    private final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss ");

    private ChatClientGUI() {
        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setTitle(TITLE);
        setLocationRelativeTo(null);

        upperPanel.add(fieldIPAddr);
        upperPanel.add(fieldPort);
        upperPanel.add(chkAlwaysOnTop);
        upperPanel.add(fieldLogin);
        upperPanel.add(fieldPass);
        upperPanel.add(btnLogin);
        add(upperPanel, BorderLayout.NORTH);

        log.setEditable(false);
        log.setLineWrap(true);
        JScrollPane scrollLog = new JScrollPane(log);
        add(scrollLog, BorderLayout.CENTER);
        JScrollPane scrollUsers = new JScrollPane(userList);
        scrollUsers.setPreferredSize(new Dimension(150, 0));
        add(scrollUsers, BorderLayout.EAST);

        bottomPanel.add(btnDisconnect, BorderLayout.WEST);
        bottomPanel.add(fieldInput, BorderLayout.CENTER);
        bottomPanel.add(btnSend, BorderLayout.EAST);
        bottomPanel.setVisible(true);
        add(bottomPanel, BorderLayout.SOUTH);

        fieldIPAddr.addActionListener(this);
        fieldPort.addActionListener(this);
        fieldLogin.addActionListener(this);
        fieldPass.addActionListener(this);
        btnLogin.addActionListener(this);
        btnDisconnect.addActionListener(this);
        fieldInput.addActionListener(this);
        btnSend.addActionListener(this);
        chkAlwaysOnTop.addActionListener(this);

        setAlwaysOnTop(chkAlwaysOnTop.isSelected());
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (    src == fieldIPAddr ||
                src == fieldPort ||
                src == fieldLogin ||
                src == fieldPass ||
                src == btnLogin) {
            connect();
        } else if (src == btnDisconnect) {
            disconnect();
        } else if (src == fieldInput || src == btnSend) {
            if (src == fieldInput){
                if (tipFieldInput==true){
                    fieldInput.setText("");
                    tipFieldInput=false;
                }
            }
            if (fieldInput.getText().equals("")){

                    fieldInput.setText("Type the message");
                    tipFieldInput=true;

            } else
            sendMsg();
        } else if(src == chkAlwaysOnTop) {
            setAlwaysOnTop(chkAlwaysOnTop.isSelected());
        } else {
            throw new RuntimeException("Unknown src = " + src);
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        StackTraceElement[] stackTraceElements = e.getStackTrace();
        String msg;
        if (stackTraceElements.length == 0) {
            msg = "Пустой stackTraceElements";
        } else {
            msg = e.getClass().getCanonicalName() + ": " + e.getMessage() + "\n" + stackTraceElements[0];
        }
        JOptionPane.showMessageDialog(null, msg, "Exception: ", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private SocketThread socketThread;

    private void connect() {
        try {
            Socket socket = new Socket(fieldIPAddr.getText(), Integer.parseInt(fieldPort.getText()));
            socketThread = new SocketThread(this, "SocketThread", socket);
        } catch (IOException e) {
            e.printStackTrace();
            log.append("Exception: " + e.getMessage() + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        }
    }

    private void disconnect() {
        socketThread.close();
    }

    private void sendMsg() {
        String msg = fieldInput.getText();
        if(msg.equals("")) return;
        fieldInput.setText(null);
        socketThread.sendMsg(msg);
    }

    @Override
    public void onStartSocketThread(SocketThread socketThread) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append("Поток сокета запущен.\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
        });
    }

    @Override
    public void onStopSocketThread(SocketThread socketThread) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append("Соединение потеряно.\n");
                log.setCaretPosition(log.getDocument().getLength());
                upperPanel.setVisible(true);
                bottomPanel.setVisible(false);
            }
        });
    }

    @Override
    public void onReadySocketThread(SocketThread socketThread, Socket socket) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append("Соединение установлено.\n");
                log.setCaretPosition(log.getDocument().getLength());
                upperPanel.setVisible(false);
                bottomPanel.setVisible(true);
                String login = fieldLogin.getText();
                String password = new String(fieldPass.getPassword());
                socketThread.sendMsg(Messages.getAuthRequest(login, password));
            }
        });
    }

    @Override
    public void onReceiveString(SocketThread socketThread, Socket socket, String value) {

        String input = fieldInput.getText();
        SwingUtilities.invokeLater(new Runnable() {
            @Override

            public void run() {
                handleMsg(value);
            }
        });
    }

    private void handleMsg(String msg) {
//        String[] msgArray = msg.split(Messages.DELIMITER);
//        msg = "";
//        String userList;
//        if (msgArray[0].equals(Messages.BROADCAST)){
//            msgArray[0] = "\n";
//
//            Long date = new Long(msgArray[1]);
//            //  String date = dateFormat.format();
//            msgArray[1]=dateFormat.format(date);
//            if (msgArray[2].equals("Server")){
//
//            }
//            msgArray[2]+=": \n";
//        }else if (msgArray[0].equals(Messages.AUTH_ACCEPT)) {
//            msgArray[0] = "\nAuthetification accepted. Welcome ";
//
//        }
//        else if (msgArray[0].equals(Messages.AUTH_REQUEST))
//            msgArray[0]="Authetification requested ";
//        else if (msgArray[0].equals(Messages.AUTH_ERROR))
//            msgArray[0]="Authetification error ";
//        else if (msgArray[0].equals(Messages.USERS_LIST)){
//            listModel.clear();
//            String[] userListArr = msgArray[1].split(Messages.USERLIST_DELIMITER);
//            for (int i = 0; i < userListArr.length; i++) {
//                listModel.addElement(userListArr[i]);
//
//            }
//            return;

        String[] tokens = msg.split(Messages.DELIMITER);
        String type = tokens[0];
        switch (type) {
            case Messages.AUTH_ACCEPT:
                log.append("\n");
                setTitle(TITLE+ ": "+tokens[1]);
                log.setCaretPosition(log.getDocument().getLength());
                break;
            case Messages.AUTH_ERROR:
                log.append(dateFormat.format(System.currentTimeMillis())+" ошибка авторизации\n");
                log.setCaretPosition(log.getDocument().getLength());
                break;
            case Messages.BROADCAST:
                log.append(dateFormat.format(Long.parseLong(tokens[1])) + tokens[2] + ": " + tokens[3] + "\n");
                log.setCaretPosition(log.getDocument().getLength());
                break;
            case Messages.USERS_LIST:
                log.append(msg + "\n");
                log.setCaretPosition(log.getDocument().getLength());
                break;
            case Messages.RECONNECT:
                log.append(msg + "\n");
                log.setCaretPosition(log.getDocument().getLength());
                break;
            case Messages.MSG_FORMAT_ERROR:
                log.append("Неверный формат сообщения: \n'" + msg + "'\n");
                log.setCaretPosition(log.getDocument().getLength());
                socketThread.close();
                break;
            default:
                throw new RuntimeException("Неизвестный тип сообщения: " + msg);


        }


//        if (msgArray.length==5){
//
//            String[] list=msgArray[5].split(Messages.clientListSepapator);
//            userList = new JList<>(list);
//
//        }
//        for (int i = 0; i < msgArray.length; i++) {
//            msg+=msgArray[i]+" ";
//        }
        //    msg+=;
//
//        log.append(msg + "\n");
//        log.setCaretPosition(log.getDocument().getLength());
    }

    @Override
    public void onExceptionSocketThread(SocketThread socketThread, Socket socket, Exception e) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                e.printStackTrace();
//                listModel.clear();
//                log.append("Exception: " + e.getMessage() + "\n");
//                log.setCaretPosition(log.getDocument().getLength());
            }
        });
    }
}
