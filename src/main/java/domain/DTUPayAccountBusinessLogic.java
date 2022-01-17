package domain;

import domain.exception.DuplicateBankAccountException;
import domain.exception.NoSuchAccountException;
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

    /**
     * Get an account by id
     *
     * @param id
     * @return
     * @throws NoSuchAccountException
     */
    public DTUPayAccount get(String id) throws NoSuchAccountException {
       DTUPayAccount account = this.memory.getAccount(id);

       if (account == null) {
           throw new NoSuchAccountException("Account doesn't exists");
       }

       return account;
    }

    /**
     * Create a DTUPay account
     *
     * @param account
     * @throws DuplicateBankAccountException
     */
    public String createAccount(DTUPayAccount account) throws DuplicateBankAccountException {
        // check bank account already has been registered
        checkUniqueBankAccount(account);

        // Add account
        String id = UUID.randomUUID().toString();
        account.setId(id);
        memory.addAccount(account);

        return id;
    }

    /**
     * Delete a DTUPay account
     *
     * @param account
     * @throws NoSuchAccountException
     */
    public void delete(DTUPayAccount account) throws NoSuchAccountException {
        if (checkAccount(account)) {
            memory.deleteAccount(account.getId());
        }
    }

    /**
     * Check a DTUBank account is already registered to a DTUPay account
     *
     * @param account
     * @throws DuplicateBankAccountException
     */
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
