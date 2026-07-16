package com.demo.billing;

import com.demo.orders.Order;
import com.demo.orders.OrderNotifier;

import javax.ejb.Stateless;

@Stateless
public class BillingService {

    private final OrderNotifier orderNotifier = new OrderNotifier();

    public void registerPayment(Long orderId, double amount) {
        Order order = new Order();
        order.setId(orderId);
        order.setTotal(amount);
        orderNotifier.notifyPaid(order);
    }

    public double calculateTax(double amount) {
        return amount * 0.15;
    }
}
