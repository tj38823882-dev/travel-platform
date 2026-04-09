package com.example.demo.config;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;

import com.example.demo.responseDto.LoggedInMemberDto;
import com.example.demo.service.AuthService;
import com.example.demo.utils.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtutil;
    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        String id = null;

        try {
            id = jwtutil.getId(token);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            System.out.println("JWT Token 已過期");
        } catch (Exception e) {
            System.out.println("JWT Token 無效");
        }

        if (id != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                LoggedInMemberDto loggedInMemberDto = authService.createLoggedInUserDto(id);
                
                if (loggedInMemberDto != null && !Boolean.TRUE.equals(loggedInMemberDto.getIsActive())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "您的帳號已被封鎖，請聯繫管理員");
                    return;
                }

                if (loggedInMemberDto != null) {
                    String roleName = loggedInMemberDto.getRole().getRoleName();

                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            loggedInMemberDto,
                            null,
                            List.of(new SimpleGrantedAuthority(roleName)));

                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (Exception e) {
                // 若找不到使用者 (例如 DB 重置導致 Token 內的 ID 失效)，視為未登入，不阻擋請求
                System.out.println("JWT User lookup failed: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}