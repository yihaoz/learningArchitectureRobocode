package rsa;

import java.io.*;
import java.util.*;

import org.jfree.ui.RefineryUtilities;

public class NeuralNet implements NeuralNetInterface {

    private int numOfInputs;
    private int numOfHidden;
    private double learningRate;
    private double momentum;
    private double lowerBound;
    private double upperBound;
    public double[][] inputWeights; // first dimension is hidden neuron number, second dimension is input
    private double[][] prevInputWeights;
    public double[] outputWeights;
    private double[] prevOutputWeights;
    private double[][] values;
    private boolean isSigmoidCustom; // true for custom sigmoid with binary representation, false for sigmoid with bipolar representation

    private String name;

    /**
     * Constructor. (Cannot be declared in an interface, but your implementation will need one)
     * @param argNumInputs The number of inputs in your input vector
     * @param argNumHidden The number of hidden neurons in your hidden layer. Only a single hidden layer is supported * @param argLearningRate The learning rate coefficient
     * @param argMomentumTerm The momentum coefficient
     * @param argA Integer lower bound of sigmoid used by the output neuron only.
     * @param argB Integer upper bound of sigmoid used by the output neuron only.
     * **/
    public NeuralNet ( int argNumInputs, int argNumHidden,
                       double argLearningRate, double argMomentumTerm, double argA,
                       double argB ){
        int numOfHiddenLayer = 1;
        numOfInputs     = argNumInputs;
        numOfHidden     = argNumHidden;
        learningRate    = argLearningRate;
        momentum        = argMomentumTerm;
        lowerBound      = argA;
        upperBound      = argB;

        inputWeights    = new double[numOfHidden+1][numOfInputs+1];
        prevInputWeights    = new double[numOfHidden+1][numOfInputs+1];
        prevOutputWeights   = new double[numOfHidden+1];
        outputWeights   = new double[numOfHidden+1];

        values          = new double[numOfHiddenLayer+2][Math.max(numOfHidden, numOfInputs)+1];

    }

    @Override
    public double outputFor(double[] X) {
        //clear neurons values
        for (double[] row: values)
            Arrays.fill(row, 0.0);

        // For Bias
        for(int i = 0; i < 2; i++){
            values[i][0] = bias;
        }

        // For input values
        for(int i = 1; i < numOfInputs + 1; i++){
            values[0][i] = X[i-1];
        }

        // For hidden neuron values
        for(int i = 0; i < numOfHidden; i++){
            for(int j = 0; j < numOfInputs + 1; j++){
                values[1][i+1] += inputWeights[i+1][j] * values[0][j];
            }
            if(isSigmoidCustom == true) values[1][i+1] = customSigmoid(values[1][i+1]);
            else values[1][i+1] = sigmoid(values[1][i+1]);
        }

        //For output layer
        for(int i = 0; i < numOfHidden+1; i++){

            values[2][0] +=  outputWeights[i] * values[1][i];
        }

        if(isSigmoidCustom == true) return customSigmoid(values[2][0]);
        else return sigmoid(values[2][0]);
    }

    @Override
    public double train(double[] X, double argValue) {
        // For hidden-to-output layer
        double actualOutput = outputFor(X);

        double err = 0.5 * Math.pow((actualOutput - argValue), 2);

        double derivative = getDerivative(isSigmoidCustom, actualOutput);

        double outputDelta = derivative * (argValue - actualOutput);

        for(int i = 0; i < numOfHidden + 1; i++){
            double updatedOutputWeight = updateWeight(outputWeights[i], prevOutputWeights[i], learningRate, outputDelta, values[1][i]);
            prevOutputWeights[i] = outputWeights[i];
            outputWeights[i] = updatedOutputWeight;
        }

        // For input-to-hidden layer
        double[] hiddenDelta = new double[numOfHidden + 1];
        for(int i = 1; i < numOfHidden + 1; i++){
            //Bias term does not need delta
            derivative = getDerivative(isSigmoidCustom, values[1][i]);
            hiddenDelta[i] = derivative * outputDelta * outputWeights[i];
        }

        for(int i = 1; i < numOfHidden + 1; i++){
            for(int j = 0; j < numOfInputs + 1; j++){
                double updatedInputWeight = updateWeight(inputWeights[i][j], prevInputWeights[i][j], learningRate, hiddenDelta[i], values[0][j]);
                prevInputWeights[i][j] = inputWeights[i][j];
                inputWeights[i][j] = updatedInputWeight;
            }
        }

        return err;
    }

    // Weight update without the momentum term
    public double updateWeight(double originalWeight, double prevWeight, double rate, double delta, double input){

        return  (originalWeight + rate * delta * input + momentum * (originalWeight - prevWeight));
    }

    // Get Output layer delta based on the type of sigmoid functions used and the output
    public double getDerivative(boolean isSigmoidCustom, double output){
        if(isSigmoidCustom) return output * (1 - output);
        else return 0.5  * (1 - Math.pow(output, 2));
    }

