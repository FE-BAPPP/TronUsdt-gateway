package com.UsdtWallet.UsdtWallet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    // Public endpoints that should skip JWT authentication
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/check-username",
        "/api/auth/check-email",
        "/api/auth/create-admin",
        "/api/admin/wallet/",
        "/api/test/",
        "/actuator/",
        "/health",
        "/favicon.ico",
        "/.well-known/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        log.debug("Processing request: {} {}", request.getMethod(), requestPath);

        // Skip JWT processing for public endpoints
        if (isPublicPath(requestPath)) {
            log.debug("Skipping JWT authentication for public path: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = getJwtFromRequest(request);
            log.debug("JWT token found: {}", jwt != null ? "Yes" : "No");

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                log.debug("JWT valid for username: {}", username);
                log.debug("Extracted username: {}", username);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                log.debug("UserDetails loaded for: {}", userDetails.getUsername());

                // Cast về UserPrincipal để đảm bảo @AuthenticationPrincipal hoạt động
                UserPrincipal userPrincipal = (UserPrincipal) userDetails;

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userPrincipal, null, userPrincipal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authentication set for user: {}", username);
            } else {
                log.debug("Invalid or missing JWT token for path: {}", requestPath);
            }

        } catch (Exception e) {
            log.error("Could not set user authentication in security context for path: {}", requestPath, e);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(publicPath -> {
            if (publicPath.endsWith("/")) {
                return path.startsWith(publicPath);
            } else {
                return path.equals(publicPath);
            }
        });
        log.debug("Path {} is public: {}", path, isPublic);
        return isPublic;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
