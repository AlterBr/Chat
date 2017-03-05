package main.server;

import java.io.*;
import java.net.*;
import java.util.*;

class VerySimpleChatServer {
    private ArrayList<ClientHandler> clientHandlerArrayList;
    private boolean notClose = true;
    private ListOfUserInRoom[] listOfRoomWithUser = new ListOfUserInRoom[3];
    private History history;

    public static void main(String[] args) {
        new VerySimpleChatServer().go();
    }

    private void go() {
        clientHandlerArrayList = new ArrayList<>();
        history = new History(10);
        listOfRoomWithUser[0] = new ListOfUserInRoom("main");
        listOfRoomWithUser[1] = new ListOfUserInRoom("yell");
        listOfRoomWithUser[2] = new ListOfUserInRoom("whisper");

        try {
            ServerSocket serverSock = new ServerSocket(5000);
            System.out.println("server starting...");
            Thread threadExit = new Thread(new ThreadExit());
            threadExit.start();
            while (notClose) {
                Socket clientSocket = serverSock.accept();
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
                ClientHandler clientHandler = new ClientHandler(clientSocket, writer, listOfRoomWithUser[0].getRoom());
                clientHandlerArrayList.trimToSize();
                clientHandlerArrayList.add(clientHandler);
                Thread t = new Thread(clientHandler);
                t.start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void tellRefresh(String toRoom) {
        for (ClientHandler clientHandler : clientHandlerArrayList) {
            if (toRoom.equals(clientHandler.getCurrentRoom())) {
                try {
                    Thread.sleep(100);
                    clientHandler.getWriterClient().println("/refresh");
                    clientHandler.getWriterClient().flush();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void tellEveryoneInRoom(String message, String toRoom, boolean systemMessage) {
        for (ClientHandler clientHandler : clientHandlerArrayList) {
            if (toRoom.equals(clientHandler.getCurrentRoom())) {
                try {
                    clientHandler.getWriterClient().println(message);
                    clientHandler.getWriterClient().flush();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (toRoom.equals(listOfRoomWithUser[0].getRoom()) && !systemMessage) {
            history.addToHistory(message);
        }
    }

    private void tellPrivateMessage(String message, String userName) {
        for (ClientHandler clientHandler : clientHandlerArrayList) {
            if (userName.equals(clientHandler.getName())) {
                try {
                    clientHandler.getWriterClient().println(message);
                    clientHandler.getWriterClient().flush();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            }
        }
    }

    private void tellCurrentUser(String message, ClientHandler currentCH) {
        for (ClientHandler clientHandler : clientHandlerArrayList) {
            if (currentCH == clientHandler) {
                try {
                    clientHandler.getWriterClient().println(message);
                    clientHandler.getWriterClient().flush();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            }
        }
    }

    class ClientHandler implements Runnable {
        BufferedReader reader;
        Socket sock;
        PrintWriter writerClient;
        String name;
        String currentRoom;
        ObjectOutputStream objectOutputStream;

        ClientHandler(Socket clientSocket, PrintWriter writer, String room) {
            try {
                currentRoom = room;
                writerClient = writer;
                sock = clientSocket;
                InputStreamReader isReader = new InputStreamReader(sock.getInputStream());
                reader = new BufferedReader(isReader);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        String getCurrentRoom() {
            return currentRoom;
        }

        PrintWriter getWriterClient() {
            return writerClient;
        }

        String getName() {
            return name;
        }

        void removeFromList(String name) {
            for (ListOfUserInRoom listOfUserInRoom : listOfRoomWithUser) {
                if (getCurrentRoom().equals(listOfUserInRoom.getRoom())) {
                    listOfUserInRoom.removeFromList(name);
                    break;
                }
            }
        }

        void addToList(String name) {
            for (ListOfUserInRoom listOfUserInRoom : listOfRoomWithUser) {
                if (getCurrentRoom().equals(listOfUserInRoom.getRoom())) {
                    listOfUserInRoom.addToList(name);
                    break;
                }
            }
        }

        void initRoom() {
            String[] listOfRoom = new String[listOfRoomWithUser.length];
            for (int i = 0; i < listOfRoomWithUser.length; i++) {
                listOfRoom[i] = listOfRoomWithUser[i].getRoom();
            }
            Set<String> listOfRoomSet = new HashSet<>(Arrays.asList(listOfRoom));
            try {
                ServerSocket serverSock = new ServerSocket(5050);
                Socket clientSocket = serverSock.accept();
                objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                objectOutputStream.writeObject(listOfRoomSet);
                serverSock.close();
                clientSocket.close();
                objectOutputStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        void initHistory() {
            try {
                ServerSocket serverSock = new ServerSocket(5050);
                Socket clientSocket = serverSock.accept();
                objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                objectOutputStream.writeObject(history.getHistory());
                serverSock.close();
                clientSocket.close();
                objectOutputStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        void postUsers() {
            try {
                ServerSocket serverSock = new ServerSocket(5050);
                Socket clientSocket = serverSock.accept();
                objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                for (ListOfUserInRoom listOfUserInRoom : listOfRoomWithUser) {
                    if (listOfUserInRoom.getRoom().equals(currentRoom)) {
                        objectOutputStream.writeObject(listOfUserInRoom.getListOfUsers());
                        break;
                    }
                }
                serverSock.close();
                clientSocket.close();
                objectOutputStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void run() {
            String message;
            try {
                //Проверка ника на оригинальность
                String tempName = reader.readLine();
                for (ClientHandler clientHandler : clientHandlerArrayList) {
                    if (tempName.equals(clientHandler.getName())) {
                        throw new NameException();
                    }
                }
                name = tempName;
                tellCurrentUser("networking established", this);
                System.out.println("got a connection: " + name);
                tellEveryoneInRoom(name + " присоеденился к чату", currentRoom, true);
                addToList(name);

                //Отправляем историю сообщений клиенту при подключении
                initHistory();

                //Передаем список комнат клиенту
                initRoom();

                //обновляем список клиентов при подключении
                tellRefresh(currentRoom);

                while ((message = reader.readLine()) != null) {
                    switch (message) {
                        case "/refreshButton":
                            postUsers();
                            break;
                        case "/privateMessage":
                            String nick = reader.readLine();
                            if (reader.readLine().equals("OK")) {
                                String privateMessage = reader.readLine();
                                tellPrivateMessage(privateMessage, nick);
                                tellCurrentUser(privateMessage, this);
                            }
                            break;
                        case "/name":
                            tempName = reader.readLine();
                            boolean sameName = false;
                            for (ClientHandler clientHandler : clientHandlerArrayList) {
                                if (tempName.equals(clientHandler.getName())) {
                                    sameName = true;
                                    break;
                                }
                            }
                            if (sameName) {
                                tellCurrentUser("Такой ник уже используется, выберете другой.", this);
                            } else {
                                tellCurrentUser("/GOOD", this);
                                tellCurrentUser("Server: ник успешно изменен.", this);
                                tellEveryoneInRoom(name + " изменил ник на " + tempName, currentRoom, true);
                                name = tempName;
                            }
                            break;
                        case "/changeRoom":
                            tellEveryoneInRoom(name + " вышел из комнаты " + currentRoom, currentRoom, true);

                            removeFromList(name);
                            tellRefresh(currentRoom);

                            currentRoom = reader.readLine();

                            addToList(name);
                            tellRefresh(currentRoom);

                            tellEveryoneInRoom(name + " подлючился к комнате " + currentRoom, currentRoom, true);
                            break;
                        case "/disconnect":
                            throw new Exception();
                        default:
                            System.out.println("[server read]" + "[" + currentRoom + "]: " + message);
                            tellEveryoneInRoom(message, currentRoom, false);
                    }
                }
            } catch (NameException ex) {
                tellCurrentUser("Такой ник уже используется. Пожалуйста, смените ник.", this);
                tellCurrentUser("/disconnect", this);
                clientHandlerArrayList.remove(this);
                clientHandlerArrayList.trimToSize();
            } catch (Exception ex) {
                System.out.println(name + " disconnect");
                tellEveryoneInRoom(name + " disconnect", currentRoom, true);

                removeFromList(name);
                tellRefresh(currentRoom);

                clientHandlerArrayList.remove(this);
                clientHandlerArrayList.trimToSize();
            }
        }
    }

    class ThreadExit implements Runnable {
        public void run() {
            try {
                BufferedReader readExit = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    if (readExit.readLine().equals("exit")) {
                        notClose = false;
                        System.exit(0);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}