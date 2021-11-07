package ir.rai.Data;

import ir.rai.App;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.fx.overlay.CrosshairOverlayFX;
import org.jfree.chart.labels.StandardCrosshairLabelGenerator;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

public class Chart extends StackPane implements ChartMouseListenerFX {

    public ChartViewer chartViewer;
    public JFreeChart chart;
    public ChartPanel chartPanel;
    private Crosshair xCrosshair;
    private Crosshair yCrosshair;

    public Chart(XYSeriesCollection dataset, XYLineAndShapeRenderer renderer, String sectionName) {
        InputStream is = App.loadFont("Gandom-FD.ttf");
        Font gandom = null;
        try {
            gandom = Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }

        this.chart = createChart(dataset, sectionName, gandom);
        this.chart.getXYPlot().setRenderer(renderer);
        this.chart.getXYPlot().setBackgroundPaint(new Color(173, 173, 173));
        this.chartViewer = new ChartViewer(chart, false);
        this.chartViewer.addChartMouseListener(this);
        this.chartViewer.getCanvas().getChart().setBackgroundPaint(new Color(242, 242, 242));
        getChildren().add(this.chartViewer);

        CrosshairOverlayFX crosshairOverlay = new CrosshairOverlayFX();
        this.xCrosshair = new Crosshair(Double.NaN, Color.GRAY,
                new BasicStroke(0f));
        this.xCrosshair.setStroke(new BasicStroke(1.5f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1,
                new float[]{2.0f, 2.0f}, 0));
        this.xCrosshair.setLabelVisible(true);
        this.xCrosshair.setLabelGenerator(new StandardCrosshairLabelGenerator("{0}",
                new DecimalFormat("##")));
        this.xCrosshair.setLabelFont(gandom.deriveFont(12f));

        this.yCrosshair = new Crosshair(Double.NaN, Color.GRAY,
                new BasicStroke(0f));
        this.yCrosshair.setStroke(new BasicStroke(1.5f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1,
                new float[]{2.0f, 2.0f}, 0));
        this.yCrosshair.setLabelVisible(true);
        this.yCrosshair.setLabelGenerator(new StandardCrosshairLabelGenerator("{0}",
                new DecimalFormat("##")));
        this.yCrosshair.setLabelFont(gandom.deriveFont(12f));

        crosshairOverlay.addDomainCrosshair(xCrosshair);
        crosshairOverlay.addRangeCrosshair(yCrosshair);

        Platform.runLater(() -> {
            this.chartViewer.getCanvas().addOverlay(crosshairOverlay);
        });
    }

    private JFreeChart createChart(XYDataset dataset, String sectionName, Font gandom) {
        JFreeChart chart = ChartFactory.createXYLineChart
                ("گاباری مسیر " + sectionName, "میلی متر", "میلی متر", dataset);
        chart.removeLegend();
        chart.getTitle().setFont(gandom.deriveFont(12f));
        chart.getXYPlot().getDomainAxis().setLabelFont(gandom.deriveFont(12f));
        chart.getXYPlot().getDomainAxis().setTickLabelFont(gandom.deriveFont(12f));
        chart.getXYPlot().getRangeAxis().setLabelFont(gandom.deriveFont(12f));
        chart.getXYPlot().getRangeAxis().setTickLabelFont(gandom.deriveFont(12f));
        return chart;
}

    @Override
    public void chartMouseClicked(ChartMouseEventFX chartMouseEventFX) {

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
//        y = DatasetUtils.findYValue(plot.getDataset(), 0, x);
        this.xCrosshair.setValue(x);
        this.yCrosshair.setValue(y);
    }

}

