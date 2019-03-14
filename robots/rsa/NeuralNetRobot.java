package rsa;
import robocode.*;
import com.sun.javafx.geom.Point2D;
import java.io.*;
import java.util.*;
import java.util.HashMap;

public class NeuralNetRobot extends AdvancedRobot {
    public double alpha = 0.1; // step size
    public double gamma = 0.9; // discount factor
    public boolean learn = true;
    public double learningRate = 0.005;
    public double momentum = 0.2;
    //    public boolean explore = true;
    public boolean isOnPolicy = true;
    public double explore_rate = 0.0;
    public int numOfHidden = 5;

    static NeuralNet action0Net;
    static NeuralNet action1Net;
    static NeuralNet action2Net;
    static NeuralNet action3Net;
    static NeuralNet action4Net;
    static NeuralNet action5Net;
    static NeuralNet action6Net;
    static NeuralNet action7Net;
    private int getResult = 500;
    private static int wins;
    private static int games;
    private static double max_win_rate;
    private static int winPer100;
    private static int gamePer100;

    private static double[][] tmp_output_weights;
    private static double[][][] tmp_input_weights;

    private static List<Double> winPercentage;
    private static List<Double> gameCounter;
    // pre-define 3 monitored states
    private static double[][] monitored_states= {{1.5, 1, 3, 3.5}, {0.5, 2, 4, 1}, {3.3, 3.3, 1.2, 1.2}};
    private static List<Double>[] eForDefinedState = new List[monitored_states.length];
    private double[] es = {0.0033291187026620594  , -0.016639151817959497  , -0.00770203384769963  };
    private static double[] prev_diff;
    private static ArrayList<Double> cumE = new ArrayList<>(); // Q(st) - Q(st-1)

    static ArrayList<NeuralNet> neuralNetList = new ArrayList<>();
    private double maxQ;
    private double curQ;
    private double prevQ;
    private double[] prev_state = new double[] {0, 0, 0, 0};
    private double[] cur_state= new double[] {0, 0, 0, 0};
    private int[] prev_action = new int[] {0, 0};
    private int[] cur_action = new int[] {0, 0};
    private int[] action_max = new int[] {0, 0};

    double self_x, self_y, enemy_x, enemy_y, self_energy, enemy_energy;
    double bearing, gunHeading, heading, absBearing;
    int[][] actionList = {{0, 0}, {1, 0}, {2, 0}, {3, 0}, {0, 1}, {1, 1}, {2, 1}, {3, 1}};

    private static int count = 0;

    private enum Action{
        Scan,
        Move
    }

    Action action = Action.Scan;

    static HashMap<Integer, int[]> actionMap = new HashMap<>();


    public void run(){
        while(true){
            if(count == 0){
                init();
            }
            count++;

            switch(action){
                case Scan:
                    turnRadarRight(180);
                    break;

                case Move:
                    Random r = new Random();
                    prev_action = cur_action;
                    prevQ = curQ;
                    if(learn){
                        if(Math.random() < explore_rate){
                            prevQ = curQ;
                            // random move
                            cur_action = actionList[r.nextInt(8)];

                            // find next action leads to max Q
                            curQ = 0;
                            for(NeuralNet tmp : neuralNetList){
                                double output = tmp.outputFor(cur_state);
                                if(output > curQ){
                                    curQ = output;
                                    action_max = getBestAction(tmp);
                                }
                            }
                        }
                        // Perform greey move
                        else{
                            // find next action leads to max Q and set the action
                            curQ = 0;
                            cur_action = actionList[r.nextInt(8)];
                            for(NeuralNet tmp : neuralNetList){
                                double output = tmp.outputFor(cur_state);
                                if(output >= curQ){
                                    curQ = output;
                                    cur_action = getBestAction(tmp);
                                }
                                if(cur_state[0]>2
                                        && cur_state[0] < 2.3
                                        && cur_state[1] >4
                                        && cur_state[1] < 4.3
                                        && cur_state[2] > 1
                                        && cur_state[2] < 1.3
                                        && cur_state[3] > 3
                                        && cur_state[3] <3.3 ){
                                    cumE.add(tmp.outputFor(cur_state) - tmp.outputFor(prev_state));
                                }
                            }
                        }
                    }
                    // greedy move
                    else{
                        // find next action leads to max Q and set the action
                        curQ = 0;
//                        cur_action = actionList[r.nextInt(8)];
                        for(NeuralNet tmp : neuralNetList){
                            double output = tmp.outputFor(cur_state);
                            if(output >= curQ){
                                curQ = output;
                                cur_action = getBestAction(tmp);
                            }
                        }
                    }

                    //Perform move
                    if(cur_action[1] != 0){
                        bulletFire(3);
                    }
                    switch(cur_action[0]){
                        case 0:
                            moveNorth();
                            execute();
                            break;
                        case 1:
                            moveEast();
                            execute();
                            break;

                        case 2:
                            moveSouth();
                            execute();
                            break;

                        case 3:
                            moveWest();
                            execute();
                            break;
                    }

                    action = Action.Scan;
                    break;

            }
        }
    }

