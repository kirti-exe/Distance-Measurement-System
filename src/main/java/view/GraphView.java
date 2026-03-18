package view;

import model.DistanceModel;
import model.DistanceReading;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * GraphGenerator.
 * Builds the JFreeChart panel and updates it when the model fires a reading.
 */
public class GraphView implements DistanceModel.ReadingListener {

    private final XYSeries   series     = new XYSeries("Distance");
    private final ChartPanel chartPanel;

    public GraphView() {
        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
            "Distance Monitoring",
            "Time",
            "Distance (cm)",
            dataset
        );

        chartPanel = new ChartPanel(chart);
    }

    public ChartPanel getChartPanel() {
        return chartPanel;
    }

    @Override
    public void onNewReading(DistanceReading reading) {
        int time = series.getItemCount();
        series.add(time, reading.getDistance());
    }
}
