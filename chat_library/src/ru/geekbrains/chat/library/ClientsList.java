package ru.geekbrains.chat.library;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by Александр on 06.07.2017.
 */
public class ClientsList {
    public static Vector<String> clientList = new Vector<>();
    boolean isClientsListChanged;

    public ClientsList() {
        this.isClientsListChanged=false;
    }
    public  void addList(String nick){
        clientList.add(nick);
        isClientsListChanged=true;
    }
    public  void removeFromList(String nick){
        clientList.remove(nick);
        isClientsListChanged=true;
    }
    public boolean isClientsListChanged(){
        return isClientsListChanged;
    }
    public Vector<String> getClientList(){
        return clientList;
    }
}
