package com.example.user_service.service.GeneralServices;


import com.example.user_service.Config.JwtConfig;
import com.example.user_service.model.Enum.Roles;
import com.example.user_service.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class JwtService {

    private final JwtConfig jwtConfig;


    public String generateAccessToken(User user, List<Roles> roles) {
        Map<String, Object> claims = new HashMap<>();
        List<String> roleNames = roles.stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        claims.put("roles", roleNames);
        claims.put("id", user.getId().toString());
        return generateJwt(user, jwtConfig.getAccessTokenExpiration(), claims);
    }

    public String generateRefreshToken(User user) {
        return generateJwt(user, jwtConfig.getRefreshTokenExpiration(), null);
    }

    private String generateJwt(User user, Integer expirationTime, Map<String, Object> claims) {
        System.out.println(user.getId().toString());
        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(jwtConfig.getSecretKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            var claims = getClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException ex) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtConfig.getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String extractId(String token) {
        return getClaims(token).get("id").toString();
    }

    public List<Roles> extractRoles(String token) {
        Claims claims = getClaims(token);
        List<String> roleNames = claims.get("roles", List.class);
        return roleNames.stream()
                .map(Roles::valueOf)
                .collect(Collectors.toList());
    }


}
