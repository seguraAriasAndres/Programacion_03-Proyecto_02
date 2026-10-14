module cr.ac.una.est.programacion03proyecto {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens cr.ac.una.est.programacion03proyecto02.controller to javafx.fxml;
    exports cr.ac.una.est.programacion03proyecto02;
}
