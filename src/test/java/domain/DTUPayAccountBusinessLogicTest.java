package domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import domain.storage.InMemory;
import domain.model.DTUPayAccount;
import domain.exception.NoSuchAccountException;
import domain.exception.DuplicateBankAccountException;

class DTUPayAccountBusinessLogicTest {
    InMemory memory = InMemory.instance();
    DTUPayAccountBusinessLogic businessLogic = new DTUPayAccountBusinessLogic(memory);

    // Get - Fail
    @Test
    public void testGetAccount_ThrowsNoSuchAccountException () {
        assertThrows(NoSuchAccountException.class, () -> {
            businessLogic.get("unknown");
        });
    }

    // Get - OK
    @Test
    public void testGetAccount_OK () {
        boolean result = false;
        try {
            // Create account
            DTUPayAccount account = new DTUPayAccount();
            account.setName("John Doe");
            account.setCpr("1234");
            account.setDtuBankAccount("bank identifier");
            businessLogic.createAccount(account);

            // Get account by id
            businessLogic.get(account.getId());
            result = true;
        } catch (NoSuchAccountException|DuplicateBankAccountException e) {
            System.out.println(e.getMessage());
        }

        assertTrue(result);
    }

    // Create - Fail
    @Test
    public void testCreateAccount_ThrowsDuplicateBankAccountException () {
        assertThrows(DuplicateBankAccountException.class, () -> {
            // Create account
            DTUPayAccount account = new DTUPayAccount();
            account.setName("John Doe");
            account.setCpr("1234");
            account.setDtuBankAccount("bank identifier");
            businessLogic.createAccount(account);

            // Create account with same bank identifier
            businessLogic.createAccount(account);
        });
    }

    // Create - OK
    @Test
    public void testCreateAccount_OK () {
        boolean result = false;
        try {
            // Create account
            DTUPayAccount account1 = new DTUPayAccount();
            account1.setName("John Doe");
            account1.setCpr("1234");
            account1.setDtuBankAccount("bank identifier");
            businessLogic.createAccount(account1);

            // Create account with different bank identifier
            DTUPayAccount account2 = new DTUPayAccount();
            account2.setName("Xina");
            account2.setCpr("5678");
            account2.setDtuBankAccount("bank identifier 2");
            businessLogic.createAccount(account2);
            result = true;
        } catch (DuplicateBankAccountException e) {
            System.out.println(e.getMessage());
        }

        assertTrue(result);
    }

    // Delete - Fail
    @Test
    public void testDeleteAccount_ThrowsNoSuchAccountException () {
        assertThrows(NoSuchAccountException.class, () -> {
            DTUPayAccount account = new DTUPayAccount("unknown", "name", "cpr", "bank identifier");
            businessLogic.delete(account);
        });
    }

    // Delete - OK
    @Test
    public void testDeleteAccount_OK () {
        boolean result = false;
        try {
            // Create account
            DTUPayAccount account = new DTUPayAccount();
            account.setName("John Doe");
            account.setCpr("1234");
            account.setDtuBankAccount("bank identifier");
            String id = businessLogic.createAccount(account);

            // Delete account
            account.setId(id);
            businessLogic.delete(account);
            result = true;
        } catch (NoSuchAccountException|DuplicateBankAccountException e) {
            System.out.println(e.getMessage());
        }

        assertTrue(result);
    }

    /**
     * Cleans up memory for independent test functions
     */
    @AfterEach
    private void tearDown() {
        memory.cleanAccounts();
    }
}



