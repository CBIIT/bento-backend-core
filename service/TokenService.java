package gov.nih.nci.bento.service;

import gov.nih.nci.bento.model.ConfigurationDAO;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@RequiredArgsConstructor
@Service("TokenService")
public class TokenService {
    private static final Logger logger = LogManager.getLogger(TokenService.class);
    private final ConfigurationDAO config;

    public boolean verifyToken(String token) {
        try {
            String secret = config.getTokenSecret();
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(secret.getBytes("UTF-8"))
                    .build()
                    .parseClaimsJws(token);
            return !claims.getBody().isEmpty();
        } catch (UnsupportedEncodingException | IllegalArgumentException | JwtException e) {
            logger.error(e);
            logger.warn("Token is not valid!");
        }
        return false;
    }
}