package adapters;

import domain.DTUPayAccountBusinessLogic;
import domain.exception.DuplicateBankAccountException;
import domain.exception.NoSuchAccountException;
import domain.model.DTUPayAccount;
import domain.model.PaymentEvent;
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
        queue.addHandler("DeleteAccount", this::handleDeleteAccountRequest);
        queue.addHandler("ExportBankAccounts", this::handleExportBankAccountsRequest);
    }

    /**
     * Consumes events of type CreateCustomerAccount
     *
     * @author s212358
     * @param event
     */
    public void handleCreateCustomerAccountRequest(Event event) {
        var account = event.getArgument(0, DTUPayAccount.class);

        try {
            // Create account
            accountLogic.createAccount(account);
        } catch (DuplicateBankAccountException e) {
            // Publish event
            Event accCreationFailed = new Event("CustomerAccountCreatedFailed", new Object[] {e.getMessage()});
            queue.publish(accCreationFailed);
        }

        // Publish event for facade
        Event accCreationSucceeded = new Event("CustomerAccountCreatedSucceeded", new Object[] {account});
        queue.publish(accCreationSucceeded);

        // Publish event for token
        Event tokenAssign = new Event("AssignTokensToCustomer", new Object[] {account});
        queue.publish(tokenAssign);
    }
    /**
     * Merchant events of type CreateMerchantAccount
     *
     * @author s212358
     * @param event
     */
    public void handleCreateMerchantAccountRequest(Event event) {
        var account = event.getArgument(0, DTUPayAccount.class);

        try {
            // Create account
            accountLogic.createAccount(account);
        } catch (DuplicateBankAccountException e) {
            // Publish event
            Event accCreationFailed = new Event("MerchantAccountCreatedFailed", new Object[] {e.getMessage()});
            queue.publish(accCreationFailed);
        }

        // Publish event
        Event accCreationSucceeded = new Event("MerchantAccountCreatedSucceeded", new Object[] {account});
        queue.publish(accCreationSucceeded);
    }

    /**
     * Consumes events of type DeleteAccount
     *
     * @author s184174
     * @param event
     */
    public void handleDeleteAccountRequest(Event event) {
        var account = event.getArgument(0, DTUPayAccount.class);

        // Delete account
        try {
            accountLogic.delete(account);
        } catch (NoSuchAccountException e) {
            // Publish event
            Event accDeleteFailed = new Event("AccountDeletedFailed", new Object[] {e.getMessage()});
            queue.publish(accDeleteFailed);
        }
        String deleteMsg = "Account with id: " + account.getId() + " is successfully deleted";
        // Publish event
        Event accDeleteSucceeded = new Event("AccountDeletedSucceeded", new Object[] {deleteMsg});
        queue.publish(accDeleteSucceeded);

    }

    /**
     * Consumes events of type ExtractBankAccounts and publishes an event that includes the PaymentEvent with
     * extracted the customer and merchant bank accounts from their respective ids.
     *
     * @author s184174
     * @param event
     */
    public void handleExportBankAccountsRequest(Event event) {
        PaymentEvent paymentEvent = event.getArgument(0, PaymentEvent.class);

        // Get account
        String accountId = "";
        try {
            // Get customer bank account
            DTUPayAccount customerAccount = accountLogic.get(paymentEvent.getCustomerId());
            String customerBankAccount = customerAccount.getDtuBankAccount();
            paymentEvent.setCustomerBankAccount(customerBankAccount);

            // Get merchant bank account
            DTUPayAccount merchantAccount = accountLogic.get(paymentEvent.getMerchantId());
            String merchantBankAccount = merchantAccount.getDtuBankAccount();
            paymentEvent.setMerchantBankAccount(merchantBankAccount);
        } catch (NoSuchAccountException e) {
            // Publish event
            Event accExtractedFailed = new Event("AccountsExportFailed", new Object[] {e.getMessage()});
            queue.publish(accExtractedFailed);
        }

        // Publish event for the payment microservice to complete the payment
        Event accExportedSucceeded = new Event("BankAccountsExported", new Object[] {paymentEvent});
        queue.publish(accExportedSucceeded);

    }
}
