import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class GraphGenerator {

    static  XYSeries series = new XYSeries("Distance");
    static ChartPanel chartPanel;

    public static ChartPanel startGraph(){

        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Distance Monitoring",
                "Time",
                "Distance (cm)",
                dataset
        );

        chartPanel = new ChartPanel(chart);

        return chartPanel;

//        JFrame frame = new JFrame("Distance Graph");
//        frame.setSize(800, 600);
//        frame.setContentPane(chartPanel);
//        frame.setVisible(true);
    }

    public static void update(double distance){
        int time = series.getItemCount();

        series.add(time, distance);
    }
}
