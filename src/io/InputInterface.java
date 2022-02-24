package io;

import service.Triplet;

import java.util.List;

public interface InputInterface {
    int getNumHubs();
    int getNumCustomers();
    List<Triplet<Double,Double,Integer>> getListHubs();
    List<Triplet<Double,Double,Integer>> getListCustomers();
}
