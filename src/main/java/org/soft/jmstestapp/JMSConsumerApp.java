/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.soft.jmstestapp;
import java.util.logging.Logger;
import java.util.Properties;
import java.util.logging.Level;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
/**
 *
 * @author xitizbhatia
 */
public class JMSConsumerApp {
    private static final Logger log = Logger.getLogger(JMSConsumerApp.class.getName());
    private Context namingContext = null;
    private JMSContext context = null;
    private Destination destination = null;
    private volatile boolean stopConsumer = false;
    // Set up all the default values
    private static final String DEFAULT_CONNECTION_FACTORY = "jms/RemoteConnectionFactory";
    private static final String DEFAULT_DESTINATION = "jms/queue/test";
    private static final String DEFAULT_USERNAME = "jmsuser";
    private static final String DEFAULT_PASSWORD = "jmsuser";
    private static final String INITIAL_CONTEXT_FACTORY = "org.wildfly.naming.client.WildFlyInitialContextFactory";
    private static final String PROVIDER_URL = "http-remoting://127.0.0.1:8080";

    public static void main(String[] args) {
        JMSConsumerApp consumer = new JMSConsumerApp();
        consumer.execute();
        
    }
    
    public void execute() {
        try {
            connect();
            try {
                
                log.info("Waiting for messages");
                
                // Create the JMS consumer
                JMSConsumer consumer = context.createConsumer(destination);
                consumer.setMessageListener(new JMSMessageListener(this));
                // Then receive the same number of messages that were sent
//                while(true) {
//                    String text = consumer.receiveBody(String.class);
//                    log.info("Received message with content " + text);
                while (true) {
                    Thread.sleep(1000);
                    if(stopConsumer)
                        break;
                }
//                    if(text.equalsIgnoreCase("exit"))
//                        break;
//                }
                
            
            } catch (InterruptedException ex) {
                Logger.getLogger(JMSConsumerApp.class.getName()).log(Level.SEVERE, "interrupted consumer thread thread", ex);
            }
        } catch (NamingException e) {
            log.severe(e.getMessage());
        } catch(Throwable ex) {
            log.severe(ex.getMessage());
        } finally {
            if(context != null) {
                context.close();
            }
            if (namingContext != null) {
                try {
                    namingContext.close();
                } catch (NamingException e) {
                    log.severe(e.getMessage());
                }
            }
        }
    }
    
    public void exit() {
        stopConsumer = true;
    }
    
    public void connect() throws NamingException {
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
        destination = (Destination) namingContext.lookup(destinationString);
        log.info("Found destination \"" + destinationString + "\" in JNDI");
        context = connectionFactory.createContext(userName, password);

    }
    
}
