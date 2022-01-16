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

    public DTUPayAccount get(String id) throws NoSuchAccountException {
       DTUPayAccount account = this.memory.getAccount(id);

       if (account == null) {
           throw new NoSuchAccountException("No DTU Pay account exists for the given id.");
       }

       return account;
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

    public void delete(DTUPayAccount account) throws NoSuchAccountException {
        if (checkAccount(account)) {
            memory.deleteAccount(account.getId());
        }
    }

    public boolean checkAccount(DTUPayAccount account) throws NoSuchAccountException {
        if (memory.getAccount(account.getId()) != null) {
            return true;
        } else {
            throw new NoSuchAccountException("Account doesn't exists");
        }
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
