/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.soft.jmstestapp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 * @author xitizbhatia
 */
public class JMSProducerApp {
    private static final Logger log = Logger.getLogger(JMSProducerApp.class.getName());
    private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/YYYY HH:mm:ss");

    // Set up all the default values
    
    private static final String DEFAULT_CONNECTION_FACTORY = "jms/RemoteConnectionFactory";
    private static final String DEFAULT_DESTINATION = "jms/queue/test";
    private static final String DEFAULT_USERNAME = "jmsuser";
    private static final String DEFAULT_PASSWORD = "jmsuser";
    private static final String INITIAL_CONTEXT_FACTORY = "org.wildfly.naming.client.WildFlyInitialContextFactory";
    private static final String PROVIDER_URL = "http-remoting://127.0.0.1:8080";

    public static void main(String[] args) {
        log.setLevel(Level.ALL);
        JMSProducerApp producer = new JMSProducerApp();
        producer.execute();
    }
    
    public void execute() {
        Context namingContext = null;

        try {
            String userName = System.getProperty("username", DEFAULT_USERNAME);
            String password = System.getProperty("password", DEFAULT_PASSWORD);

            // Set up the namingContext for the JNDI lookup
            final Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
            env.put(Context.PROVIDER_URL, System.getProperty(Context.PROVIDER_URL, PROVIDER_URL));
            env.put(Context.SECURITY_PRINCIPAL, userName);
            env.put(Context.SECURITY_CREDENTIALS, password);
            namingContext = new InitialContext(env);

            // Perform the JNDI lookups
            String connectionFactoryString = System.getProperty("connection.factory", DEFAULT_CONNECTION_FACTORY);
            log.info("Attempting to acquire connection factory \"" + connectionFactoryString + "\"");
            ConnectionFactory connectionFactory = (ConnectionFactory) namingContext.lookup(connectionFactoryString);
            log.info("Found connection factory \"" + connectionFactoryString + "\" in JNDI");

            String destinationString = System.getProperty("destination", DEFAULT_DESTINATION);
            log.info("Attempting to acquire destination \"" + destinationString + "\"");
            Destination destination = (Destination) namingContext.lookup(destinationString);
            log.info("Found destination \"" + destinationString + "\" in JNDI");
            int count = 0; int autosendCount = 100;
            try (JMSContext context = connectionFactory.createContext(userName, password)) {
                log.info("Type messages in a line. Type exit to quit. ");
                
                // Using Scanner for Getting Input from User 
                Scanner in = new Scanner(System.in); 
                while(true) {
                    String s = in.nextLine();
                    if(s.equals("autosend")) {
                        JMSProducer producer = context.createProducer();
                        for(int i=0; i<autosendCount;i++) {
                            producer.send(destination, "message"+String.valueOf(i));
                        }
                        log.info("Sent " + autosendCount + " messages @ " + sdf.format(new Date()));
                        
                    } if(s.equals("exception")) {
                        JMSProducer producer = context.createProducer();
                        for(int i=0; i<5;i++) {
                            producer.send(destination, "message"+String.valueOf(i));
                        }
                        producer.send(destination, "exception");
                        for(int i=6; i<10;i++) {
                            producer.send(destination, "message"+String.valueOf(i));
                        }
                        log.info("Sent 10 messages including exception @ " + sdf.format(new Date()));
                    } else {
                       log.info("Sending " + ++count + " message with content: " + s  + " @ " + sdf.format(new Date()));
                        context.createProducer().send(destination, s); 
                    }
                    if(s.equalsIgnoreCase("exit"))
                        break;
                }
            }
        } catch (NamingException e) {
            log.severe(e.getMessage());
        } finally {
            if (namingContext != null) {
                try {
                    namingContext.close();
                } catch (NamingException e) {
                    log.severe(e.getMessage());
                }
            }
        }
    }
}
