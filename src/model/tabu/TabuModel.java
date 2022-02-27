package model.tabu;

import model.AbstractModel;
import service.Triplet;
import service.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class TabuModel extends AbstractModel {
    private static int NUM_GENERATION;

    private static void setNumGeneration(int nHub,int nCustomer){
        NUM_GENERATION = Math.max(50,Math.min(1500,100*Math.max(nHub/nCustomer,nCustomer/nHub)));
    }

    private static void setWeight(double maxDistance, int nHub, int nCustomer){
        W = (int)maxDistance*2*Math.max(nHub/nCustomer,nCustomer/nHub);
    }

    private int tbl=3;

    Solution currSol, bestSol, lastImprovedSol;

    private static int W;

    public TabuModel(){
        this.modelName = "TabuSearch";
    }

    /**
     * generate a random solution
     * @param nHubOr5 5 or maximum amount of hubs near a customer
     * @param nCustomer number of customer
     * @return random solution
     */
    private Solution genSolution(int nHubOr5, int nCustomer){
        List<Integer> assignments = new ArrayList<>(nCustomer);
        Random rd = new Random();
        for (int i=0;i<nCustomer;++i){
            assignments.add(rd.nextInt(nHubOr5));
        }
        return new Solution(assignments);
    }

    @Override
    protected void solve(List<Triplet<Double,Double,Integer>> listHubs,
                         List<Triplet<Double,Double,Integer>> listCustomers,
                         double[][] matrixDistance,
                         int[][] top5HubIdx) {
        Solution.listHubs=listHubs;
        Solution.listCustomers=listCustomers;
        Solution.matrixDistance=matrixDistance;
        Solution.top5HubIdx=top5HubIdx;
        final int N_CUSTOMERS= listCustomers.size();
        final int N_HUBS = listHubs.size();
        Solution.nCustomers= N_CUSTOMERS;
        Solution.nHubs=N_HUBS;

        double maxDistance = Utils.getMaxDistance(matrixDistance,top5HubIdx);
        setWeight(maxDistance,N_HUBS,N_CUSTOMERS);
        Solution.W = W;

        int[] tabu = new int[N_CUSTOMERS];
        final int TB_MIN=2, TB_MAX=5;

        currSol = genSolution(top5HubIdx[0].length,N_CUSTOMERS);
        double bestObj = Double.POSITIVE_INFINITY;
        double oldObj;

        setNumGeneration(N_HUBS,N_CUSTOMERS);
        int it=0;
        int stable=0, stableLimit=30;
        int restartFreq=100;

        System.out.println("#Generations="+NUM_GENERATION);
        while ((it<NUM_GENERATION)&&(!isTimeUp)){
            it++;
            if (currSol.obj<bestObj){
                bestObj=currSol.obj;
                bestSol=(Solution)currSol.clone();
                stable=0;
            } else if (stable==stableLimit){
                currSol=(Solution) lastImprovedSol.clone();
                stable=0;
            } else {
                stable++;
                if (it%restartFreq==0){
                    currSol = genSolution(top5HubIdx[0].length,N_CUSTOMERS);
                    Arrays.fill(tabu,0);
                }
            }

            oldObj=currSol.obj;
            Triplet<Integer,Integer,Integer> moveToNext = currSol.findBestNeighbor(tabu);
            if (moveToNext.first()==-1||
                    moveToNext.second()==-1||
                    moveToNext.third()==-1){
                currSol = genSolution(top5HubIdx[0].length,N_CUSTOMERS);
                continue;
            }
            currSol.update(moveToNext);
            for (int i = 0; i< tabu.length; ++i){
                if (tabu[i]>0){
                    tabu[i]--;
                }
            }

            tabu[moveToNext.first()]= tbl;
            if (currSol.obj<oldObj){
                if (tbl>TB_MIN){
                    tbl--;
                }

                lastImprovedSol = (Solution) currSol.clone();
                stable=0;
            } else {
                if (tbl<TB_MAX){
                    tbl++;
                }
            }
        }

        // return result
        if (bestSol.violation!=0){
            this.status="INFEASIBLE";
            bestDistance=-1;
            assignments = null;
            System.out.println("TabuSearch cannot find any solutions");
        } else {
            this.status="FEASIBLE";
            bestDistance=bestSol.totalDistance;
            assignments=Utils.convertAssignmentsIdxFromTop5(bestSol.assignments,top5HubIdx);
            System.out.println("Best Distance is " + bestDistance);
            System.out.println("Assigment is: " + assignments.toString());
        }
    }
}
