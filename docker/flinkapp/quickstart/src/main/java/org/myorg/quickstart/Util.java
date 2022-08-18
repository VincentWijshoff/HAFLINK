package org.myorg.quickstart;

import io.github.cdimascio.dotenv.Dotenv;

public class Util {
    public static String getEnvVar(String name){
        Dotenv dotenv = Dotenv.load();
        return dotenv.get(name);
    }
}
