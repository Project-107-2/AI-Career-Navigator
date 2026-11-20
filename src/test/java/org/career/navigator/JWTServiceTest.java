package org.career.navigator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JWTServiceTest {

    @Test
    void validTokenReturnsEmail() {
        JWTService service =
                new JWTService();

        String token =
                service.createToken(
                        "agas@mail.ru"
                );

        String email =
                service.getEmail(token);

        assertEquals(
                "agas@mail.ru",
                email
        );
    }

    @Test
    void invalidTokenReturnsNull() {
        JWTService service =
                new JWTService();

        String email =
                service.getEmail(
                        "wrong-token"
                );

        assertNull(email);
    }
}