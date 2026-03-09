import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.JFrame;

public class GraphGenerator {

    static  XYSeries series = new XYSeries("Distance");

    public static  void startGraph(){

        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Distance Monitoring",
                "Time",
                "Distance (cm)",
                dataset
        );

        ChartPanel panel = new ChartPanel(chart);

        JFrame frame = new JFrame("Distance Graph");
        frame.setSize(800, 600);
        frame.setContentPane(panel);
        frame.setVisible(true);
    }

    public static void update(double distance){
        int time = series.getItemCount();

        series.add(time, distance);
    }
}
