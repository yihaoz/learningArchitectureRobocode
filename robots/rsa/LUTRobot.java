package rsa;

import com.sun.javafx.geom.Point2D;
import robocode.*;

import java.io.*;
import java.util.*;

public class LUTRobot extends AdvancedRobot {

    public double alpha = 0.05; // step size
    public double gamma = 0.9; // discount factor
    public boolean explore = false;
    public boolean re_init_LUT = false;
    public double explore_rate = 0.9;
    public boolean isOnPolicy = true;

    public static int wins;
    public static int games;

    public static int winPer100;
    public static int gamePer100;

    public static List<Double> winPercentage;

    private int self_x;
    private int self_y;
    private int enemy_x;
    private int enemy_y;
    private int self_energy;
    private int enemy_energy;

    private double bearing;
    private double absBearing;
    private double heading;
    private double gunHeading;

    private int[] prev_state = new int[] {0, 0, 0, 0, 0, 0};
    private int[] cur_state= new int[] {0, 0, 0, 0, 0, 0};

    private int[] prev_action = new int[] {0, 0, 0, 0, 0};
    private int[] cur_action = new int[] {0, 0, 0, 0, 0};
    private int[] action_max = new int[] {0, 0, 0, 0, 0};

    private static double [][][][][][][][][][][] LUT;

    private int state1_len, state2_len, state3_len, state4_len, state5_len, state6_len;
    private int action1_len, action2_len, action3_len, action4_len, action5_len;

    private double Qmax = 0;

    private static int count = 0;

    private enum Action{
        Scan,
        Move
    }

    Action action = Action.Scan;

