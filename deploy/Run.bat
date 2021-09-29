@ECHO OFF
.\jdk-11.0.11.9-openj9\bin\java.exe -Djava.library.path=./cplex/bin/x64_win64 --module-path ".\fx\lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics -jar GB.jar