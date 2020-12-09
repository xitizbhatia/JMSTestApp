# JMSTestApp
JMS java application with a producer and consumer (tested with JBoss EAP 7.3)

Download and setupJBoss EAP 7.3 as per standard instructions.

In standalone.xml, add the following (can be copied from standalone-full.xml):

In the extensions section, add:
```xml
      <extension module="org.wildfly.extension.messaging-activemq"/>
```

In the ee:4.0 subsystem, if the code is using properties names in annotations or in the spec based xml files, add the following:
```xml
      <spec-descriptor-property-replacement>true</spec-descriptor-property-replacement>
      <annotation-property-replacement>true</annotation-property-replacement>
```


Add the messaging subsystem (below the mail subsystem but anywhere is fine)
```xml
      <subsystem xmlns="urn:jboss:domain:messaging-activemq:8.0">
            <server name="default">
                <statistics enabled="${wildfly.messaging-activemq.statistics-enabled:${wildfly.statistics-enabled:false}}"/>
                <security-setting name="#">
                    <role name="guest" send="true" consume="true" create-non-durable-queue="true" delete-non-durable-queue="true"/>
                </security-setting>
                <address-setting name="#" dead-letter-address="jms.queue.DLQ" expiry-address="jms.queue.ExpiryQueue" max-size-bytes="10485760" page-size-bytes="2097152" message-counter-history-day-limit="10" max-delivery-attempts="10" redelivery-delay="5000" max-redelivery-delay="5000" redelivery-multiplier="1.0"/>
                <http-connector name="http-connector" socket-binding="http" endpoint="http-acceptor"/>
                <http-connector name="http-connector-throughput" socket-binding="http" endpoint="http-acceptor-throughput">
                    <param name="batch-delay" value="50"/>
                </http-connector>
                <in-vm-connector name="in-vm" server-id="0">
                    <param name="buffer-pooling" value="false"/>
                </in-vm-connector>
                <http-acceptor name="http-acceptor" http-listener="default"/>
                <http-acceptor name="http-acceptor-throughput" http-listener="default">
                    <param name="batch-delay" value="50"/>
                    <param name="direct-deliver" value="false"/>
                </http-acceptor>
                <in-vm-acceptor name="in-vm" server-id="0">
                    <param name="buffer-pooling" value="false"/>
                </in-vm-acceptor>
                <jms-queue name="ExpiryQueue" entries="java:/jms/queue/ExpiryQueue"/>
                <jms-queue name="DLQ" entries="java:/jms/queue/DLQ"/>
                <jms-queue name="FhirConversionQueue" entries="java:/jboss/exported/jms/queue/FhirConversionQueue"/>
                <jms-queue name="TestQ" entries="java:/jboss/exported/jms/queue/TestQ"/>
                <jms-queue name="testQueue" entries="queue/test java:jboss/exported/jms/queue/test"/>
                <connection-factory name="InVmConnectionFactory" entries="java:/ConnectionFactory" connectors="in-vm"/>
                <connection-factory name="RemoteConnectionFactory" reconnect-attempts="-1" retry-interval-multiplier="1.0" retry-interval="1000"  consumer-window-size="0" entries="java:jboss/exported/jms/RemoteConnectionFactory" connectors="http-connector"/>
                <pooled-connection-factory name="activemq-ra" entries="java:/JmsXA java:jboss/DefaultJMSConnectionFactory" connectors="in-vm" transaction="xa"/>
            </server>
        </subsystem>
```


In the above subsystem, if you are relying on this server's http-connector to relay messages to a queue hosted on another JBoss server especially when using an annotation injected JMSContext and connection-factory and not relying on the InitialContext lookup to directly lookup the remote queue, then change the socket-binding for <http-connector> to jms. And in the socket-binding section, create a jms socket-binding by including this:
  
```xml
      <outbound-socket-binding name="jms">
          <remote-destination host="localhost" port="8080"/>
      </outbound-socket-binding>
```


In the server startup script or in the standalone.xml file, add the following properties:
```
    -DsendToJMSQueue=true -DjmsFhirConnectionFactory=java:jboss/exported/jms/RemoteConnectionFactory -DjmsFhirUsername=username -DjmsFhirPassword=password -DjmsFhirQueue=java:/jboss/exported/jms/queue/TestQ
```

Create a user in jboss using the add-user batch/shell script located in the bin directory of JBoss.
Ensure that the username and password is matching the user that you create and ensure that the role of guest is asssigned to the user.

Now, start the server using standalone.sh
Run the JMSProducerApp.java. It will connect to the JMS provider.
Run the JMSConsumerApp.java. It will connect to the JMS provider. If you start the consumer after the messages have been sent already from the producer then they will be held in the queue on the server. If the consumer is stopped then then any messages sent during the time when it was not consuming will get delivered to the consumer after the JMSConsumerApp comes back up.

For messages, type any word or sentences in the window in which JMSProducerApp is running. The messages will be displayed in the JMSConsumerApp window.

Type "exception" to simulate a RunTimeException and see how the JMS Provider re-tries the message before sending it to Dead Letter Queue (DLQ).

Type "autosend" to simulate a burst of messages and see how they get consumed by the JMSConsumerApp. During this if you shutdown the server and restart the server then the remaining messages are delievered showing that the queue is peristent. 

If the ```consumer-window-size="0"``` is removed or changed to have a value of -1 or greater than 0 then that indicates the buffer size on the client end. For example, if if ```consumer-window-size="0"``` is removed then it uses a deafult buffer size of 1MB for the client. As a result, the server sends the messages to the client until its buffer of 1MB is full. And the client then receives messages in its onMessage() listener or the blocking receive() method from this buffer. This can be tested by removing the ```consumer-window-size="0"``` from the above configuration and by shutting down the server as soon as the producer has sent a burst of 100 messages using autosend command typed in the window for JMSProducerApp. Now, even if the server is shutdown, the client will continue to receive the messages from its buffer at a rate of 1 message every 60 seconds. I am not sure if and how this rate of messages from client buffer can be changed but for reliable systems or to prevent duplicates, the ```consumer-window-size="0"``` can be added back to disable any client side buffering of messages.

Type "exit" to exit both the producer and the consumer app.

To ensure that consumer and producer clients keep re-trying to establish a connection to the JMS provider in case the server goes down, this has been added to the connection-factory configuration above:
```
  reconnect-attempts="-1" retry-interval-multiplier="1.0" retry-interval="1000"
```

To ensure that the JMS provider re-tries sending a message when the client threw a RunTimeException, the following has been added to the address-setting in the configuration above:
```
    max-delivery-attempts="10" redelivery-delay="5000" max-redelivery-delay="5000" redelivery-multiplier="1.0"
```

Following three custom queues have been added to the configuration above:
```xml
      
      <jms-queue name="FhirConversionQueue" entries="java:/jboss/exported/jms/queue/FhirConversionQueue"/>
      <jms-queue name="TestQ" entries="java:/jboss/exported/jms/queue/TestQ"/>
      <jms-queue name="testQueue" entries="queue/test java:jboss/exported/jms/queue/test"/>
```

The last one testQueue is used by this application.
