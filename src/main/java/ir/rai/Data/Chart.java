package ir.rai.Data;

import ir.rai.MyCoordinate;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.layout.StackPane;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.fx.overlay.CrosshairOverlayFX;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class Chart extends StackPane implements ChartMouseListenerFX {

    public ChartViewer chartViewer;
    private Crosshair xCrosshair;
    private Crosshair yCrosshair;

    public Chart(ObservableList<MyCoordinate> data) {
        XYDataset dataset = createDataset(data);
        JFreeChart chart = createChart(dataset);
        this.chartViewer = new ChartViewer(chart);
        this.chartViewer.addChartMouseListener(this);
        getChildren().add(this.chartViewer);

        CrosshairOverlayFX crosshairOverlay = new CrosshairOverlayFX();
        this.xCrosshair = new Crosshair(Double.NaN, Color.GRAY,
                new BasicStroke(0f));
        this.xCrosshair.setStroke(new BasicStroke(1.5f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1,
                new float[]{2.0f, 2.0f}, 0));
        this.xCrosshair.setLabelVisible(true);
        this.yCrosshair = new Crosshair(Double.NaN, Color.GRAY,
                new BasicStroke(0f));
        this.yCrosshair.setStroke(new BasicStroke(1.5f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1,
                new float[]{2.0f, 2.0f}, 0));
        this.yCrosshair.setLabelVisible(true);
        crosshairOverlay.addDomainCrosshair(xCrosshair);
        crosshairOverlay.addRangeCrosshair(yCrosshair);

        Platform.runLater(() -> {
            this.chartViewer.getCanvas().addOverlay(crosshairOverlay);
        });
    }

    private XYDataset createDataset(ObservableList<MyCoordinate> data) {
        XYSeries series1 = new XYSeries("Bar");
        for (MyCoordinate coordinate :
                data) {
            series1.add(coordinate.getX(), coordinate.getY());
        }
        XYSeriesCollection dataset = new XYSeriesCollection(series1);
        return dataset;
    }

    private static JFreeChart createChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Test", "X", "Y", dataset);
        return chart;
    }


    @Override
    public void chartMouseClicked(ChartMouseEventFX event) {
//            event.getChart().getXYPlot().
    }

    @Override
    public void chartMouseMoved(ChartMouseEventFX event) {
        Rectangle2D dataArea = this.chartViewer.getCanvas().getRenderingInfo().getPlotInfo().getDataArea();
        JFreeChart chart = event.getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis xAxis = plot.getDomainAxis();
        ValueAxis yAxis = plot.getRangeAxis();
        double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
        double y = yAxis.java2DToValue(event.getTrigger().getY(), dataArea, RectangleEdge.LEFT);
        // make the crosshairs disappear if the mouse is out of range
        if (!xAxis.getRange().contains(x)) {
            x = Double.NaN;
        }
//            DatasetUtils.
        y = DatasetUtils.findYValue(plot.getDataset(), 0, x);
        this.xCrosshair.setValue(x);
        this.yCrosshair.setValue(y);
    }

}

