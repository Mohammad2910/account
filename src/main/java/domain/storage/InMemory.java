package domain.storage;

import domain.exception.DuplicateBankAccountException;
import domain.model.DTUPayAccount;
import java.util.HashMap;
import java.util.Map;

public class InMemory {

    // key: value -> id: dtuBankAccount
    private final Map<String, DTUPayAccount> dtuPayAccounts = new HashMap<>();

    private static InMemory instance;

    private InMemory() {}

    public static InMemory instance() {
        if(instance == null) {
            instance = new InMemory();
            return instance;
        }
        return instance;
    }

    /**
     * Get an account by id
     *
     * @param id
     * @return dtuPayAccount object
     */
    public DTUPayAccount getAccount(String id) {
        return this.dtuPayAccounts.get(id);
    }

    /**
     * Get all accounts in the storage
     *
     * @return Map of id: dtuPayAccount
     */
    public Map<String, DTUPayAccount> getAccounts() {
        return this.dtuPayAccounts;
    }

    /**
     * Add account to the in memory storage
     *
     * @param account
     */
    public void addAccount(DTUPayAccount account) {
       this.dtuPayAccounts.put(account.getId(), account);
    }

    /**
     * Remove account by id
     *
     * @param id
     */
    public void deleteAccount(String id) {
        this.dtuPayAccounts.remove(id);
    }

    /**
     * Cleans up the registration list
     */
    public void cleanAccounts() {
        this.dtuPayAccounts.clear();
    }
}


