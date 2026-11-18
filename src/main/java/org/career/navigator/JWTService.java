package org.career.navigator;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;

import java.util.Date;

public class JWTService {

    private final Algorithm algorithm;

    public JWTService() {
        algorithm = Algorithm.HMAC256("career-navigator-secret");
    }

    public String createToken(String email) {
        Date expiration = new Date(
                System.currentTimeMillis() + 60 * 60 * 1000
        );

        return JWT.create()
                .withSubject(email)
                .withExpiresAt(expiration)
                .sign(algorithm);
    }

    public String getEmail(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm).build();

            return verifier
                    .verify(token)
                    .getSubject();

        } catch (JWTVerificationException exception) {
            return null;
        }
    }
}

