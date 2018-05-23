package ru.geekbrains.chat.server.core;

import ru.geekbrains.chat.library.Messages;
import ru.geekbrains.network.ServerSocketThread;
import ru.geekbrains.network.ServerSocketThreadListener;
import ru.geekbrains.network.SocketThread;
import ru.geekbrains.network.SocketThreadListener;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Vector;

public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {

    private final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss: ");
    private final ChatServerListener eventListener;
    private final SecurityManager securityManager;
    private ServerSocketThread serverSocketThread;
    private final Vector<SocketThread> clients = new Vector<>();
    private ArrayList<String> userList = new ArrayList<>();
    private  String users;
    public ChatServer(ChatServerListener eventListener, SecurityManager securityManager) {
        this.eventListener = eventListener;
        this.securityManager = securityManager;
    }
    public void startListening(int port) {
        if(serverSocketThread != null && serverSocketThread.isAlive()) {
            putLog("Сервер уже запущен.");
            return;
        }
        serverSocketThread = new ServerSocketThread(this, "ServerSocketThread", port, 2000);
        securityManager.init();
    }

    public void dropAllClients() {
        putLog("dropAllClients");
    }

    public void stopListening() {
        if(serverSocketThread == null || !serverSocketThread.isAlive()) {
            putLog("Сервер не запущен.");
            return;
        }
        serverSocketThread.interrupt();
        securityManager.dispose();
    }

    //ServerSocketThread
    @Override
    public void onStartServerSocketThread(ServerSocketThread thread) {
        putLog("started...");
    }

    @Override
    public void onStopServerSocketThread(ServerSocketThread thread) {
        putLog("stopped.");
    }

    @Override
    public void onReadyServerSocketThread(ServerSocketThread thread, ServerSocket serverSocket) {
        putLog("ServerSocket is ready...");
    }

    @Override
    public void onTimeOutAccept(ServerSocketThread thread, ServerSocket serverSocket) {
//        putLog("accept() timeout");
    }

    @Override
    public void onAcceptedSocket(ServerSocketThread thread, ServerSocket serverSocket, Socket socket) {
        putLog("Client connected: " + socket);
        String threadName = "Socket thread: " + socket.getInetAddress() + ":" + socket.getPort();
        new ChatSocketThread(this, threadName, socket);
    }

    @Override
    public void onExceptionServerSocketThread(ServerSocketThread thread, Exception e) {
        putLog("Exception: " + e.getClass().getName() + ": " + e.getMessage());
    }

    private synchronized void putLog(String msg) {
        String msgLog = dateFormat.format(System.currentTimeMillis()) +
                Thread.currentThread().getName() + ": " + msg;
        eventListener.onChatServerLog(this, msgLog);
    }

    //SocketThread
    @Override
    public synchronized void onStartSocketThread(SocketThread socketThread) {
        putLog("started...");
    }

    @Override
    public synchronized void onStopSocketThread(SocketThread socketThread) {
        clients.remove(socketThread);
        putLog("stopped.");
        ChatSocketThread client = (ChatSocketThread) socketThread;
        if(client.isAuthorized()) {
            sendToAllAuthorizedClients(Messages.getBroadcast("Server", client.getNick() + " disconnected."));
            updateUserlist(client.getNick(), 0);
            sendToAllAuthorizedClients(Messages.getUsersList(users));
        }
    }

    @Override
    public synchronized void onReadySocketThread(SocketThread socketThread, Socket socket) {
        putLog("Socket is ready.");
        clients.add(socketThread);
    }

    @Override
    public synchronized void onReceiveString(SocketThread socketThread, Socket socket, String value) {
        ChatSocketThread client = (ChatSocketThread) socketThread;
        if(client.isAuthorized()) {
            handleAuthorizeClient(client, value);
        } else {
            handleNonAuthorizeClient(client, value);
        }
    }

    private void handleAuthorizeClient(ChatSocketThread client, String msg){
        sendToAllAuthorizedClients(Messages.getBroadcast(client.getNick(), msg));
    }

    private void sendToAllAuthorizedClients(String msg) {
        for (int i = 0; i < clients.size(); i++) {
            ChatSocketThread client = (ChatSocketThread) clients.get(i);
            if(client.isAuthorized()) client.sendMsg(msg);
        }
    }

    private String userListToString(ArrayList<String> userList){
        String users="";
        for (int i = 0; i < userList.size(); i++) {
            users+=userList.get(i)+Messages.USERLIST_DELIMITER;
        }
        return users;
    }
    private void updateUserlist(String nick, int var){
        if (var == 1) userList.add(nick);
        else if (var==0) userList.remove(nick);
        users=userListToString(userList);
    }
    private void handleNonAuthorizeClient(ChatSocketThread newClient, String msg){
        putLog("auth msg: '" + msg + "'");
        String[] tokens = msg.split(Messages.DELIMITER);
        if(tokens.length != 3 || !tokens[0].equals(Messages.AUTH_REQUEST)) {
            newClient.messageFormatError(msg);
            return;
        }
        String login = tokens[1];
        String password = tokens[2];
        String nickname = securityManager.getNick(login, password);
        if(nickname == null) {
            newClient.authError();
            return;
        }
        ChatSocketThread client = getClientByNick(nickname);
        newClient.authorizeAccept(nickname);
        if (client == null) {
            putLog(nickname + " connected.");
            sendToAllAuthorizedClients(Messages.getBroadcast("Server", newClient.getNick() + " connected."));
            sendToAllAuthorizedClients(Messages.getUsersList(users));
        } else {
            putLog(nickname + " reconnected.");
            client.reconnect();
            newClient.sendMsg(Messages.getUsersList(users));
        }
        newClient.authorizeAccept(nickname);
        updateUserlist(newClient.getNick(), 1);
        sendToAllAuthorizedClients(Messages.getUsersList(users));
    }

    public ChatSocketThread getClientByNick(String nickname){
        final int cnt =clients.size();

        for (int i = 0; i < cnt; i++) {
            ChatSocketThread client = (ChatSocketThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            if (client.getNick().equals(nickname)) return client;

        }
        return null;
    }

    @Override
    public synchronized void onExceptionSocketThread(SocketThread socketThread, Socket socket, Exception e) {
        putLog("Exception: " + e.getClass().getName() + ": " + e.getMessage());
    }
}
