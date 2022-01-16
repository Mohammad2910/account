package adapters;

import domain.model.DTUPayAccount;
import messaging.Event;
import messaging.MessageQueue;

public class FacadeController {

    MessageQueue queue;

    public FacadeController(MessageQueue q) {
        queue = q;
        // todo: make handlers for each event Facade need to look at
        queue.addHandler("CreateAccount", this::handleCreateAccountRequest);
    }

    public void handleCreateAccountRequest(Event event) {
        var p = event.getArgument(0, DTUPayAccount.class);

        // Todo: this should be implemented such that we send an "checkToken" event

        // Publish result
        Event accountRequest = new Event("AccountCreated", new Object[] {p});
        queue.publish(accountRequest);
    }
}
