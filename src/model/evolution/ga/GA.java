package model.evolution.ga;

import model.AbstractModel;
import service.Triplet;
import service.Utils;

import java.util.List;
import java.util.Objects;

public class GA extends AbstractModel {
    private static int NUM_GENERATION, NUM_INDIVIDUAL;
    private static void setNumGeneration(int nHub,int nCustomer){
        NUM_GENERATION = Math.max(50,Math.min(1000,100*Math.max(nHub/nCustomer,nCustomer/nHub)));
    }

    private static void setWeight(double maxDistance, int nHub, int nCustomer){
        GAIndividual.W = (int)maxDistance*2*Math.max(nHub/nCustomer,nCustomer/nHub);
    }

    private static void setNumIndividual(int nHubs, int nCustomers){
        NUM_INDIVIDUAL = Math.max(50,Math.min(500,nHubs*nCustomers));
    }

    public GA(){
        this.modelName = "GA";
    }

    @Override
    protected void solve(List<Triplet<Double,Double,Integer>> listHubs,
                         List<Triplet<Double,Double,Integer>> listCustomers,
                         double[][] matrixDistance,
                         int[][] top5HubIdx) {
        // setup parameter
        GAIndividual.listHubs = inputInterface.getListHubs();
        GAIndividual.listCustomers=inputInterface.getListCustomers();
        GAIndividual.matrixDistance= Utils.calMatrixDistances(listHubs,listCustomers);
        GAIndividual.top5HubIdx= Utils.selectTop5Hubs(GAIndividual.matrixDistance);
        GAIndividual.nHubs=GAIndividual.listHubs.size();
        GAIndividual.nNeighbors=GAIndividual.top5HubIdx[0].length;
        GAIndividual.nGen = GAIndividual.listCustomers.size();

        setNumGeneration(GAIndividual.nHubs,GAIndividual.nGen);
        double maxDistance = Utils.getMaxDistance(matrixDistance,top5HubIdx);
        setWeight(maxDistance,GAIndividual.nHubs,GAIndividual.nGen);
        setNumIndividual(GAIndividual.nHubs,GAIndividual.nGen);

        GAPopulation population = new GAPopulation(NUM_INDIVIDUAL);

        System.out.println("#Generations="+NUM_GENERATION);
        System.out.println("#Individual="+NUM_INDIVIDUAL);
        int it=0;
        while ((it<NUM_GENERATION)&&(!isTimeUp)) {
            it++;
            population.evolutePopulation();
            population.selection();
        }

//        System.out.println(population.getPopulation().toString());
        GAIndividual best = population.getBest();
        status = best.status;
        if (Objects.equals(status, "INFEASIBLE")){
            bestDistance=-1;
            this.assignments=null;
            System.out.println("GA cannot find any solutions");
        } else {
            this.assignments = best.getAssignment();
            bestDistance = best.getTotalDistance();
            System.out.println("Best Distance is " + bestDistance);
            System.out.println("Assigment is: " + assignments.toString());
        }

    }
}
