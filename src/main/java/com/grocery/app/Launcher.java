package com.grocery.app;

/**
 * Launcher class - does NOT extend Application.
 * This is needed for fat-JAR execution because the module system
 * checks if the main class extends Application, which breaks with classpath JARs.
 * Using a plain Launcher class avoids this restriction.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
