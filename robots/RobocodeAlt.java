package rsa;

import net.sf.robocode.security.HiddenAccess;
import org.jfree.ui.RefineryUtilities;

import java.util.Set;

public class RobocodeAlt {

    public static void main(String[] args) {
        //This would also be called by the regular Robocode main class
        HiddenAccess.robocodeMain(args);

        //Here we wait for the robocode initialization to take place
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        for(Thread thread : threads){
            if(thread.getName().equals("Robocode main thread")){
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    //Ignore
                }
            }
        }

        //Here we wait for the battle to end
        threads = Thread.getAllStackTraces().keySet();
        for(Thread thread : threads){
            if(thread.getName().equals("Battle Thread")){
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    //Ignore
                }
            }
        }

        //Since the battle has ended, our Output Thread will definitely be created. With this part
        //of the code we wait until this thread has completed its work
        threads = Thread.getAllStackTraces().keySet();
        for(Thread thread : threads){
            if(thread.getName().equals("Output Thread")){
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    //Ignore
                }
            }
        }

    }
    static void drawXY(String title, Double[] input, Double[] output){
//        if(binaryOrBipolar == "binary"){
//            XYSeriesDemo binaryDemo = new XYSeriesDemo("Binary Total Error vs Num Of Epochs", fileOutput);
//            drawXYPlot(binaryDemo);
//        }
//        else if(binaryOrBipolar == "bipolar"){
//            XYSeriesDemo bipolarDemo = new XYSeriesDemo("Bipolar Total Error vs Num Of Epochs", fileOutput);
//            drawXYPlot(bipolarDemo);
//        }
//        else{
//            throw new IllegalArgumentException("Not a valid representation type, choose either binary or bipolar");
//        }
        System.out.println("drawing the graph");
        drawXY XYGraph = new drawXY(title, input, output);
        drawXYPlot(XYGraph);
    }

    static public void drawXYPlot(drawXY plotDemo){
        plotDemo.pack();
        RefineryUtilities.centerFrameOnScreen(plotDemo);
        plotDemo.setVisible(true);
    }

}
