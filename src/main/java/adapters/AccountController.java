package adapters;

import domain.DTUPayAccountBusinessLogic;
import domain.exception.DuplicateBankAccountException;
import domain.exception.NoSuchAccountException;
import domain.model.DTUPayAccount;
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
        queue.addHandler("BankAccountRequested", this::handleExportBankAccountRequest);
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
        String deleteMsg = "Account with id: " + account.getId() + " is succesfully deleted";
        // Publish event
        Event accDeleteSucceeded = new Event("AccountDeletedSucceeded", new Object[] {deleteMsg});
        queue.publish(accDeleteSucceeded);

    }

    /**
     * Consumes events of type GetBankAccountRequested
     *
     * @author s184174
     * @param event
     */
    public void handleExportBankAccountRequest(Event event) {
        var id = event.getArgument(0, String.class);

        // Get account
        String accountId = "";
        try {
           DTUPayAccount account = accountLogic.get(id);
           accountId = account.getDtuBankAccount();
        } catch (NoSuchAccountException e) {
            // Publish event
            Event accDeleteFailed = new Event("AccountExportFailed", new Object[] {e.getMessage()});
            queue.publish(accDeleteFailed);
        }

        // Publish event
        Event accDeleteSucceeded = new Event("AccountExportSucceeded", new Object[] {accountId});
        queue.publish(accDeleteSucceeded);

    }
}
