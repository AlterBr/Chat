package main.server;

import java.util.HashSet;
import java.util.Set;

class ListOfUserInRoom {
    private String room;
    private Set<String> listOfUsers;

    ListOfUserInRoom(String room) {
        this.room = room;
        listOfUsers = new HashSet<>();
    }

    String getRoom() {
        return room;
    }

    Set<String> getListOfUsers() {
        return listOfUsers;
    }

    void addToList(String name) {
        listOfUsers.add(name);
    }

    void removeFromList(String name) {
        listOfUsers.remove(name);
    }

}