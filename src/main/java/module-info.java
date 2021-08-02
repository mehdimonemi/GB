module ir.rai {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires poi;
    requires poi.ooxml;
    requires org.jfxtras.styles.jmetro;
    requires esri.geometry.api;

    opens ir.rai to javafx.fxml;
    exports ir.rai;
}
