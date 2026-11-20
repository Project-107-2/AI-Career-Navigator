package org.career.navigator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void correctPasswordIsValid() {
        String hash =
                Main.hashPassword("12345678");

        boolean result =
                Main.checkPassword(
                        "12345678",
                        hash
                );

        assertTrue(result);
    }

    @Test
    void wrongPasswordIsInvalid() {
        String hash =
                Main.hashPassword("12345678");

        boolean result =
                Main.checkPassword(
                        "wrongpassword",
                        hash
                );

        assertFalse(result);
    }

    @Test
    void samePasswordsHaveDifferentHashes() {
        String first =
                Main.hashPassword("12345678");

        String second =
                Main.hashPassword("12345678");

        assertNotEquals(first, second);
    }
}