    // Different actions
    public void moveNorth(){
        setTurnLeft(getHeading());
        setAhead(80);
    }

    public void moveEast(){
        setTurnLeft(getHeading() - 90);
        setBack(80);
    }

    public void moveSouth(){
        setTurnLeft(getHeading() - 180);
        setAhead(80);
    }

    public void moveWest(){
        setTurnLeft(getHeading() - 270);
        setBack(80);
    }

    public void bulletFire(double power){
        turnGunRight(normalizeBearing(heading - gunHeading + bearing));
        fire(power);
    }

    public int[] getBestAction(NeuralNet nn){
        switch (nn.getName()){
            case "action0Net":
                return actionList[0];
            case "action1Net":
                return actionList[1];
            case"action2Net":
                return actionList[2];
            case "action3Net":
                return actionList[3];
            case "action4Net":
                return actionList[4];
            case "action5Net":
                return actionList[5];
            case "action6Net":
                return actionList[6];
            case "action7Net":
                return actionList[7];
        }
        throw new IllegalArgumentException("The neural net is not in the list");
    }

    public double normalizeBearing(double angle) {
        while (angle >  180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    //update Q, use action_max for Q learning, on policy
    public void updateQ(double reward_value, boolean isOnPolicy){
        double newQ = prevQ + alpha * (reward_value + gamma * curQ - curQ);
//        System.out.println("prev Q " + prevQ + " reward " + reward_value + " curQ " + curQ);
        if(isOnPolicy){
            if(prev_action == actionList[0])
                action0Net.train(prev_state, action0Net.sigmoid(newQ));
            else if(prev_action == actionList[1])
                action1Net.train(prev_state, action1Net.sigmoid(newQ));
            else if(prev_action == actionList[2])
                action2Net.train(prev_state, action2Net.sigmoid(newQ));
            else if(prev_action == actionList[3])
                action3Net.train(prev_state, action3Net.sigmoid(newQ));
            else if(prev_action == actionList[4])
                action4Net.train(prev_state, action4Net.sigmoid(newQ));
            else if(prev_action == actionList[5])
                action5Net.train(prev_state, action5Net.sigmoid(newQ));
            else if(prev_action == actionList[6])
                action6Net.train(prev_state, action6Net.sigmoid(newQ));
            else
                action7Net.train(prev_state, action7Net.sigmoid(newQ));
        }
        else{
            if(action_max == actionList[0])
                action0Net.train(prev_state, action0Net.sigmoid(newQ));
            else if(action_max == actionList[1])
                action1Net.train(prev_state, action1Net.sigmoid(newQ));
            else if(action_max == actionList[2])
                action2Net.train(prev_state, action2Net.sigmoid(newQ));
            else if(action_max == actionList[3])
                action3Net.train(prev_state, action3Net.sigmoid(newQ));
            else if(action_max == actionList[4])
                action4Net.train(prev_state, action4Net.sigmoid(newQ));
            else if(action_max == actionList[5])
                action5Net.train(prev_state, action5Net.sigmoid(newQ));
            else if(action_max == actionList[6])
                action6Net.train(prev_state, action6Net.sigmoid(newQ));
            else
                action7Net.train(prev_state, action7Net.sigmoid(newQ));
        }
//        cumE.add(action1Net.outputFor(cur_state) - action1Net.outputFor(prev_state));

    }

    public void onScannedRobot(ScannedRobotEvent e) {
        updateQ(0, isOnPolicy);

        self_x = getX()/100;
        self_y = getY()/100;
        self_energy = getEnergy()/30;
        double angle = Math.toRadians((getHeading() + e.getBearing()) % 360);
        enemy_x = (getX() + Math.sin(angle) * e.getDistance())/100;
        enemy_y = (getY() + Math.cos(angle) * e.getDistance())/100;
        enemy_energy = e.getEnergy()/30;

        absBearing=absoluteBearing((float) getX(),(float) getY(),(float) enemy_x,(float) enemy_y);

        bearing = e.getBearing();
        heading = getHeading();
        gunHeading = getGunHeading();

        prev_state = cur_state;

        cur_state = new double[] {self_x, self_y, enemy_x, enemy_y};

        action = Action.Move;
    }

    public void onHitWall(HitWallEvent e) {
        double reward = -0.25;
//        double reward = 0;

        updateQ(reward, isOnPolicy);

        // Self position
        self_x = getX()/100;
        self_y = getY()/100;

        self_energy = getEnergy()/30;

        bearing = e.getBearing();
        heading = getHeading();
        gunHeading = getGunHeading();

        prev_state = cur_state;
        cur_state = new double[] {self_x, self_y, enemy_x, enemy_y};
    }

    public void onBulletMissed(BulletMissedEvent e){
        double reward = -0.05;
        updateQ(reward, isOnPolicy);
    }

    public void onBulletHit(BulletHitEvent e) {
        double reward = 0.05;
        updateQ(reward, isOnPolicy);
    }

    public void onHitByBullet(HitByBulletEvent e) {
        double reward = -0.05;
        updateQ(reward, isOnPolicy);
    }

    public void onWin(WinEvent e) {
        double reward = 1;
        updateQ(reward, isOnPolicy);
        wins++;
        winPer100++;
    }

    public void onRoundEnded(RoundEndedEvent e){
        games++;
        gamePer100++;
        if(gamePer100 == getResult){
            double tmp = ((double)winPer100/(double)getResult);
            System.out.println(tmp);

            System.out.println("Adding to winpercentage list");
            winPercentage.add(tmp);
            gameCounter.add((double)games);
            if(tmp >= max_win_rate){
                max_win_rate = tmp;

                tmp_input_weights[0] = action0Net.inputWeights;
                tmp_input_weights[1] = action1Net.inputWeights;
                tmp_input_weights[2] = action2Net.inputWeights;
                tmp_input_weights[3] = action3Net.inputWeights;
                tmp_input_weights[4] = action4Net.inputWeights;
                tmp_input_weights[5] = action5Net.inputWeights;
                tmp_input_weights[6] = action6Net.inputWeights;
                tmp_input_weights[7] = action7Net.inputWeights;

                tmp_output_weights[0] = action0Net.outputWeights;
                tmp_output_weights[1] = action1Net.outputWeights;
                tmp_output_weights[2] = action2Net.outputWeights;
                tmp_output_weights[3] = action3Net.outputWeights;
                tmp_output_weights[4] = action4Net.outputWeights;
                tmp_output_weights[5] = action5Net.outputWeights;
                tmp_output_weights[6] = action6Net.outputWeights;
                tmp_output_weights[7] = action7Net.outputWeights;
            }
            gamePer100 = 0;
            winPer100 = 0;
        }
        for(int i = 0; i < monitored_states.length; i++){
            eForDefinedState[i].add(action6Net.outputFor(monitored_states[i]));
        }

    }

    public void onBattleEnded(BattleEndedEvent e) {
        //Here we are looking for the "main" Thread Group. We are doing this because we will do the
        //actual saving of the LUT in a seperate thread. And since we do not want this Thread to be
        //"seen" by the robocode runtime, we do not add it in the Thread Group of the robocode runtime
        //but in the "main" Thread Groupnba
        System.out.println("Winning rate is " + (double)(wins)/(double)(games));
        ArrayList<NeuralNet> nnList_copy = neuralNetList;

        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        while(true){
            tg = tg.getParent();
            if(tg.getName().equals("main")){
                break;
            }
        }

        //Here we create the thread, by supplying the Thread Group we just looked up and the function
        //to be executed by the thread
        Thread thread = new Thread(tg, () -> {
            System.out.println("Thread started");
            if (learn) {
                List<Double> winPercentageFinal = winPercentage;
                List<Double>[] eForDefinedStateFinal = eForDefinedState;
                List<Double> cumErrorFinal = cumE;

                for (NeuralNet tmp : nnList_copy) {
                    String filename = tmp.getName() + "Weights.txt";
                    File nnWeightFile = new File(filename);
                    tmp.save(nnWeightFile);
                }

                PrintStream writeWinPercentage;
                try {
                    writeWinPercentage = new PrintStream(("C:\\robocode\\robots\\rsa\\NNRobot\\winPercentageLearningRate" + learningRate + "NumOfNeurons" + numOfHidden + "ExploreRate" + explore_rate +".txt"));
                    for (int i = 0; i < winPercentageFinal.size(); i++) {
                        writeWinPercentage.println(getResult * (i + 1) + " " + winPercentageFinal.get(i) + " ");
                    }
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }

                try{
                    FileWriter writer = new FileWriter("C:\\robocode\\robots\\rsa\\NNRobot\\cumE.txt");
                    for(int i = 0; i < cumErrorFinal.size(); i++){
                        writer.write(String.valueOf(cumErrorFinal.get(i)) + " ");
                    }
                    writer.close();
                }
                catch(IOException ex){
                    ex.printStackTrace();
                }

                for(int i = 0; i < monitored_states.length; i++) {
                    try {
                        FileWriter writer = new FileWriter("C:\\robocode\\robots\\rsa\\NNRobot\\Es" + i + ".txt");

                        // Write the hidden-to-output weights, in this case 5 weights
                        for (int j = 0; j < eForDefinedStateFinal[i].size(); j++) {
                            writer.write(String.valueOf(eForDefinedStateFinal[i].get(j)) + " ");
                        }

                        writer.close();
//                    }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

                for(int i = 0; i < actionList.length; i++) {
                    try {
                        FileWriter writer = new FileWriter("C:\\robocode\\robots\\rsa\\NNRobot\\localMaxWin&" + max_win_rate + "action" + i + "learnrate" + learningRate + "NumOfNeurons" + numOfHidden + "ExploreRate" + explore_rate + ".txt");

                        // Write the hidden-to-output weights, in this case 5 weights
                        for (int j = 0; j < tmp_output_weights[i].length; j++) {
                            writer.write(String.valueOf(tmp_output_weights[i][j]) + " ");
                        }

                        //Write the input-to-hidden weights, in this case 12 weights
                        for (int j = 0; j < numOfHidden + 1; j++) {
                            for (int k = 0; k < cur_state.length + 1; k++) {
                                writer.write(String.valueOf(tmp_input_weights[i][j][k]) + " ");
                            }
                        }
                        writer.close();
//                    }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        //We give the thread a special name so that we can wait for the completion of the thread in the
        //main method
        thread.setName("Output Thread");
        thread.start();
    }

    public void init(){
        for(int i = 0; i < actionList.length; i++){
            actionMap.put(i, actionList[i]);
        }

        action0Net = new NeuralNet(4, numOfHidden, learningRate, momentum, -1, 1);
        action0Net.setName("action0Net");
        action1Net = new NeuralNet(4, numOfHidden, learningRate, momentum, -1, 1);
        action1Net.setName("action1Net");
        action2Net = new NeuralNet(4, numOfHidden, learningRate, momentum, -1, 1);
        action2Net.setName("action2Net");
        action3Net = new NeuralNet(4, numOfHidden, learningRate, momentum, -1, 1);
        action3Net.setName("action3Net");
        action4Net = new NeuralNet(4, numOfHidden, learningRate, momentum, -1, 1);
        action4Net.setName("action4Net");
        action5Net = new NeuralNet(4, numOfHidden, learningRate, momentum, -1, 1);
        action5Net.setName("action5Net");
        action6Net = new NeuralNet(4, numOfHidden, learningRate, momentum, -1, 1);
        action6Net.setName("action6Net");
        action7Net = new NeuralNet(4, numOfHidden, learningRate, momentum, -1, 1);
        action7Net.setName("action7Net");
        neuralNetList.add(action0Net);
        neuralNetList.add(action1Net);
        neuralNetList.add(action2Net);
        neuralNetList.add(action3Net);
        neuralNetList.add(action4Net);
        neuralNetList.add(action5Net);
        neuralNetList.add(action6Net);
        neuralNetList.add(action7Net);
        System.out.println("Finish the list");
        winPercentage = new ArrayList<>();
        for(int i = 0; i < monitored_states.length; i++){
            eForDefinedState[i] = new ArrayList<>();
        }

        if(learn) {
            for (NeuralNet tmp : neuralNetList) {
                System.out.println("Initializing all the weights");
                tmp.initializeWeights();
            }
        }
        else{
            load();
        }
        gameCounter = new ArrayList<>();
        tmp_output_weights = new double[8][numOfHidden+1];
        tmp_input_weights = new double[8][numOfHidden+1][4+1];
        prev_diff = new double[3];
    }

    public void load(){
        try {
            action0Net.load("action0NetWeights.txt");
            action1Net.load("action1NetWeights.txt");
            action2Net.load("action2NetWeights.txt");
            action3Net.load("action3NetWeights.txt");
            action4Net.load("action4NetWeights.txt");
            action5Net.load("action5NetWeights.txt");
            action6Net.load("action6NetWeights.txt");
            action7Net.load("action7NetWeights.txt");
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    //absolute bearing
    double absoluteBearing(float x1, float y1, float x2, float y2) {
        double xo = x2-x1;
        double yo = y2-y1;
        double hyp = Point2D.distance(x1, y1, x2, y2);
        double arcSin = Math.toDegrees(Math.asin(xo / hyp));
        double bearing = 0;

        if (xo > 0 && yo > 0) { // both pos: lower-Left
            bearing = arcSin;
        } else if (xo < 0 && yo > 0) { // x neg, y pos: lower-right
            bearing = 360 + arcSin; // arcsin is negative here, actuall 360 - ang
        } else if (xo > 0 && yo < 0) { // x pos, y neg: upper-left
            bearing = 180 - arcSin;
        } else if (xo < 0 && yo < 0) { // both neg: upper-right
            bearing = 180 - arcSin; // arcsin is negative here, actually 180 + ang
        }

        return bearing;
    }

}
