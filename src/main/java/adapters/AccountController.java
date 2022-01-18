package adapters;

import domain.DTUPayAccountBusinessLogic;
import domain.exception.DuplicateBankAccountException;
import domain.exception.NoSuchAccountException;
import domain.model.*;
import domain.storage.InMemory;
import messaging.Event;
import messaging.MessageQueue;

public class AccountController {
    MessageQueue queue;
    DTUPayAccountBusinessLogic accountLogic;

    /**
     * Delegate events to handlers
     *
     * @param queue
     * @param memory
     */
    public AccountController(MessageQueue queue, InMemory memory) {
        accountLogic = new DTUPayAccountBusinessLogic(memory);
        // todo: make handlers for each event Account need to look at
        queue.addHandler("CreateCustomerAccount", this::handleCreateCustomerAccountRequest);
        queue.addHandler("CreateMerchantAccount", this::handleCreateMerchantAccountRequest);
        queue.addHandler("ExportBankAccounts", this::handleExportBankAccountsRequest);
        queue.addHandler("DeleteAccount", this::handleDeleteAccountRequest);
        queue.addHandler("TokenSupplyResponse", this::handleTokenSupplyResponse);
    }

    /**
     * Consumes events of type CreateCustomerAccount and published a TokenSupplyEvent or a not completed ResponseEvent.
     * In case of a "not completed" (false) it will publish a ResponseEvent with the requestId and the
     * propagated error message.
     *
     * @author s212358
     * @param event
     */
    public void handleCreateCustomerAccountRequest(Event event) {
        // Get arguments
        var accountEvent = event.getArgument(0, CreateAccountEvent.class);

        // Init account model
        DTUPayAccount account = new DTUPayAccount("", accountEvent.getName(), accountEvent.getCpr(), accountEvent.getDtuBankAccount());

        try {
            // Create account
            accountLogic.createAccount(account);
        } catch (DuplicateBankAccountException e) {
            // Publish event
            ResponseEvent response = new ResponseEvent(accountEvent.getRequestId(), e.getMessage(), false);
            Event accCreationFailed = new Event("CustomerAccountCreatedFailed", new Object[] {response});
            queue.publish(accCreationFailed);
        }

        // Publish event for token
        SupplyTokenEvent supplyEvent = new SupplyTokenEvent(accountEvent.getRequestId(), account.getId());
        Event tokenAssign = new Event("AssignTokensToCustomer", new Object[] {supplyEvent});
        queue.publish(tokenAssign);
    }

    /**
     * Consumes events of type CreateMerchantAccount and published a completed or not completed ResponseEvent.
     * In case of a "not completed" (false) it will publish a ResponseEvent with the requestId and the
     * propagated error message.
     *
     * @author s212358
     * @param event
     */
    public void handleCreateMerchantAccountRequest(Event event) {
        // Get arguments
        var accountEvent = event.getArgument(0, CreateAccountEvent.class);

        // Init account model
        DTUPayAccount account = new DTUPayAccount("", accountEvent.getName(), accountEvent.getCpr(), accountEvent.getDtuBankAccount());

        try {
            // Create account
            accountLogic.createAccount(account);
        } catch (DuplicateBankAccountException e) {
            // Publish event
            ResponseEvent response = new ResponseEvent(accountEvent.getRequestId(), e.getMessage(), false);
            Event accCreationFailed = new Event("MerchantAccountCreatedFailed", new Object[] {response});
            queue.publish(accCreationFailed);
        }

        // Publish event
        ResponseEvent response = new ResponseEvent(accountEvent.getRequestId(), "Merchant Account is successfully created!", true);
        Event accCreationSucceeded = new Event("MerchantAccountCreateResponse", new Object[] {response});
        queue.publish(accCreationSucceeded);
    }

    /**
     * Consumes events of type DeleteAccount and published a completed or not completed ResponseEvent.
     * In case of a "not completed" (false) it will publish a ResponseEvent with the requestId and the
     * propagated error message.
     *
     * @author s184174
     * @param event
     */
    public void handleDeleteAccountRequest(Event event) {
        // Get arguments
        DeleteAccountEvent accountEvent = event.getArgument(0, DeleteAccountEvent.class);

        // Delete account
        try {
            DTUPayAccount account = accountLogic.get(accountEvent.getId());
            accountLogic.delete(account);
        } catch (NoSuchAccountException e) {
            // Publish response event for facade with propagated error message
            ResponseEvent response = new ResponseEvent(accountEvent.getRequestId(), e.getMessage(), false);
            Event accDeleteFailed = new Event("AccountDeletedFailed", new Object[] {response});
            queue.publish(accDeleteFailed);
        }

        // Publish event for facade
        String deleteMsg = "Account with id: " + accountEvent.getId() + " is successfully deleted";
        ResponseEvent response = new ResponseEvent(accountEvent.getRequestId(), deleteMsg, false);
        Event accDeleteSucceeded = new Event("AccountDeletedSucceeded", new Object[] {response});
        queue.publish(accDeleteSucceeded);
    }

    /**
     * Consumes events of type ExtractBankAccounts and publishes an event that includes the
     * PaymentEvent with  extracted the customer and merchant bank accounts from their respective ids.
     * In case of failure the published event will be a ResponseEvent with the requestId and the
     * propagated error message.
     *
     * @author s184174
     * @param event
     */
    public void handleExportBankAccountsRequest(Event event) {
        // Get arguments
        PaymentEvent paymentEvent = event.getArgument(0, PaymentEvent.class);

        // Get account
        String accountId = "";
        try {
            // Set customer bank account to payment event
            DTUPayAccount customerAccount = accountLogic.get(paymentEvent.getCustomerId());
            paymentEvent.setCustomerBankAccount(customerAccount.getDtuBankAccount());

            // Set merchant bank account to payment event
            DTUPayAccount merchantAccount = accountLogic.get(paymentEvent.getMerchantId());
            paymentEvent.setMerchantBankAccount(merchantAccount.getDtuBankAccount());
        } catch (NoSuchAccountException e) {
            // Publish response event for the payment microservice
            ResponseEvent response = new ResponseEvent(paymentEvent.getRequestId(), e.getMessage(), false);
            Event accExtractedFailed = new Event("BankAccountsExportFailed", new Object[] {response});
            queue.publish(accExtractedFailed);
        }

        // Publish payment event for the payment microservice to complete the payment
        Event accExportedSucceeded = new Event("BankAccountsExported", new Object[] {paymentEvent});
        queue.publish(accExportedSucceeded);
    }

    /**
     * Consumes events of type TokenSupplyResponse and published a completed or not completed ResponseEvent.
     *
     * Based on the "completed" flag of the ResponseEvent:
     * If the token supply response is "completed" (true) then a completed ResponseEvent will be published for the customer account,
     * otherwise it will publish a "not completed" (false) ResponseEvent with propagated error message from the token supply.
     *
     * @param event
     */
    public void handleTokenSupplyResponse(Event event) {
        // Get arguments
        ResponseEvent response = event.getArgument(0, ResponseEvent.class);

        // Check if token supply failed
        if (!response.isCompleted()){
            // Propagate error tto facade
            Event accExportedSucceeded = new Event("AccountCreateResponse", new Object[] {response});
        }

        // Override with customer account create message
        response.setMessage("Customer Account is successfully created!");

        // Publish response event for facade
        Event accCreationSucceeded = new Event("CustomerAccountCreateResponse", new Object[] {response});
        queue.publish(accCreationSucceeded);
    }
}
