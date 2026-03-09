package com.tiximax.txm.Service;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Repository.AuthenticationRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service

public class TokenService {
    @Autowired
    AuthenticationRepository authenticationRepository;

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey getSigninKey(){
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Account account) {
        String token =
                Jwts.builder().
                                subject(account.getUsername()).
                                issuedAt(new Date(System.currentTimeMillis()))
                        .expiration(new Date(System.currentTimeMillis()+24*60*60*1000))
                        .signWith(getSigninKey())
                        .compact();
        return token;
    }

    // form token to Claim Object
    public Claims extractAllClaims(String token) {
        return  Jwts.parser().
                verifyWith(getSigninKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // get userName form CLAIM
    public Account extractAccount (String token){
        String username = extractClaim(token,Claims::getSubject);
        return authenticationRepository.findByUsername(username);
    }


    public boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }
    // get Expiration form CLAIM
    public Date extractExpiration(String token){
        return extractClaim(token,Claims::getExpiration);
    }

    // from claim and extract specific data type.
    public <T> T extractClaim(String token, Function<Claims,T> resolver){
        Claims claims = extractAllClaims(token);
        return  resolver.apply(claims);
    }

}
