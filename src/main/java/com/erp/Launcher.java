package com.erp;

import java.util.TimeZone;

/**
 * Classe launcher separada — necessária para iniciar JavaFX a partir de um fat JAR.
 * O jpackage e o java -jar devem apontar para esta classe, não para Main.
 */
public class Launcher {
    public static void main(String[] args) {
        // Forçar timezone do Brasil para conversão correta de timestamps com o banco UTC
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        Main.main(args);
    }
}