    public void run(){

        while(true){
            if(count == 0){
                state1_len = (int)getWidth()/100;
                state2_len = (int)getHeight()/100;
                state3_len = (int)getWidth()/100;
                state4_len = (int)getHeight()/100;
                state5_len = 4;
                state6_len = 4;
                action1_len = 2;
                action2_len = 2;
                action3_len = 2;
                action4_len = 2;
                action5_len = 4;
                initializeLUT();
                winPercentage = new ArrayList<>();

                if(!re_init_LUT){
                    try {
                        System.out.println("Loading");
                        load();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                count++;
            }
            switch(action){

                case Scan:
                    turnRadarRight(180);
                    break;

                case Move:
                    if(explore){
                        Random r = new Random();

                        if(Math.random() < explore_rate){
                            prev_action = cur_action;

                            // random move
                            cur_action = new int[] {(Math.random()<0.5)?0:1, (Math.random()<0.5)?0:1, (Math.random()<0.5)?0:1, (Math.random()<0.5)?0:1, r.nextInt(4)};

                            // find action leads to max Q
                            for(int a1 = 0; a1 < 2; a1++){
                                for(int a2 = 0; a2< 2; a2++){
                                    for(int a3 = 0; a3 < 2; a3++){
                                        for(int a4 = 0; a4 < 2; a4++){
                                            for(int fire = 1; fire <= 3; fire++){

                                                double tmp = LUT[cur_state[0]][cur_state[1]][cur_state[2]][cur_state[3]][cur_state[4]][cur_state[5]][a1][a2][a3][a4][fire];
                                                if(tmp > Qmax){
                                                    Qmax = tmp;
                                                    action_max = new int[] {a1, a2, a3, a4, fire};
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else{
                            // find greedy move
                            Qmax = 0;
                            for(int a1 = 0; a1 < 2; a1++){
                                for(int a2 = 0; a2< 2; a2++){
                                    for(int a3 = 0; a3 < 2; a3++){
                                        for(int a4 = 0; a4 < 2; a4++){
                                            for(int fire = 0; fire <= 3; fire++){
                                                double tmp = LUT[cur_state[0]][cur_state[1]][cur_state[2]][cur_state[3]][cur_state[4]][cur_state[5]][a1][a2][a3][a4][fire];
                                                if(tmp > Qmax){
                                                    Qmax = tmp;
                                                    prev_action = cur_action;
                                                    cur_action = new int[] {a1, a2, a3, a4, fire};
                                                    action_max = cur_action;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // greedy
                    else{
                        Qmax = 0;
                        for(int a1 = 0; a1 < 2; a1++){
                            for(int a2 = 0; a2< 2; a2++){
                                for(int a3 = 0; a3 < 2; a3++){
                                    for(int a4 = 0; a4 < 2; a4++){
                                        for(int fire = 1; fire <= 3; fire++){
                                            double tmp = LUT[cur_state[0]][cur_state[1]][cur_state[2]][cur_state[3]][cur_state[4]][cur_state[5]][a1][a2][a3][a4][fire];
                                            if(tmp > Qmax){
                                                Qmax = tmp;
                                                prev_action = cur_action;
                                                cur_action = new int[] {a1, a2, a3, a4, fire};
                                                action_max = cur_action;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        updateQ(0, isOnPolicy);
                    }

                    // Perform action
                    if(cur_action[4] != 0) bulletFire((double)cur_action[4]);
                    if(cur_action[0] == 1) moveNorth();
                    if(cur_action[1] == 1) moveEast();
                    if(cur_action[2] == 1) moveSouth();
                    if(cur_action[3] == 1) moveWest();

                    execute();
                    action = Action.Scan;
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

    public void onScannedRobot(ScannedRobotEvent e){

        updateQ(0, isOnPolicy);
        // Self position
        self_x = quantizeMetre(getX());
        self_y = quantizeMetre(getY());

        // Enemy position
        // Calculate the angle to the scanned robot
        double angle = Math.toRadians((getHeading() + e.getBearing()) % 360);
        enemy_x = quantizeMetre(getX() + Math.sin(angle) * e.getDistance());
        enemy_y = quantizeMetre(getY() + Math.cos(angle) * e.getDistance());

        self_energy = quantizeEnergy(getEnergy());
        enemy_energy = quantizeEnergy(e.getEnergy());

        absBearing=absoluteBearing((float) getX(),(float) getY(),(float) enemy_x,(float) enemy_y);

        bearing = e.getBearing();
        heading = getHeading();
        gunHeading = getGunHeading();

        prev_state = cur_state;

        cur_state = new int[] {self_x, self_y, enemy_x, enemy_y, self_energy, enemy_energy};

        action = Action.Move;

    }

    public void onHitWall(HitWallEvent e) {
        double reward = -10;
//        double reward = 0;

        updateQ(reward, isOnPolicy);

        // Self position
        self_x = quantizeMetre(getX());
        self_y = quantizeMetre(getY());

        self_energy = quantizeEnergy(getEnergy());

        bearing = e.getBearing();
        heading = getHeading();
        gunHeading = getGunHeading();

        prev_state = cur_state;
        cur_state = new int[] {self_x, self_y, enemy_x, enemy_y, self_energy, enemy_energy};
    }

    public void onBulletMissed(BulletMissedEvent e){
        double reward = -1;
//        double reward = 0;

        updateQ(reward, isOnPolicy);
    }

    public void onBulletHit(BulletHitEvent e) {
        double reward = 2;
//        double reward = 0;
        //update Q
        updateQ(reward, isOnPolicy);
    }

    public void onHitByBullet(HitByBulletEvent e) {
        double reward = -3;
//        double reward = 0;

        updateQ(reward, isOnPolicy);
    }

    public void onWin(WinEvent e) {
        wins++;
        winPer100++;

        double reward = 20;
        updateQ(reward, isOnPolicy);
    }

    public void onRoundEnded(RoundEndedEvent e){
        games++;
        gamePer100++;
        if(gamePer100 == 1000){
            winPercentage.add(((double)winPer100/1000));
            gamePer100 = 0;
            winPer100 = 0;
        }
    }

    public void onBattleEnded(BattleEndedEvent e) {
        //Here we are looking for the "main" Thread Group. We are doing this because we will do the
        //actual saving of the LUT in a seperate thread. And since we do not want this Thread to be
        //"seen" by the robocode runtime, we do not add it in the Thread Group of the robocode runtime
        //but in the "main" Thread Group
        System.out.println("Winning rate is " + (double)(wins)/(double)(games));

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
            //You will probably need to change this line since you are probably using different
            //interfaces for your LUT than me
            if(re_init_LUT){
                double [][][][][][][][][][][] LUT_final = LUT;
                List<Double> winPercentageFinal = winPercentage;
                PrintStream w = null;
                PrintStream writeWinPercentage = null;
                System.out.println("start writing");

                try{
                    writeWinPercentage = new PrintStream(("C:\\robocode\\robots\\rsa\\LUTRobot.data\\winPercentage.txt"));
                    for(int i = 0; i <winPercentageFinal.size(); i++){
                        writeWinPercentage.println(1000*(i+1) + " " + winPercentageFinal.get(i) + " ");
                    }
                    w = new PrintStream(("C:\\robocode\\robots\\rsa\\LUTRobot.data\\LUT.txt"));
                    for(int state1 = 0; state1 < 8; state1++){
                        for(int state2 = 0; state2 < 6; state2++){
                            for(int state3 = 0; state3 < 8; state3++) {
                                for(int state4 = 0; state4 < 6; state4++) {
                                    for (int state5 = 0; state5 < 4; state5++) {
                                        for (int state6 = 0; state6 < 4; state6++) {
                                            for(int a1 = 0; a1 < 2; a1++){
                                                for(int a2 = 0; a2 < 2; a2++){
                                                    for(int a3 = 0; a3 < 2; a3++){
                                                        for(int a4 = 0; a4 < 2; a4++){
                                                            for(int fire = 0; fire < 4; fire++){
                                                                double tmp = LUT_final[state1][state2][state3][state4][state5][state6][a1][a2][a3][a4][fire];
                                                                w.print(tmp + " ");

                                                                count++;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    w.print(count + " ");
                    System.out.println("finish writing");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }finally {
                    w.flush();
                    w.close();
                }
            }
        });

        //We give the thread a special name so that we can wait for the completion of the thread in the
        //main method
        thread.setName("Output Thread");
        thread.start();
    }

    public void save(){
        PrintStream w = null;
        System.out.println("start writing");

        try{
//            int count = 0;
            w = new PrintStream(new RobocodeFileOutputStream(getDataFile("LUT.txt")));
            for(int state1 = 0; state1 < 8; state1++){
                for(int state2 = 0; state2 < 6; state2++){
                    for(int state3 = 0; state3 < 8; state3++) {
                        for(int state4 = 0; state4 < 6; state4++) {
                            for (int state5 = 0; state5 < 4; state5++) {
                                for (int state6 = 0; state6 < 4; state6++) {
                                    for(int a1 = 0; a1 < 2; a1++){
                                        for(int a2 = 0; a2 < 2; a2++){
                                            for(int a3 = 0; a3 < 2; a3++){
                                                for(int a4 = 0; a4 < 2; a4++){
                                                    for(int fire = 0; fire < 4; fire++){
                                                        double tmp = LUT[state1][state2][state3][state4][state5][state6][a1][a2][a3][a4][fire];
                                                        w.print(tmp + " ");

                                                        count++;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            w.print(count + " ");
            System.out.println("finish writing");
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            w.flush();
            w.close();
        }
    }

    public void load() throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(("C:\\robocode\\robots\\rsa\\LUTRobot.data\\LUT_92_50w.txt")));
        String stringBuffer = in.readLine();
        String[] strs = stringBuffer.trim().split("\\s+");

        int count = 0;

        for(int state1 = 0; state1 < 8; state1++){
            for(int state2 = 0; state2 < 6; state2++){
                for(int state3 = 0; state3 < 8; state3++) {
                    for(int state4 = 0; state4 < 6; state4++) {
                        for (int state5 = 0; state5 < 4; state5++) {
                            for (int state6 = 0; state6 < 4; state6++) {
                                for(int a1 = 0; a1 < 2; a1++){
                                    for(int a2 = 0; a2 < 2; a2++){
                                        for(int a3 = 0; a3 < 2; a3++){
                                            for(int a4 = 0; a4 < 2; a4++){
                                                for(int fire = 0; fire < 4; fire++){

                                                    if(count >= strs.length) throw new ArrayIndexOutOfBoundsException("number of entries in the txt file doesn't match the array size");

                                                    LUT[state1][state2][state3][state4][state5][state6][a1][a2][a3][a4][fire] = Double.parseDouble(strs[count]);
                                                    count++;

                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void initializeLUT(){
        LUT = new double[8][6][8][6][4][4][2][2][2][2][4];
        if(re_init_LUT){
            for(int state1 = 0; state1 < 8; state1++){
                for(int state2 = 0; state2 < 6; state2++){
                    for(int state3 = 0; state3 < 8; state3++) {
                        for(int state4 = 0; state4 < 6; state4++) {
                            for (int state5 = 0; state5 < 4; state5++) {
                                for (int state6 = 0; state6 < 4; state6++) {
                                    for(int a1 = 0; a1 < 2; a1++){
                                        for(int a2 = 0; a2 < 2; a2++){
                                            for(int a3 = 0; a3 < 2; a3++){
                                                for(int a4 = 0; a4 < 2; a4++){
                                                    for(int fire = 0; fire < 4; fire++){
                                                        LUT[state1][state2][state3][state4][state5][state6][a1][a2][a3][a4][fire] = 0.5;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //update Q, use action_max for Q learning, on policy
    public void updateQ(double reward_value, boolean isOnPolicy){
        if(isOnPolicy){
            double prevQ = LUT[prev_state[0]][prev_state[1]][prev_state[2]][prev_state[3]][prev_state[4]][prev_state[5]][prev_action[0]][prev_action[1]][prev_action[2]][prev_action[3]][prev_action[4]];
            double curQ = LUT[cur_state[0]][cur_state[1]][cur_state[2]][cur_state[3]][cur_state[4]][cur_state[5]][cur_action[0]][cur_action[1]][cur_action[2]][cur_action[3]][cur_action[4]];
            LUT[prev_state[0]][prev_state[1]][prev_state[2]][prev_state[3]][prev_state[4]][prev_state[5]][prev_action[0]][prev_action[1]][prev_action[2]][prev_action[3]][prev_action[4]] = prevQ + alpha * (reward_value + gamma * curQ - curQ);
        }
        else{
            double prevQ = LUT[prev_state[0]][prev_state[1]][prev_state[2]][prev_state[3]][prev_state[4]][prev_state[5]][prev_action[0]][prev_action[1]][prev_action[2]][prev_action[3]][prev_action[4]];
            double curQ = LUT[cur_state[0]][cur_state[1]][cur_state[2]][cur_state[3]][cur_state[4]][cur_state[5]][action_max[0]][action_max[1]][action_max[2]][action_max[3]][action_max[4]];
            LUT[prev_state[0]][prev_state[1]][prev_state[2]][prev_state[3]][prev_state[4]][prev_state[5]][prev_action[0]][prev_action[1]][prev_action[2]][prev_action[3]][prev_action[4]] = prevQ + alpha * (reward_value + gamma * curQ - curQ);
        }
    }

    private int quantizeMetre(double metre){
        return (int)metre/100;
    }

    public double normalizeBearing(double angle) {
        while (angle >  180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    public int quantizeEnergy(double energy){

        if(energy < 90){
            return (int)(energy/30);
        }
        else{
            return 3;
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