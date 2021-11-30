module ir.rai {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires poi;
    requires poi.ooxml;
    requires JDXF;
    requires org.controlsfx.controls;
    requires org.jfree.chart.fx;
    requires org.jfree.jfreechart;
    requires org.jfxtras.styles.jmetro;
    requires org.locationtech.jts;
    requires cplex;
    requires java.desktop;
    requires org.jfree.fxgraphics2d;
    requires javafx.swing;
    requires batik.transcoder;

    opens ir.rai to javafx.fxml;

    exports ir.rai;
    exports ir.rai.Data;
}
