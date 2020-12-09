/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.soft.jmstestapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 *
 * @author xitizbhatia
 */
public class JMSMessageListener implements MessageListener {
    private static final Logger log = Logger.getLogger(JMSProducerApp.class.getName());
    private JMSConsumerApp consumer = null;
    private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/YYYY HH:mm:ss");
    public JMSMessageListener(JMSConsumerApp consumer) {
        this.consumer = consumer;
    }
    
    @Override
    public void onMessage(Message message) {
        try {
            log.info("Received message with content " + message.getBody(String.class) + " @ " + sdf.format(new Date()));
            if("exit".equalsIgnoreCase(message.getBody(String.class))) {
                log.info("Stopping the consumer");
                Thread.sleep(1000);
                consumer.exit();
            } else if("exception".equalsIgnoreCase(message.getBody(String.class))) {
                log.info("throwing an exception");
                Thread.sleep(500);
                String a = null;
                a.toLowerCase();
            } else {
                Thread.sleep(500);
                log.info("Finished processing content " + message.getBody(String.class) + " @ " + sdf.format(new Date()));
            }
        } catch (JMSException ex) {
            Logger.getLogger(JMSMessageListener.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(JMSMessageListener.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
