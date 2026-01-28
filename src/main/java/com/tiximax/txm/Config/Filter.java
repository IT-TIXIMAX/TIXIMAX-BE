package com.tiximax.txm.Config;

import com.tiximax.txm.Entity.Account;
import com.tiximax.txm.Exception.AuthException;
import com.tiximax.txm.Service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Component
public class Filter extends OncePerRequestFilter {
    @Autowired
    TokenService tokenService;

    @Autowired
    SessionRegistry sessionRegistry;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver resolver;

    private final List<String> AUTH_PERMISSION = List.of(
            "/accounts/login",
            "/otp/send",
            "/accounts/verify-account",
            "/accounts/forgot-password/send-otp",
            "/accounts/forgot-password/reset",
            "/accounts/verify",
            "/accounts/register/staff",
            "/accounts/register/customer",
            "/accounts/update-all-passwords",
            "/accounts/login-google",
            "/accounts/callback",
            "/images/upload-image",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/websocket/**",
            "/ws/info",
            "/orders/shipments-by-phone/{phone}",
            "/payments/receive",
            "/api/payments/receive",
            "/api/accounts/login",
        "/api/accounts/login",
        "/api/otp/send",
        "/api/accounts/verify-account",
        "/api/accounts/forgot-password/send-otp",
        "/api/accounts/forgot-password/reset",
        "/api/accounts/verify",
        "/api/accounts/register/staff",
        "/api/accounts/register/customer",
        "/api/accounts/update-all-passwords",
        "/api/accounts/login-google",
        "/api/accounts/callback",
        "/api/images/upload-image",
        "/api/swagger-ui.html",
        "/api/swagger-ui/**",
        "/api/v3/api-docs/**",
        "/api/swagger-resources/**",
        "/api/websocket/**",
        "/api/ws/info",
        "/api/orders/shipments-by-phone/{phone}"
    );

    private boolean isPermitted(String uri) {
        AntPathMatcher pathMatcher = new AntPathMatcher();
        return AUTH_PERMISSION.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri) || uri.startsWith("/ws"));
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    String uri = request.getRequestURI();
    String token = getToken(request);

    if (isPermitted(uri)) {
        filterChain.doFilter(request, response);
        return;
    }

    
    if (token == null) {
        resolver.resolveException(request, response, null, new AuthException("Empty token!"));
        return;
    }

    try {
        Account account = tokenService.extractAccount(token);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(account, null, account.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (ExpiredJwtException e) {
        resolver.resolveException(request, response, null, new AuthException("Expired Token!"));
        return;
    } catch (MalformedJwtException e) {
        resolver.resolveException(request, response, null, new AuthException("Invalid Token!"));
        return;
    }

    filterChain.doFilter(request, response);
}

    public String getToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring(7);
    }

}
