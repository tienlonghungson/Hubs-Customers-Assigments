package io;

import service.Triplet;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

public class TripletBaseInput implements InputInterface{
    public final int N_HUBS;
    public final int N_CUSTOMERS;

    private List<Triplet<Double,Double,Integer>> listHubs;
    private List<Triplet<Double,Double,Integer>> listCustomers;

    public TripletBaseInput(String filename) throws IOException {
        this(new File(filename));
    }

    public TripletBaseInput(File file) throws IOException {
        BufferedReader bfReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        StringTokenizer stringTokenizer = new StringTokenizer(bfReader.readLine()," ");

        N_HUBS = Integer.parseInt(stringTokenizer.nextToken());
        N_CUSTOMERS = Integer.parseInt(stringTokenizer.nextToken());
        listHubs = new ArrayList<>(N_HUBS);
        listCustomers = new ArrayList<>(N_CUSTOMERS);

        String currLine;
        for (int i = 0; i < N_HUBS; i++) {
            currLine = bfReader.readLine();
            stringTokenizer = new StringTokenizer(currLine);
            listHubs.add(new Triplet<>(
                    Double.parseDouble(stringTokenizer.nextToken()),
                    Double.parseDouble(stringTokenizer.nextToken()),
                    Integer.parseInt(stringTokenizer.nextToken()))
            );
        }

        for (int i = 0; i < N_CUSTOMERS; i++) {
            currLine = bfReader.readLine();
            stringTokenizer = new StringTokenizer(currLine);
            listCustomers.add(new Triplet<>(
                    Double.parseDouble(stringTokenizer.nextToken()),
                    Double.parseDouble(stringTokenizer.nextToken()),
                    Integer.parseInt(stringTokenizer.nextToken()))
            );
        }
    }

    @Override
    public int getNumHubs() {
        return N_HUBS;
    }

    @Override
    public int getNumCustomers() {
        return N_CUSTOMERS;
    }

    @Override
    public List<Triplet<Double, Double, Integer>> getListHubs() {
        return Collections.unmodifiableList(listHubs);
    }

    @Override
    public List<Triplet<Double, Double, Integer>> getListCustomers() {
        return Collections.unmodifiableList(listCustomers);
    }
}
