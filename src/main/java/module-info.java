module cr.ac.una.est.programacion03proyecto {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;

    opens cr.ac.una.est.programacion03proyecto02.controller.demo to javafx.fxml;
    exports cr.ac.una.est.programacion03proyecto02.app;
}