    public double getError(double[][] inputs, double[] outputs, int numOfSets){
        double sqr_error = 0;
//        double error = 0;
//        int monitored = 1;

        for(int i = 0; i < numOfSets; i++){
            train(inputs[i], outputs[i]);
        }

        for(int i = 0; i < numOfSets; i++){
            double actualOutput = outputFor(inputs[i]);
            sqr_error += Math.pow(outputs[i] - actualOutput, 2);
//            if(i == monitored){
//                error += (actualOutput - outputs[i]);
//                System.out.println("Expected output is " + outputs[i] + " actual output is " + actualOutput);
//            }
        }
//        System.out.println("average error for selected training pattern is " + 100*(error/numOfSets)/outputs[monitored] + "%");
        return Math.sqrt(sqr_error/numOfSets);
    }

    @Override
    public void save(File argFile) {
        try {
            FileWriter writer = new FileWriter(argFile);

            // Write the hidden-to-output weights, in this case 5 weights
            for(int i = 0; i < outputWeights.length; i++){
                writer.write(String.valueOf(outputWeights[i]) + " ");
            }

            //Write the input-to-hidden weights, in this case 12 weights
            for(int i = 0; i < numOfHidden + 1; i++){
                for(int j = 0; j < numOfInputs + 1; j++){
                    writer.write(String.valueOf(inputWeights[i][j]) + " ");
                }
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void load(String argFileName) throws IOException {
        System.out.println("Loading Weights");
        File inputFile = new File(argFileName);
        try{
            Scanner scanner = new Scanner(inputFile);

            while(scanner.hasNextDouble()){
                for(int i = 0; i < numOfHidden + 1; i++){
                    if(!scanner.hasNextDouble()){
                        throw new IllegalAccessError("Weight file doesn't have enough weights");
                    }
                    outputWeights[i] = scanner.nextDouble();
                }
                for(int i = 0; i < numOfHidden + 1; i++){
                    for(int j = 0; j < numOfInputs + 1; j++){
                        if(!scanner.hasNextDouble()){
                            throw new IllegalAccessError("Weight file doesn't have enough weights");
                        }
                        inputWeights[i][j] = scanner.nextDouble();
                    }
                }
                if(scanner.hasNext()) {
                    throw new IllegalAccessError("Weight file contains more weights than the dimensions");
                }
            }
        } catch(FileNotFoundException e){
            e.printStackTrace();
        }
    }

    @Override
    // Modified sigmoid for bipolar
    public double sigmoid(double x) {
        return 2 / (1 + Math.pow(Math.E, -x)) - 1;
    }

    @Override
    public double customSigmoid(double x) {
        return (upperBound - lowerBound) / (1 + Math.pow(Math.E, -x)) + lowerBound;
    }

    @Override
    public void initializeWeights() {
        Random r = new Random();
        for (int i = 0; i < numOfHidden + 1; i++) {
            for(int j = 0; j < numOfInputs + 1; j++){
                inputWeights[i][j] = (-0.5 + r.nextDouble());
                prevInputWeights[i][j] = inputWeights[i][j];
            }
        }
        for (int i = 0; i < numOfHidden + 1; i++) {
            outputWeights[i] = (-0.5 + r.nextDouble());
            prevOutputWeights[i] = outputWeights[i];
        }
    }

    @Override
    public void zeroWeights() {
        Arrays.fill(outputWeights, 0.0);
        for(double[] row: inputWeights) Arrays.fill(row, 0.0);
    }

    public void readInputOutput(String filename, List<double[]> inputs, List<Double> outputs) throws IOException{
        try {
            BufferedReader in = new BufferedReader(new FileReader((filename)));
            String stringBuffer;

            int lineCounter = 0;
            while ((stringBuffer = in.readLine()) != null) {
                String[] strs = stringBuffer.trim().split("\\s+");
                if (strs.length != 5)
                    throw new ArrayIndexOutOfBoundsException("Current line doesn't have 6 states + 1 Qvalue");
                double[] strsToDouble = new double[5];
                for (int i = 0; i < 5; i++) {
                    strsToDouble[i] = Double.parseDouble(strs[i]);
                }
                double[] tmp_input = Arrays.copyOfRange(strsToDouble, 0, 4);
                inputs.add(tmp_input);
                outputs.add(this.sigmoid(strsToDouble[4]));
                lineCounter++;
            }
        }
        catch(IOException e){
            System.out.println("file exception");
        }
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return this.name;
    }

    public static void main(String[] args) {

        double[][] binaryInputs = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
        double[][] bipolarInputs = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        double[] binaryOutputs = {0, 1, 1, 0};
        double[] bipolarOutputs = {-1, 1, 1, -1};

        boolean useBipolar = true;
        boolean useBinary = true;
        boolean useTestNN = false;
        double tolerance = 0.05;
        int maxNumOfTrials = 20000;


        List<double[]> inputList = new ArrayList<>();
        List<Double> outputList = new ArrayList<>();
        double[][] inputs;
        double[] outputs;

        if(useTestNN){
            NeuralNet testNN = new NeuralNet(4, 5, 0.005, 0.2, -1, 1);
            testNN.isSigmoidCustom = false;
            try{
                testNN.readInputOutput("action1fire.txt", inputList, outputList);
            }
            catch(IOException e){
            }

            // Convert arraylist to array
            if(inputList.size() != outputList.size()) throw new ArrayIndexOutOfBoundsException("input output size don't match");
            inputs = new double[inputList.size()][];
            outputs = new double[outputList.size()];
            for (int i = 0; i < inputList.size(); i++) {
                inputs[i] = inputList.get(i).clone();
                outputs[i] = outputList.get(i);
            }

            File testNNWeightFile = new File("TestNNWeightFile.txt");

            testNN.initializeWeights();

            System.out.println("Starting Binary Representation Training");

            PrintStream testNNErrFile = null;

            try {
                testNNErrFile = new PrintStream(new FileOutputStream("C:\\robocode\\robots\\rsa\\NNRobot\\TestNNError.txt", true));

                for (int i = 0; i < maxNumOfTrials; i++) {
                    double error = testNN.getError(inputs, outputs, inputList.size());
                    if(i%100 == 0){
                        System.out.println("Iteration " + i + " Error " + error);
                    }

                    testNNErrFile.println(i + " " + error);

                    if (error < tolerance || i == maxNumOfTrials-1) {
                        System.out.println("After epoch " + (i+1) + " the neural net reaches the tolerance");

                        writeResult("numOfEpochBipolar.txt", String.valueOf(i) + " ");

                        testNN.save(testNNWeightFile);

                        break;
                    }
                    if(i == maxNumOfTrials - 1) System.out.println("Binary representation reaches maximum number of trials!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(useBinary){
            NeuralNet binaryNN = new NeuralNet(2, 4, 0.2,
                    0.0, 0, 1);
            binaryNN.isSigmoidCustom = true;

            String binaryErrFile = "binaryErrFile.txt";
            File binaryWeightFile = new File("binaryWeightFile.txt");

            binaryNN.initializeWeights();

            System.out.println("Starting Binary Representation Training");

            try {
                FileOutputStream fos = new FileOutputStream(binaryErrFile);
                DataOutputStream dos = new DataOutputStream(fos);
                for (int i = 0; i < maxNumOfTrials; i++) {
                    double error = binaryNN.getError(binaryInputs, binaryOutputs, 4);

                    dos.writeDouble(error);

                    if (error < tolerance) {
                        System.out.println("After epoch " + (i+1) + " the neural net reaches the tolerance");

                        writeResult("numOfEpochBinary.txt", String.valueOf(i) + " ");

                        dos.close();
                        binaryNN.save(binaryWeightFile);

                        drawGraph("binary", binaryErrFile);

                        break;
                    }
                    if(i == maxNumOfTrials - 1) System.out.println("Binary representation reaches maximum number of trials!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(useBipolar){
            NeuralNet bipolarNN = new NeuralNet(2, 4, 0.2,
                    0.0, -1, 1);
            bipolarNN.isSigmoidCustom = false;

            String bipolarErrFile = "bipolarErrFile.txt";
            File bipolarWeightFile = new File("bipolarWeightFile.txt");
            bipolarNN.initializeWeights();

            System.out.println("Starting Bipolar Representation Training");

            try {
                FileOutputStream fos = new FileOutputStream(bipolarErrFile);
                DataOutputStream dos = new DataOutputStream(fos);
                for (int i = 0; i < maxNumOfTrials; i++) {
                    double error = bipolarNN.getError(bipolarInputs, bipolarOutputs, 4);

                    dos.writeDouble(error);

                    if (error < tolerance) {
                        System.out.println("After epoch " + i + " the neural net reaches the tolerance");

                        writeResult("numOfEpochBipolar.txt", String.valueOf(i) + " ");

                        dos.close();
                        bipolarNN.save(bipolarWeightFile);

                        drawGraph("bipolar", bipolarErrFile);

                        break;
                    }
                    if(i == maxNumOfTrials - 1) System.out.println("Bipolar representation reaches maximum number of trials!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static void drawGraph(String binaryOrBipolar, String fileOutput){
        if(binaryOrBipolar == "binary"){
            XYSeriesDemo binaryDemo = new XYSeriesDemo("Binary Total Error vs Num Of Epochs", fileOutput);
            drawXYPlot(binaryDemo);
        }
        else if(binaryOrBipolar == "bipolar"){
            XYSeriesDemo bipolarDemo = new XYSeriesDemo("Bipolar Total Error vs Num Of Epochs", fileOutput);
            drawXYPlot(bipolarDemo);
        }
        else{
            throw new IllegalArgumentException("Not a valid representation type, choose either binary or bipolar");
        }
    }

    static public void drawXYPlot(XYSeriesDemo plotDemo){
        plotDemo.pack();
        RefineryUtilities.centerFrameOnScreen(plotDemo);
        plotDemo.setVisible(true);
    }

    static void writeResult(String filename, String data){
        BufferedWriter bw = null;
        FileWriter fw = null;

        try {
            File file = new File(filename);

            // if file doesn't exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            // true = append file
            fw = new FileWriter(file.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);

            bw.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();

                if (fw != null)
                    fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
