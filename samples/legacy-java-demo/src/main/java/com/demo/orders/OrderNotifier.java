package com.demo.orders;

import com.demo.billing.BillingService;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class OrderNotifier {

    @Inject
    private BillingService billingService;

    public void notifyPaid(Order order) {
        billingService.registerPayment(order.getId(), order.getTotal());
    }
}
