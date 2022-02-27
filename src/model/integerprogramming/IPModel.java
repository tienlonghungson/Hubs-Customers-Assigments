package model.integerprogramming;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import model.AbstractModel;
import service.Triplet;

import java.util.ArrayList;
import java.util.List;

public class IPModel extends AbstractModel {
    public IPModel(){
        this.modelName = "IPSolver";
    }

    @Override
    protected  void solve(List<Triplet<Double,Double,Integer>> listHubs,
                          List<Triplet<Double,Double,Integer>> listCustomers,
                          double[][] matrixDistance,
                          int[][] top5HubIdx) {
        final int N_HUBS = inputInterface.getNumHubs();
        final int N_CUSTOMERS = inputInterface.getNumCustomers();

        MPSolver solver = MPSolver.createSolver("SCIP");
        if (solver == null) {
            System.out.println("Could not create solver SCIP");
            return;
        }
        solver.setTimeLimit(TIME_LIMIT_IN_MINUTE *60000);


        final int N_NN = top5HubIdx[0].length; // # nearest neighbors

        MPVariable[][] x = new MPVariable[N_CUSTOMERS][N_NN];
        for (int i=0;i<N_CUSTOMERS;++i){
            for (int j = 0; j < N_NN; j++) {
                x[i][j] = solver.makeBoolVar("x["+i+"]["+j+"]");
            }
        }

        // constraint (1): each customer belongs to exactly one hub
        for (int i=0;i<N_CUSTOMERS;++i){
            MPConstraint mpConstraint1 = solver.makeConstraint(1,1,"cus "+i+" belongs to only 1 hub");
            for (int j = 0; j <N_NN ; j++) {
                mpConstraint1.setCoefficient(x[i][j],1);
            }
        }

        // constraint (2): max load at each hub
        MPConstraint[] mpConstraints = new MPConstraint[N_HUBS];
        for (int i=0;i<N_HUBS;++i){
            mpConstraints[i] = solver.makeConstraint(
                    Long.MIN_VALUE,listHubs.get(i).third(),"max load of hub "+i);
        }

        int k;
        for (int i=0;i<N_CUSTOMERS;++i){
            for (int j=0;j<N_NN;++j){
                k = top5HubIdx[i][j];
                mpConstraints[k].setCoefficient(x[i][j],listCustomers.get(i).third());
            }
        }

        // set up objective and calculate total weight of the graph at the same time
        MPObjective objective = solver.objective();
        for (int i=0;i<N_CUSTOMERS;++i){
            for (int j=0;j<N_NN;++j){
                objective.setCoefficient(x[i][j],matrixDistance[i][top5HubIdx[i][j]]);
            }
        }

        objective.setMinimization();
        System.out.println("IP Running");
        final MPSolver.ResultStatus  resultStatus = solver.solve();
        System.out.println("IP Finished");

        switch (resultStatus){
            case OPTIMAL -> this.status = "OPTIMAL";
            case FEASIBLE -> this.status = "FEASIBLE";
            case INFEASIBLE -> this.status = "INFEASIBLE";
            default -> this.status="NOT_SOLVED";
        }

        if (resultStatus== MPSolver.ResultStatus.INFEASIBLE){
            bestDistance=-1;
            assignments = null;
            System.out.println("IP_Solver cannot find any solutions");
        } else {
            bestDistance = objective.value();
            assignments = new ArrayList<>(N_CUSTOMERS);
            for (int i = 0; i < N_CUSTOMERS; ++i) {
                for (int j = 0; j < N_NN; ++j) {
                    if (x[i][j].solutionValue() == 1) {
                        assignments.add(top5HubIdx[i][j]);
                        break;
                    }
                }
            }
            System.out.println("Best Distance is " + bestDistance);
            System.out.println("Assigment is: " + assignments.toString());
        }
    }
}
