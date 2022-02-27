package model.evolution.ga;

import model.evolution.Individual;
import service.Pair;
import service.Triplet;
import service.Utils;

import java.util.*;

public class GAIndividual extends Individual {
    static {
        numObjective=0;
    }

    /**
     * nGen : number of genes
     */
    protected static int nGen;
    protected static int nHubs, nNeighbors;

    /**
     * weighted matrix of graph
     */
    protected static double[][] matrixDistance;
    /**
     * an array containing index of top 5 nearest hubs to each customer
     */
    protected static int[][] top5HubIdx;

    /**
     * list of hubs and customers, containing their coords and load
     */
    protected static List<Triplet<Double,Double,Integer>> listHubs, listCustomers;
    /**
     * weight for objective (fitness)
     */
    protected static int W;

    protected static final double R_MUTATE1=0.06, R_MUTATE2=0.08, R_MUTATE3=0.1;
    protected int[] genes;
    private double totalDistance;
    protected String status;
    /**
     * constructor
     * @param isAutoGen whether this individual is auto generated or not (offspring, mutated)
     */
    public GAIndividual(boolean isAutoGen){
        super(nGen);
        this.genes = new int[nGen];

        if (isAutoGen) {
            Random rd = new Random();
            for (int i=0;i<nGen;++i){
                this.genes[i]=rd.nextInt(nNeighbors);
                // add inCost of partition containing i vertex
            }
            this.evaluate();
        }
    }

    public void setGene(int idxGen, int idxPart){
        assert (idxGen<=N_GEN);
        this.genes[idxGen]=idxPart;
    }

    protected double getTotalDistance(){
        return totalDistance;
    }

    protected void evaluate(){
        totalDistance=0;
        int violation=0;

        long[] hubLoads= new long[nHubs];
        for (int i=0;i<nGen;++i){
            hubLoads[top5HubIdx[i][genes[i]]] += listCustomers.get(i).third();
            totalDistance+=matrixDistance[i][top5HubIdx[i][genes[i]]];
        }
        for (int i=0;i<nHubs;++i){
            violation+=Math.max(0,hubLoads[i]-listHubs.get(i).third());
        }

        if (violation>0){
            status="INFEASIBLE";
        } else {
            status="FEASIBLE";
        }
        setFitness(totalDistance+W*violation);
    }

    public Pair<Individual, Individual> onePointCrossOver(GAIndividual other){
        Random rd = new Random();
        int point = rd.nextInt((N_GEN-1))+1;

        GAIndividual child1 = new GAIndividual(false);
        GAIndividual child2 = new GAIndividual(false);

        int idxPart1, idxPart2;
        for (int i=0;i<point;++i){
            idxPart1 = this.genes[i];
            idxPart2 = other.genes[i];
            idxPart1 = (idxPart1+idxPart2)/2;
            child1.setGene(i,idxPart1);
            child2.setGene(i,idxPart2);
        }

        for (int i=point;i<N_GEN;++i){
            idxPart1 = other.genes[i];
            idxPart2 = (this.genes[i]+idxPart1)/2;
            child1.setGene(i,idxPart1);
            child2.setGene(i,idxPart2);
        }
        child1.evaluate();
        child2.evaluate();
        return new Pair<>(child1,child2);
    }

    protected void mutate(double rMutate){
        if(rMutate<GAIndividual.R_MUTATE1){
            this.onePointMutate();
        } else if (rMutate<GAIndividual.R_MUTATE2){
            this.reversedMutation();
        } else if (rMutate< GAIndividual.R_MUTATE3){
            this.swapHalfMutation();
        } else {
            return;
        }
        evaluate();
    }

    private void onePointMutate(){
        Random rd = new Random();
        int mutatePoint = rd.nextInt(N_GEN);
        int mutateValue = rd.nextInt(GAIndividual.nNeighbors);
        this.genes[mutatePoint]=mutateValue;
    }

    private void reversedMutation(){
        for (int i=0;i<N_GEN;++i){
            genes[i]=genes[i]^genes[N_GEN-1-i];
            genes[N_GEN-1-i]=genes[i]^genes[N_GEN-1-i];
            genes[i]=genes[i]^genes[N_GEN-1-i];
        }
    }

    private void swapHalfMutation(){
        final int DIS = (N_GEN-1)>>1;
        for (int i=0;i<DIS;++i){
            genes[i]=genes[i]^genes[i+DIS];
            genes[N_GEN-1-i]=genes[i]^genes[i+DIS];
            genes[i]=genes[i]^genes[i+DIS];
        }
    }

    @Override
    public boolean isDominated(Individual other) {
        return this.getFitness()>other.getFitness();
    }

    @Override
    public List<Integer> getAssignment() {
        return Utils.convertAssignmentsIdxFromTop5(genes,top5HubIdx);
    }
}
