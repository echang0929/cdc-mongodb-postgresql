package com.tutorial.cdc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.logging.Logger;
import java.util.logging.Level;



@SpringBootApplication
@EnableTransactionManagement
public class CdcApplication {

    public static void main(String[] args) {



        Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
        mongoLogger.setLevel(Level.SEVERE); // e.g. or Log.WARNING, etc.

        SpringApplication.run(CdcApplication.class, args);

    }

}
