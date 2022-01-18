package adapters;

import domain.model.*;
import domain.storage.InMemory;
import domain.DTUPayAccountBusinessLogic;
import domain.exception.DuplicateBankAccountException;
import domain.exception.NoSuchAccountException;
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
        queue.addHandler("CustomerTokensSupplied", this::handleTokenSupplyResponse);
    }

    /**
     * Consumes events of type CreateCustomerAccount and published an event in queue SupplyCustomerWithTokens
     *
     * Consumed event arguments:
     * 1. requestId
     * 2. DTUPayAccount
     * 3. errorMessage
     *
     * Successful event arguments:
     * 1. requestId
     * 2. customerId
     *
     * Failed event arguments:
     * 1. requestId
     * 2. null
     * 3. error message
     *
     * @author s212358
     * @param event
     */
    public void handleCreateCustomerAccountRequest(Event event) {
        // Publish propagated error, if any
        String requestId = event.getArgument(0, String.class);
        String errorMessage = event.getArgument(2, String.class);
        this.publishPropagatedError("CustomerAccountCreated", requestId, errorMessage);

        // Create account
        DTUPayAccount account = event.getArgument(1, DTUPayAccount.class);
        try {
            accountLogic.createAccount(account);
        } catch (DuplicateBankAccountException e) {
            // Publish event with propagated error
            Event accCreationFailed = new Event("CustomerAccountCreated", new Object[] {requestId, null, e.getMessage()});
            queue.publish(accCreationFailed);
        }

        // Publish event for token
        Event tokenAssign = new Event("CustomerTokenSupplied", new Object[] {requestId, account.getId()});
        queue.publish(tokenAssign);
    }

    /**
     *
     * Consumes events of type CreateMerchantAccount and published an event in queue CustomerAccountCreated
     *
     * Consumed event arguments:
     * 1. requestId
     * 2. DTUPayAccount
     * 3. errorMessage
     *
     * Successful event arguments:
     * 1. requestId
     * 2. customerId
     *
     * Failed event arguments:
     * 1. requestId
     * 2. null
     * 3. error message
     *
     * @author s212358
     * @param event
     */
    public void handleCreateMerchantAccountRequest(Event event) {
        // Publish propagated error, if any
        String requestId = event.getArgument(0, String.class);
        String errorMessage = event.getArgument(2, String.class);
        this.publishPropagatedError("MerchantAccountCreated", requestId, errorMessage);

        // Create account
        DTUPayAccount account = event.getArgument(1, DTUPayAccount.class);
        try {
            accountLogic.createAccount(account);
        } catch (DuplicateBankAccountException e) {
            // Publish event with propagated error
            Event accCreationFailed = new Event("MerchantAccountCreated", new Object[] {requestId, null, e.getMessage()});
            queue.publish(accCreationFailed);
        }

        // Publish event for the facade
        Event accCreationSucceeded = new Event("MerchantAccountCreated", new Object[] {requestId, "Merchant Account is successfully created with id: " + account.getId()});
        queue.publish(accCreationSucceeded);
    }

    /**
     * Consumes events of type DeleteAccount and published an event in queue AccountDeleted
     *
     * Consumed event arguments:
     * 1. requestId
     * 2. accountId
     * 3. errorMessage
     *
     * Successful event arguments:
     * 1. requestId
     * 2. success message
     *
     * Failed event arguments:
     * 1. requestId
     * 2. null
     * 3. error message
     *
     * @author s184174
     * @param event
     */
    public void handleDeleteAccountRequest(Event event) {
        // Publish propagated error, if any
        String requestId = event.getArgument(0, String.class);
        String errorMessage = event.getArgument(2, String.class);
        this.publishPropagatedError("AccountDeleted", requestId, errorMessage);

        // Delete account
        String accountId = event.getArgument(1, String.class);
        try {
            DTUPayAccount account = accountLogic.get(accountId);
            accountLogic.delete(account);
        } catch (NoSuchAccountException e) {
            // Publish response event for facade with propagated error message
            Event accDeleteFailed = new Event("AccountDeleted", new Object[] {requestId, null, e.getMessage()});
            queue.publish(accDeleteFailed);
        }

        // Publish event for facade
        Event accDeleteSucceeded = new Event("AccountDeleted", new Object[] {requestId, "Account with id: " + accountId + " is successfully deleted"});
        queue.publish(accDeleteSucceeded);
    }

    /**
     * Consumes events of type ExtractBankAccounts and published an event in queue BankAccountsExported
     *
     *  Consumed event arguments:
     * 1. requestId
     * 2. PaymentPayload
     * 3. errorMessage
     *
     * Successful event arguments:
     * 1. requestId
     * 2. PaymentPayload
     *
     * Failed event arguments:
     * 1. requestId
     * 2. null
     * 3. error message
     *
     * @author s184174
     * @param event
     */
    public void handleExportBankAccountsRequest(Event event) {
        // Publish propagated error, if any
        String requestId = event.getArgument(0, String.class);
        String errorMessage = event.getArgument(2, String.class);
        this.publishPropagatedError("BankAccountsExported", requestId, errorMessage);

        // Get account
        PaymentPayload paymentEvent = event.getArgument(1, PaymentPayload.class);
        try {
            // Set customer bank account to payment event
            DTUPayAccount customerAccount = accountLogic.get(paymentEvent.getCustomerId());
            paymentEvent.setCustomerBankAccount(customerAccount.getDtuBankAccount());

            // Set merchant bank account to payment event
            DTUPayAccount merchantAccount = accountLogic.get(paymentEvent.getMerchantId());
            paymentEvent.setMerchantBankAccount(merchantAccount.getDtuBankAccount());
        } catch (NoSuchAccountException e) {
            // Publish response event for the payment microservice
            Event accExtractedFailed = new Event("BankAccountsExported", new Object[] {requestId, null, e.getMessage()});
            queue.publish(accExtractedFailed);
        }

        // Publish payment event for the payment microservice to complete the payment
        Event accExportedSucceeded = new Event("BankAccountsExported", new Object[] {requestId, paymentEvent});
        queue.publish(accExportedSucceeded);
    }

    /**
     * Consumes events of type CustomerTokensSupplied and published an event in queue CustomerAccountCreated
     *
     *  Consumed event arguments:
     * 1. requestId
     * 2. customerId
     * 3. errorMessage
     *
     * Successful event arguments:
     * 1. requestId
     * 2. success message
     *
     * Failed event arguments:
     * 1. requestId
     * 2. null
     * 3. error message
     *
     * @param event
     */
    public void handleTokenSupplyResponse(Event event) {
        // Publish propagated error, if any
        String requestId = event.getArgument(0, String.class);
        String errorMessage = event.getArgument(2, String.class);
        this.publishPropagatedError("CustomerAccountCreated", requestId, errorMessage);

        // Publish response event for facade
        String customerId = event.getArgument(0, String.class);
        Event accCreationSucceeded = new Event("CustomerAccountCreated", new Object[] {requestId, "Merchant Account is successfully created with id: " + customerId});
        queue.publish(accCreationSucceeded);
    }

    /**
     * It propagates error messages sent by the consumed events.
     *
     * @param eventName
     * @param requestId
     * @param errorMessage
     */
    private void publishPropagatedError(String eventName, String requestId, String errorMessage) {
        // Publish propagated error, if any
        if (errorMessage == null) {
            // Publish event
            Event errorPropagated = new Event(eventName, new Object[] {requestId, null, errorMessage});
            queue.publish(errorPropagated);
        }
    }
}
