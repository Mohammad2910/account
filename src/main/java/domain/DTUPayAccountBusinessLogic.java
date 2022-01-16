package domain;

import domain.model.DTUPayAccount;
import domain.storage.InMemory;
import java.util.Map;
import java.util.UUID;

public class DTUPayAccountBusinessLogic {
    private InMemory memory;

    /**
     * Constructor of DTUPayAccountBusinessLogic class
     */
    public DTUPayAccountBusinessLogic(InMemory memory) {
        this.memory = memory;
    }

    public DTUPayAccount get(String id){
       return this.memory.getAccount(id);
    }

    public Map<String, DTUPayAccount> getList() {
        return null;
    }

    public void createAccount(DTUPayAccount account) throws DuplicateBankAccountException {
        // check bank account already has been registered
        checkUniqueBankAccount(account);

        // Add account
        account.setId(UUID.randomUUID().toString());
        memory.addAccount(account);
    }

    public void checkUniqueBankAccount(DTUPayAccount account) throws DuplicateBankAccountException {
        // Get accounts
        Map<String, DTUPayAccount> dtuPayAccounts = memory.getAccounts();

        // Check account exists
        for (Map.Entry<String, DTUPayAccount> a : dtuPayAccounts.entrySet()) {
            if (a.getValue().getDtuBankAccount().equals(account.getDtuBankAccount())) {
                throw new DuplicateBankAccountException("An account with given bank account number already exists");
            }
        }
    }
}
