package com.urva.myfinance.coinTrack.Config;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.urva.myfinance.coinTrack.Service.AuthService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {
    @Autowired
    private AuthService authService;

    @Autowired
    ApplicationContext applicationContext;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        System.out.println("[JwtFilter] Incoming request: " + request.getRequestURI());
        System.out.println("[JwtFilter] Authorization header: " + authHeader);

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                System.out.println("[JwtFilter] Extracted token: " + token);
                try {
                    username = authService.extractUsername(token);
                    System.out.println("[JwtFilter] Extracted username: " + username);
                } catch (Exception e) {
                    System.out.println("[JwtFilter] Failed to extract username: " + e.getMessage());
                }
            } else {
                System.out.println("[JwtFilter] No Bearer token found in Authorization header.");
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = applicationContext.getBean(UserDetailsService.class)
                            .loadUserByUsername(username);
                    System.out.println("[JwtFilter] Loaded user details for: " + username);
                    if (authService.validateToken(token, userDetails)) {
                        System.out.println("[JwtFilter] Token validated for user: " + username);
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        System.out.println("[JwtFilter] Token validation failed for user: " + username);
                    }
                } catch (BeansException | UsernameNotFoundException e) {
                    System.out.println("[JwtFilter] UserDetailsService error: " + e.getMessage());
                    SecurityContextHolder.clearContext();
                }
            } else if (username == null) {
                System.out.println("[JwtFilter] Username is null after token extraction.");
            } else {
                System.out.println("[JwtFilter] Authentication already present in SecurityContextHolder.");
            }
        } catch (Exception e) {
            System.out.println("[JwtFilter] Exception: " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return "/login".equals(path) || "/api/register".equals(path);
    }

}
