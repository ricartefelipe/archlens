package com.demo.messaging;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

public class OrderQueueListener implements MessageListener {

    @Override
    public void onMessage(Message message) {
        try {
            System.out.println("Received JMS message id=" + message.getJMSMessageID());
        } catch (JMSException e) {
            System.out.println("Failed to read JMS message");
        }
    }
}
