package service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

public class Utils {

    /**
     * calculate the matrix distance
     * @param listHubs list of hubs containing their coords and capacity
     * @param listCustomer list of customers containing their coords and demand
     * @return matrix distance
     */
    public static double[][] calMatrixDistances(List<Triplet<Double,Double,Integer>> listHubs, List<Triplet<Double,Double,Integer>> listCustomer){
        final int N_HUBS = listHubs.size();
        final int N_CUSTOMERS = listCustomer.size();

        double[][] matrixDistance = new double[N_CUSTOMERS][N_HUBS];
        for (int i = 0; i < N_CUSTOMERS; i++) {
            for (int j = 0; j < N_HUBS; j++) {
                matrixDistance[i][j] = euclidDistance(
                        listCustomer.get(i).first(),
                        listCustomer.get(i).second(),
                        listHubs.get(j).first(),
                        listHubs.get(j).second());
            }
        }
        return matrixDistance;
    }

    /**
     * get the top 5 nearest hubs to each customer
     * @param matrixDistance matrix of distance
     * @return an array containing index of top 5 nearest hubs to each customer
     */
    public static int[][] selectTop5Hubs(double[][] matrixDistance){
        final int N_CUSTOMERS = matrixDistance.length;
        final int N_HUBS=matrixDistance[0].length;
        if (N_HUBS<=5){
            int[][] top5HubIdx = new int[N_CUSTOMERS][N_HUBS];
            for (int i = 0; i < N_CUSTOMERS; i++) {
                for (int j = 0; j < N_HUBS; j++) {
                    top5HubIdx[i][j]=j;
                }
            }
            return top5HubIdx;
        }
        int[][] top5HubIdx = new int[N_CUSTOMERS][5];
        PriorityQueue<Pair<Double,Integer>>maxHeap;

        for (int i = 0; i < N_CUSTOMERS; i++) {
            maxHeap = new PriorityQueue<>((pair1,pair2)-> pair2.first().compareTo(pair1.first()));
            for (int j=0;j<N_HUBS;++j){
                maxHeap.add(new Pair<>(matrixDistance[i][j],j));
                if (maxHeap.size()>5){
                    maxHeap.poll();
                }
            }
            for (int j = 0; j < 5; j++) {
                top5HubIdx[i][j]= Objects.requireNonNull(maxHeap.poll()).second();
            }
        }

        return top5HubIdx;
    }

    public static double euclidDistance(double x1, double y1, double x2, double y2){
        double dX = x1-x2;
        double dY = y1-y2;
        return Math.sqrt(dX*dX+dY*dY);
    }

    /**
     * calculate violation in an assignment list
     * @param assigment list of assignments
     * @param listHubs list of hubs
     * @param listCustomers list of customers
     * @return a Pair<int,int> :
     *                              first is nViolation
     *                              second is weightViolation
     */
    public static Pair<Integer,Integer> calViolation(List<Integer> assigment,
                                       List<Triplet<Double,Double,Integer>> listHubs,
                                       List<Triplet<Double,Double,Integer>> listCustomers){
        final int N_HUBS = listHubs.size();
        final int N_CUSTOMERS = assigment.size();
        int nViolation = 0; int weightViolation = 0;
        int[] hubLoads =  new int[N_HUBS];

        for (int i=0;i<N_CUSTOMERS;++i){
            hubLoads[assigment.get(i)]+= listCustomers.get(i).third();
        }
        for (int i=0;i<N_HUBS;++i){
            if (hubLoads[i]>listHubs.get(i).third()){
                nViolation++;
                weightViolation += (hubLoads[i]-listHubs.get(i).third());
            }
        }
        return new Pair<>(nViolation,weightViolation);
    }

    /**
     * get the longest distance between customers and their allowed hubs
     * @param matrixDistance matrix of distance
     * @param top5HubIdx an array containing index of top 5 nearest hubs to each customer
     * @return longest distance
     */
    public static double getMaxDistance(double[][] matrixDistance, int[][] top5HubIdx){
        double maxDistance=0;
        final int N_NN = top5HubIdx[0].length;
        final int N_CUSTOMER = matrixDistance.length;

        for (int i=0;i<N_CUSTOMER;++i){
            for (int j=0;j<N_NN;++j){
                maxDistance = Math.max(maxDistance,matrixDistance[i][top5HubIdx[i][j]]);
            }
        }
        return maxDistance;
    }

    /**
     * convert the assignment with index in top5HubIdx to the assignment with actual index of hubs
     * @param assignments assignment with index in top5HubIdx
     * @param top5HubIdx an array containing index of top 5 nearest hubs to each customer
     * @return assignment with actual index of hubs
     */
    public static List<Integer> convertAssignmentsIdxFromTop5(List<Integer> assignments,int[][] top5HubIdx){
        final int N_CUSTOMERS = assignments.size();
        List<Integer> resAssignments = new ArrayList<>(N_CUSTOMERS);

        for (int i=0;i<N_CUSTOMERS;++i){
            resAssignments.add(top5HubIdx[i][assignments.get(i)]);
        }
        return resAssignments;
    }
}
