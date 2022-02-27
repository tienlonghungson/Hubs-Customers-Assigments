package model;

import io.InputInterface;
import io.TripletBaseInput;
import service.Pair;
import service.Triplet;
import service.Utils;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AbstractModel {
    /*------------- timer setup----------------------*/
    protected final int TIME_LIMIT_IN_MINUTE = 5;
    protected double timeElapsed;
    /**
     * used to set time limit for solving
     */
    protected boolean isTimeUp;

    /**
     * set timer
     */
    ScheduledExecutorService timer;

    /*------------- solution's parameters-------------*/
    protected List<Integer> assignments;
    protected double bestDistance;
    /*------------- auxiliary parameter----------------*/
    /**
     * status of solution ( Optimal, Feasible, Infeasible)
     */
    protected String status;

    /**
     * number of hubs is violated by assigment
     */
    protected int nViolation;

    /**
     * total violation across all the violated hubs
     */
    protected int weightViolation;

    /*--------------- model information-----------------*/
    protected InputInterface inputInterface;
    protected String modelName;

    public String getModelName() {
        return modelName;
    }

    /**
     * read input data from file
     *
     * @param fileName string contains name of file
     * @throws IOException {@code FileNotFoundException} or {@code NullPointerException}
     */
    protected void readInput(String fileName) throws IOException {
        inputInterface = new TripletBaseInput(fileName);
    }

    /**
     * read input data from {@code file}
     *
     * @param file object reference to data input
     * @throws IOException {@code FileNotFoundException} or {@code NullPointerException}
     */
    protected void readInput(File file) throws IOException {
        inputInterface = new TripletBaseInput(file);
    }

    /**
     * setup result file
     * doing nothing
     * override at will
     *
     * @param filename name of file
     * @param isAppend set to {@code false} if starts a new file, {@code true} if ends a file
     */
    protected void setupResultFile(String filename, boolean isAppend) {
    }

    /**
     * write result of each run to specified files
     *
     * @param filename (recommended including _x.txt , x is the ith run (if nRun >1) )
     */
    protected void writeResult(String filename) {
        try {
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename, true)));

            printWriter.printf("%.2f\n", bestDistance);
            if (this.assignments!=null) {
                for (int hubIdx : this.assignments) {
                    printWriter.printf("%d ", hubIdx);
                }
            }
            printWriter.printf("\n");
            printWriter.close();
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println(fileNotFoundException.getMessage());
            fileNotFoundException.printStackTrace();
        }
    }

    /**
     * write log the results of the whole runs
     *
     * @param filename (recommended csv file)
     * @param dataName name of input Data
     * @param runIdx   the index of this run
     */
    protected void writeLog(String filename, String dataName, int runIdx) {
        try {
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename, true)));
            // columns: data, nHubs, nCustomers, run idx, result,violation time, status
            printWriter.printf("%s,%d,%d,%d,%.2f,%d,%d,%.2f,%s\n",
                    dataName, inputInterface.getNumHubs(), inputInterface.getNumCustomers(), runIdx,
                    this.bestDistance, this.nViolation, this.weightViolation, this.timeElapsed, this.status);
            printWriter.close();
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println(fileNotFoundException.getMessage());
            fileNotFoundException.printStackTrace();
        }
    }

    /**
     * implement a specific algorithm
     */
    protected abstract void solve(List<Triplet<Double,Double,Integer>> listHubs,
                                  List<Triplet<Double,Double,Integer>> listCustomers,
                                  double[][] matrixDistance,
                                  int[][] top5HubIdx);

    /**
     * prepare to solve, start timing
     *
     * @param dataName name of input
     */
    protected void run(String dataName) {
        System.out.printf("Start Solving %s with #Hubs=%d, #Customers=%d\n", dataName,
                inputInterface.getNumHubs(), inputInterface.getNumCustomers());

        timer = Executors.newSingleThreadScheduledExecutor();
        timer.schedule(new Timer(this), TIME_LIMIT_IN_MINUTE, TimeUnit.MINUTES);
        isTimeUp = false;

        List<Triplet<Double,Double,Integer>> listHubs = inputInterface.getListHubs();
        List<Triplet<Double,Double,Integer>> listCustomers = inputInterface.getListCustomers();

        double[][] matrixDistance = Utils.calMatrixDistances(listHubs,listCustomers);
        int[][] top5HubIdx = Utils.selectTop5Hubs(matrixDistance);

        this.timeElapsed = System.currentTimeMillis();
        this.solve(listHubs,listCustomers,matrixDistance,top5HubIdx);
        this.timeElapsed = System.currentTimeMillis() - this.timeElapsed;

        insightResult();
        stop();
    }

    /**
     * check if the status of the solution is INFEASIBLE
     * if so, calculate the violation
     */
    protected void insightResult() {
        this.nViolation = 0;
        this.weightViolation = 0;
        if (status.equals("INFEASIBLE")) {
            if (this.assignments!=null) {
                Pair<Integer, Integer> violation = Utils.calViolation(
                        this.assignments,
                        inputInterface.getListHubs(),
                        inputInterface.getListCustomers());
                this.nViolation = violation.first();
                this.weightViolation = violation.second();
            }
        }
    }

    /**
     * stop timer
     */
    protected void stop() {
        timer.shutdownNow();
        isTimeUp = true;
    }

    private static final String inputFolder = "data/input/";
    private static final String outputFolder = "data/output/";
    private static final String logFolder = "data/log/";

    public static void createLogFile(String filename) throws FileNotFoundException {
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename)));
        // columns: data, #hubs, #customers, run idx, result, #violation, violation, time, status
        printWriter.printf("data,#hubs,#customers,run idx,result,#violation,violation,time,status\n");
        printWriter.close();
    }

    /**
     * @param model     specific algorithm
     * @param nRun      number of run time
     * @param dataTypes specify the dataset :
     *                  (small, medium, large, test),
     *                  (dense, sparse),
     *                  (distribution:Gauss, uniform)
     */
    public static void execute(AbstractModel model, int nRun, String... dataTypes) {
        StringBuilder inputDir = new StringBuilder(inputFolder);
        StringBuilder outputFileName = new StringBuilder(outputFolder + model.getModelName() + "/");
        StringBuilder logFileName = new StringBuilder(logFolder + model.getModelName());
        for (String dataType : dataTypes) {
            inputDir.append(dataType).append("/");
            outputFileName.append(dataType).append("/");
            logFileName.append("_").append(dataType);
        }


        File dir = new File(inputDir.toString());

        try {
            logFileName.append(".csv");
            createLogFile(logFileName.toString());
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.printf("Program stopped because log file %s cannot be created\n%n", logFileName);
            return;
        }

        for (File f : Objects.requireNonNull(dir.listFiles())) {
//		File[] files = {new File("data/input/test/data_12_distance.txt")};
//		for (File f: files){
            String dataName;
            if (!f.isDirectory() && (dataName = f.getName()).endsWith(".txt")) {
                dataName = dataName.substring(0, dataName.lastIndexOf(".txt")); // remove postfix .txt
                outputFileName.append("res_").append(dataName).append(".txt");

                try {
                    model.readInput(f);
                    model.setupResultFile(outputFileName.toString(), false);
                    for (int i = 0; i < nRun; ++i) {
                        model.run(dataName);

                        model.writeResult(outputFileName.toString());
                        model.writeLog(logFileName.toString(), dataName, i);
                    }
                    model.setupResultFile(outputFileName.toString(), true);
                } catch (IOException ioException) {
                    System.out.println("Error when reading data from " + dataName);
                    System.out.println(ioException.getMessage());
                    ioException.printStackTrace();
                }

                /* remove the name of the previous result file ,
                4 is the length of prefix "res_"
                4 is the length of extension ".txt"*/
                outputFileName.delete(outputFileName.length()- 4 - dataName.length() - 4, outputFileName.length());
            }
        }
    }

}
