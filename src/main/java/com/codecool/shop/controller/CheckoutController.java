package com.codecool.shop.controller;

import com.codecool.shop.dao.AllOrdersDao;
import com.codecool.shop.dao.CustomerDao;
import com.codecool.shop.dao.OrderDao;
import com.codecool.shop.dao.db_implementation.AllOrdersDaoJDBC;
import com.codecool.shop.dao.implementation.CustomerDaoMem;
import com.codecool.shop.model.Customer;
import com.codecool.shop.service.PageCoordinator;
import com.codecool.shop.service.SessionManager;
import com.codecool.shop.service.form.UserDataForm;
import com.codecool.shop.util.HashPassword;
import com.codecool.shop.util.Message;
import com.codecool.shop.util.Util;
import com.codecool.shop.util.Validator;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = {"/checkout"})
public class CheckoutController extends MainServlet {

    /**
     * Method get - renders template for checkout if user has items in cart/order
     * @param req - Http request
     * @param resp - Http response
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        OrderDao order = SessionManager.getOrderFromSession(req);

        if (order.countProducts() <= 0) {
            SessionManager.setMessageForUser(req, Message.EMPTY_CART.getMessage());
            resp.sendRedirect("/cart");
        } else {
            renderTemplate(req, resp, "/checkoutTemplate.html", null);
        }SessionManager.clearAllMessages(req);
    }

    /**
     * Method post handles checkout form
     * It distinguishes if user is already logged or not
     * Depends of customer data validation adds messages for user
     * Updates / or adds customer into all customers collection
     * And if customer is new - sends a mail
     *
     * @param req - Http request
     * @param resp - Http response
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CustomerDao allCustomers = CustomerDaoMem.getInstance();// JDBC
        UserDataForm userDataForm = new UserDataForm();

        if (Util.isCustomerLogged(req)) {
            Customer loggedCustomer = SessionManager.getCustomerFromSession(req);
            userDataForm.setCustomerData(req);
            if (loggedCustomer.getCustomerData(loggedCustomer).equals(userDataForm.getCustomerData())) {
                saveOrderAndGoToPayment(req, resp, loggedCustomer.getId());
            } else {
                Customer changedCustomer =
                            new Customer(userDataForm.getCustomerData(),
                                    loggedCustomer.getEmail(),
                                    loggedCustomer.getPassword());
                if (Validator.isMessage(userDataForm.getMessages())) {
                    SessionManager.setMessageForUser(req, userDataForm.getMessages());
                    SessionManager.setCustomerInSession(req, changedCustomer);
                    resp.sendRedirect("/checkout");

                } else {
                    SessionManager.setCustomerInSession(req, changedCustomer);
                    int customerId = allCustomers.getCustomerId(changedCustomer.getEmail());
                    saveOrderAndGoToPayment(req, resp, customerId);
                }
            }
        // customer is not logged
        } else {
            userDataForm.setCustomerData(req);
            userDataForm.setEmail(req);
            String password = userDataForm.getValidPassword(req);

            if (allCustomers.isExistsEmail(userDataForm.getEmail())) {
                userDataForm.addMessage(Message.EMAIL_EXISTS.getMessage());
            }
            if (Validator.isMessage(userDataForm.getMessages())) {
                Customer notVerifiedCustomer = new Customer(userDataForm.getCustomerData(), userDataForm.getEmail(), "");
                SessionManager.setMessageForUser(req, userDataForm.getMessages());
                SessionManager.setCustomerInSession(req, notVerifiedCustomer);
                resp.sendRedirect("/checkout");

            } else {
                String hashedPassword = HashPassword.getHashed(password);
                Customer verifiedCustomer = new Customer(userDataForm.getCustomerData(), userDataForm.getEmail(), hashedPassword);
                int customerId = allCustomers.addCustomer(verifiedCustomer);
                SessionManager.setCustomerInSession(req, allCustomers.findById(customerId));
                SessionManager.userVerified(req);
//                PageCoordinator.sendMail(userDataForm.getEmail());
                saveOrderAndGoToPayment(req, resp, customerId);
            }
        }
    }
    /**
     * Additional method to add customerId to order and save order in database
     * @param req - Http request
     * @param resp - Http response
     * @param customerId - int customerId
     * @throws IOException
     */

    private void saveOrderAndGoToPayment(HttpServletRequest req, HttpServletResponse resp, int customerId) throws IOException{
        OrderDao currentOrder = SessionManager.getOrderFromSession(req);
        AllOrdersDao allOrders = new AllOrdersDaoJDBC();

        currentOrder.addCustomerId(customerId);
        OrderDao savedOrder = allOrders.addFullOrderAndReturn(currentOrder.getOrder());
        SessionManager.setOrderInSession(req, savedOrder);
        PageCoordinator.renderTemplate(req, resp, "/paymentTemplate.html", null);

    }
}