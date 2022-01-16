package adapters;

import domain.DTUPayAccountBusinessLogic;
import domain.DuplicateBankAccountException;
import domain.NoSuchAccountException;
import domain.model.DTUPayAccount;
import domain.storage.InMemory;
import messaging.Event;
import messaging.MessageQueue;

public class AccountController {

    MessageQueue queue;
    DTUPayAccountBusinessLogic accountLogic;

    /**
     * Delegate events to c
     *
     * @param q
     * @param memory
     */
    public AccountController(MessageQueue q, InMemory memory) {
        queue = q;
        accountLogic = new DTUPayAccountBusinessLogic(memory);
        // todo: make handlers for each event Account need to look at
        queue.addHandler("CreateAccount", this::handleCreateAccountRequest);
        queue.addHandler("DeleteAccount", this::handleDeleteAccountRequest);
        queue.addHandler("BankAccountRequested", this::handleExportBankAccountRequest);
    }

    public void handleCreateAccountRequest(Event event) {
        var account = event.getArgument(0, DTUPayAccount.class);

        try {
            // Create account
            accountLogic.createAccount(account);
        } catch (DuplicateBankAccountException e) {
            // Publish event
            Event accCreationFailed = new Event("AccountCreatedFailed", new Object[] {e.getMessage()});
            queue.publish(accCreationFailed);
        }

        // Publish event
        Event accCreationSucceeded = new Event("AccountCreatedSucceeded", new Object[] {account});
        queue.publish(accCreationSucceeded);
    }

    /**
     * Consumes events of type DeleteAccount
     *
     * @author Mohammad
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

        // Publish event
        Event accDeleteSucceeded = new Event("AccountDeletedSucceeded", new Object[] {account});
        queue.publish(accDeleteSucceeded);

    }
}
