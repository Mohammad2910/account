package domain;

import domain.model.DTUPayAccount;
import domain.storage.InMemory;
import java.util.Map;

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

    public boolean create() {
        return false;
    }

    public boolean delete() {
        return false;
    }
}
