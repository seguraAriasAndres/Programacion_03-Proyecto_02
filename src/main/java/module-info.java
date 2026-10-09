module cr.ac.una.est.programacion03proyecto {
    requires javafx.controls;
    requires javafx.fxml;


    opens cr.ac.una.est.programacion03proyecto02 to javafx.fxml;
    exports cr.ac.una.est.programacion03proyecto02;
}