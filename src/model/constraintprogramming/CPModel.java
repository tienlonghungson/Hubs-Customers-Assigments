package model.constraintprogramming;

import com.google.ortools.sat.*;
import model.AbstractModel;
import service.Triplet;

import java.util.ArrayList;
import java.util.List;

public class CPModel extends AbstractModel {
    public CPModel(){
        this.modelName = "CPSolver";
    }

    @Override
    protected void solve(List<Triplet<Double,Double,Integer>> listHubs,
                         List<Triplet<Double,Double,Integer>> listCustomers,
                         double[][] matrixDistance,
                         int[][] top5HubIdx) {
        final int N_CUSTOMERS = inputInterface.getNumCustomers();
        final int N_HUBS = inputInterface.getNumHubs();

        final int N_NN = top5HubIdx[0].length; // # nearest neighbors

        CpModel cpModel = new CpModel();

        IntVar[][] x= new IntVar[N_CUSTOMERS][N_NN];
        IntVar[] xFlatten = new IntVar[N_CUSTOMERS*N_NN];
        long[] matrixDistanceFlatten = new long[N_CUSTOMERS*N_NN];
        for (int i=0;i<N_CUSTOMERS;++i){
            for (int j = 0; j < N_NN; j++) {
                x[i][j] = cpModel.newBoolVar("x["+i+"]["+j+"]");
                xFlatten[i*N_NN+j] = x[i][j];
                matrixDistanceFlatten[i*N_NN+j]= Math.round(matrixDistance[i][top5HubIdx[i][j]]);
            }
        }

        // constraint (1): each customer belongs to exactly one hub
        for (int i=0;i<N_CUSTOMERS;++i){
            cpModel.addEquality(LinearExpr.sum(x[i]),1);
        }

        // constraint (2): max load at each hub
        List<List<IntVar>> allowedCusAtHub = new ArrayList<>();
        List<List<Integer>> correspondLoad = new ArrayList<>();
        for (int i=0;i<N_HUBS;++i){
            allowedCusAtHub.add(new ArrayList<>());
            correspondLoad.add(new ArrayList<>());
        }
        int k;
        for (int i=0;i<N_CUSTOMERS;++i){
            for (int j=0;j<N_NN;++j){
                k = top5HubIdx[i][j];
                allowedCusAtHub.get(k).add(x[i][j]);
                correspondLoad.get(k).add(listCustomers.get(i).third());
            }
        }

        for (int i=0;i<N_HUBS;++i){
            cpModel.addLessOrEqual(
                    LinearExpr.scalProd(
                            allowedCusAtHub.get(i).toArray(new IntVar[0]),
                            correspondLoad.get(i).stream().mapToInt(j -> j).toArray()
                    ),
                    listHubs.get(i).third());
        }


        cpModel.minimize(LinearExpr.scalProd(xFlatten,matrixDistanceFlatten));
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(TIME_LIMIT_IN_MINUTE *60);
        CpSolverStatus resultStatus = solver.solve(cpModel);

        switch (resultStatus){
            case OPTIMAL -> this.status = "OPTIMAL";
            case FEASIBLE -> this.status = "FEASIBLE";
            case INFEASIBLE -> this.status = "INFEASIBLE";
            default -> this.status="NOT_SOLVED";
        }

        if (resultStatus==CpSolverStatus.INFEASIBLE){
            bestDistance=-1;
            assignments=null;
            System.out.println("CP_Solver cannot find any solutions");
        } else {
            bestDistance=0;
            assignments = new ArrayList<>(N_CUSTOMERS);
            for (int i = 0; i < N_CUSTOMERS; ++i) {
                for (int j = 0; j < N_NN; ++j) {
                    if (solver.value(x[i][j]) == 1) {
                        assignments.add(top5HubIdx[i][j]);
                        bestDistance+=matrixDistance[i][top5HubIdx[i][j]];
                        break;
                    }
                }
            }
            System.out.println("Best Distance is " + bestDistance);
            System.out.println("Assigment is: " + assignments.toString());
        }
    }
}
