module cr.ac.una.est.programacion03proyecto {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;

    opens cr.ac.una.est.programacion03proyecto02.controller.catalogo to javafx.fxml;
    opens cr.ac.una.est.programacion03proyecto02.controller.pedidos to javafx.fxml;
    exports cr.ac.una.est.programacion03proyecto02.app;
}
