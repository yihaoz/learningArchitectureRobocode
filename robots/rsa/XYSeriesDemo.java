package rsa;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

import java.io.*;


public class XYSeriesDemo extends ApplicationFrame {

    /**
     * A demonstration application showing an XY series containing a null value.
     *
     * @param title the frame title.
     */
    public XYSeriesDemo(final String title, String fileName) {

        super(title);
        final XYSeries series = new XYSeries("Training set data");

        try{
            String errFile = fileName;

            FileInputStream fis = new FileInputStream(errFile);
            DataInputStream dis = new DataInputStream(fis);
            int iter = 1;
            while(dis.available() > 0){
                double error = dis.readDouble();
//                System.out.println("Adding iteration : " + iter + " Error " + error);

                series.add(iter, error);
                iter++;
            }
        }
        catch(IOException e){
            System.out.println("IOException : " + e);
        }

        final XYSeriesCollection data = new XYSeriesCollection(series);
        final JFreeChart chart = ChartFactory.createXYLineChart(
                "Total Error vs Num of Epochs",
                "Num of Epochs",
                "Total Error",
                data,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        setContentPane(chartPanel);
    }
}
