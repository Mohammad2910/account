package domain.storage;

import domain.model.DTUPayAccount;
import java.util.HashMap;
import java.util.Map;

public class InMemory {

    // key: value -> id: dtuBankAccount
    private Map<String, DTUPayAccount> dtuPayAccounts = new HashMap<>();

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
     * @return DTUPayAccount
     */
    public boolean addAccount(DTUPayAccount account) {
       this.dtuPayAccounts.put(account.getId(), account);

       return this.getAccount(account.getId()) != null;
    }

    /**
     * Remove account by id
     *
     * @param id
     * @return True if objects is deleted, false otherwise.
     */
    public boolean deleteAccount(String id) {
        // Deletion will retrieve the target item, if successful
        DTUPayAccount deletedAccount = this.dtuPayAccounts.remove(id);

        return id.equals(deletedAccount.getId());
    }
}


