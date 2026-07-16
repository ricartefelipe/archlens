package com.demo.billing;

import com.demo.orders.Order;

import javax.ejb.Stateless;

@Stateless
public class InvoiceProcessor {

    public double process(Order order) {
        double subtotal = order.getTotal();
        double discount = applyDiscount(subtotal);
        double tax = applyTax(subtotal - discount);
        double shipping = calculateShipping(order);
        double fee = calculateFee(order);
        double total = subtotal - discount + tax + shipping + fee;
        audit(subtotal, discount, tax, shipping, fee, total);
        return total;
    }

    private double applyDiscount(double subtotal) {
        if (subtotal > 1000) {
            return subtotal * 0.1;
        }
        if (subtotal > 500) {
            return subtotal * 0.05;
        }
        return 0;
    }

    private double applyTax(double taxable) {
        return taxable * 0.15;
    }

    private double calculateShipping(Order order) {
        if (order.getTotal() > 200) {
            return 0;
        }
        return 19.9;
    }

    private double calculateFee(Order order) {
        return order.getCustomerName() == null ? 2.5 : 0;
    }

    private void audit(
            double subtotal,
            double discount,
            double tax,
            double shipping,
            double fee,
            double total
    ) {
        System.out.println("invoice subtotal=" + subtotal);
        System.out.println("invoice discount=" + discount);
        System.out.println("invoice tax=" + tax);
        System.out.println("invoice shipping=" + shipping);
        System.out.println("invoice fee=" + fee);
        System.out.println("invoice total=" + total);
        System.out.println("invoice audit trail placeholder line 1");
        System.out.println("invoice audit trail placeholder line 2");
        System.out.println("invoice audit trail placeholder line 3");
        System.out.println("invoice audit trail placeholder line 4");
        System.out.println("invoice audit trail placeholder line 5");
        System.out.println("invoice audit trail placeholder line 6");
        System.out.println("invoice audit trail placeholder line 7");
        System.out.println("invoice audit trail placeholder line 8");
        System.out.println("invoice audit trail placeholder line 9");
        System.out.println("invoice audit trail placeholder line 10");
        System.out.println("invoice audit trail placeholder line 11");
        System.out.println("invoice audit trail placeholder line 12");
        System.out.println("invoice audit trail placeholder line 13");
        System.out.println("invoice audit trail placeholder line 14");
        System.out.println("invoice audit trail placeholder line 15");
        System.out.println("invoice audit trail placeholder line 16");
        System.out.println("invoice audit trail placeholder line 17");
        System.out.println("invoice audit trail placeholder line 18");
        System.out.println("invoice audit trail placeholder line 19");
        System.out.println("invoice audit trail placeholder line 20");
        System.out.println("invoice audit trail placeholder line 21");
        System.out.println("invoice audit trail placeholder line 22");
        System.out.println("invoice audit trail placeholder line 23");
        System.out.println("invoice audit trail placeholder line 24");
        System.out.println("invoice audit trail placeholder line 25");
        System.out.println("invoice audit trail placeholder line 26");
        System.out.println("invoice audit trail placeholder line 27");
        System.out.println("invoice audit trail placeholder line 28");
        System.out.println("invoice audit trail placeholder line 29");
        System.out.println("invoice audit trail placeholder line 30");
    }
}
