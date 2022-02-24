package model.tabu;

import service.Triplet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Solution implements Cloneable{
    protected static int nHubs, nCustomers, W;
    protected static List<Triplet<Double,Double,Integer>> listHubs;
    protected static List<Triplet<Double,Double,Integer>> listCustomers;
    protected static double[][] matrixDistance;
    protected static int[][] top5HubIdx;

    protected List<Integer> assignments; /* assignment for each customer (idx in top5HubIdx,
    not the idx of hub)*/
    protected int violation;
    protected long[] diffLoadAtHub;
    protected double totalDistance;
    protected double obj;

    public Solution(List<Integer> assignments){
        this.assignments = assignments;
        diffLoadAtHub = new long[nHubs];
        calLoadAndViolationAndObj();
    }

    public Solution(List<Integer> assignments, double totalDistance,
                    int violation, long[] diffLoadAtHub, double obj){
        this(assignments);
        this.totalDistance = totalDistance;
        this.diffLoadAtHub = diffLoadAtHub;
        this.violation=violation;
        this.obj=obj;
    }

    /**
     * update currLoad, Violation and Objection following the information of the move
     * @param moveInfo a Triplet including integer:
     *                 first: index of customer
     *                 second: index of the old hub assigned to this customer
     *                 third: index of the new hub assigned to this customer
     */
    protected void updateLoadAndViolationAndObj(Triplet<Integer,Integer,Integer> moveInfo){
        int idxCus = moveInfo.first();
        int oldHubIdx = top5HubIdx[idxCus][moveInfo.second()];
        int newHubIdx = top5HubIdx[idxCus][moveInfo.third()];

        // temporary remove the quantity of violated at hubs oldHub and newHub
        violation -= Math.max(0,diffLoadAtHub[oldHubIdx]);
        violation -= Math.max(0,diffLoadAtHub[newHubIdx]);

        diffLoadAtHub[oldHubIdx] -= listCustomers.get(idxCus).third();
        diffLoadAtHub[newHubIdx] += listCustomers.get(idxCus).third();

        // update the quantity of violated at hubs oldHub and newHub
        violation += Math.max(0,diffLoadAtHub[oldHubIdx]);
        violation += Math.max(0,diffLoadAtHub[newHubIdx]);

        // update distance
        totalDistance-=matrixDistance[idxCus][oldHubIdx];
        totalDistance+=matrixDistance[idxCus][newHubIdx];

        // update objective
        obj = calObjective(totalDistance,violation);
    }

    /**
     * update solution by a new move
     * @param moveInfo a Triplet including integer:
     *                 first: index of customer
     *                 second: index of the old hub assigned to this customer
     *                 third: index of the new hub assigned to this customer
     */
    protected void update(Triplet<Integer,Integer,Integer> moveInfo){
        assignments.set(moveInfo.first(),moveInfo.third());
        updateLoadAndViolationAndObj(moveInfo);
    }

    /**
     * 1st time calculate diffLoad, violation, totalDistance and objective
     */
    protected void calLoadAndViolationAndObj(){
        violation=0;
        totalDistance=0;
        Arrays.fill(diffLoadAtHub,0L);

        int idxHub;
        for (int i=0;i< nCustomers;++i){
            idxHub=top5HubIdx[i][assignments.get(i)];
            diffLoadAtHub[idxHub]+=listCustomers.get(i).third();
            totalDistance+=matrixDistance[i][idxHub];
        }

        for (int i=0;i<nHubs;++i){
            diffLoadAtHub[i]-=listHubs.get(i).third();
            violation+=Math.max(0,diffLoadAtHub[i]);
        }
        obj = calObjective(totalDistance,violation);
    }

    protected double calObjective(double totalDistance, int violation){
        return totalDistance+W*violation;
    }

    /**
     * find the best neighbor
     * @param tabu array of tabu
     * @return moveInfo a Triplet including integer:
     *                 first: index of customer
     *                 second: index of the old hub assigned to this customer
     *                 third: index of the new hub assigned to this customer
     */
    protected Triplet<Integer,Integer,Integer> findBestNeighbor(int[] tabu){
        int selectCusIdx=-1;
        int selectOldHub = -1;
        int selectNewHub =-1;

        double neighBestObj = Double.POSITIVE_INFINITY;

        final int N_NN = top5HubIdx[0].length;
        int oldHub;
        for (int cusIdx=0;cusIdx<nCustomers;++cusIdx){
            if (tabu[cusIdx]>0){
                continue;
            }
            oldHub = assignments.get(cusIdx);
            for (int newHub=0;newHub<N_NN;++newHub){
                if (newHub!=oldHub){
                    updateLoadAndViolationAndObj(new Triplet<>(cusIdx,oldHub,newHub));
                    if (obj<neighBestObj){
                        neighBestObj = obj;
                        selectCusIdx = cusIdx;
                        selectOldHub = oldHub;
                        selectNewHub = newHub;
                    }
                    updateLoadAndViolationAndObj(new Triplet<>(cusIdx,newHub,oldHub));
                }
            }
        }
        return new Triplet<>(selectCusIdx,selectOldHub,selectNewHub);
    }

    public String getStatus(){
        return (violation==0)?"FEASIBLE":"INFEASIBLE";
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object clone() {
        Solution cloned;
        try {
            cloned = (Solution) super.clone();
        } catch (CloneNotSupportedException e){
            // this shouldn't happen, since we are Cloneable
            System.out.println("Error When Cloning");
            throw new InternalError(e);
        }

        cloned.assignments = (List<Integer>) ((ArrayList<Integer>)this.assignments).clone();
        cloned.diffLoadAtHub = this.diffLoadAtHub.clone();
        return cloned;
    }
}
