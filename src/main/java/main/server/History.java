package main.server;

import java.io.Serializable;

class History implements Serializable {
    private String[] history;

    History(int sizeOfArray){
        history = new String[sizeOfArray];
    }

    void addToHistory(String str){
        if (history[history.length-1] != null){
            System.arraycopy(history, 1, history, 0, history.length - 1);
            history[history.length-1] = str;
        }else {
            for (int i = 0; i < history.length; i++) {
                if (history[i] == null) {
                    history [i] = str;
                    break;
                }
            }
        }
    }

    String[] getHistory() {
        return history;
    }

    @Override
    public String toString() {
        String string = "";
        for (String aHistory : history) {
            string += aHistory + " ";
        }
        return string;
    }
}
