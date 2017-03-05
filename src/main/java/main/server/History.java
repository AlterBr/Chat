package main.server;

import java.io.Serializable;

class History implements Serializable {
    private String[] history;

    History(int sizeOfArray){
        history = new String[sizeOfArray];
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

    void addToHistory(String str){
        if (history[history.length-1] != null){
            for (int i = 0; i < history.length-1; i++) {
                history[i] = history[i+1];
            }
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
}
