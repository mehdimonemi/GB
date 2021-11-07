@ECHO OFF
start .\jdk\bin\javaw.exe -Djava.library.path=./cplex/bin/x64_win64 --module-path ".\fx\lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics -jar GB.jar