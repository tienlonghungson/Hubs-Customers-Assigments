package main;

import model.AbstractModel;
import model.constraintprogramming.CPModel;
import model.integerprogramming.IPModel;
import model.tabu.TabuModel;

public class Main {
    static {
        com.google.ortools.Loader.loadNativeLibraries();
    }

    public static void main(String[] args){
        AbstractModel model;
        switch (args[0]){
            case "CP" -> model = new CPModel();
            case "TabuSearch" -> model = new TabuModel();
            default -> model = new IPModel();
        }
        AbstractModel.execute(model,Integer.parseInt(args[1]),args[2]);
    }
}
