package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.example.demo.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import jakarta.servlet.http.Cookie;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        HandshakeInterceptor cookieInterceptor = new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                    WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                if (request instanceof ServletServerHttpRequest) {
                    ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                    Cookie[] cookies = servletRequest.getServletRequest().getCookies();
                    if (cookies != null) {
                        for (Cookie cookie : cookies) {
                            if ("jwt_token".equals(cookie.getName())) {
                                attributes.put("jwt_token", cookie.getValue());
                            }
                        }
                    }
                }
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                    WebSocketHandler wsHandler, Exception exception) {
            }
        };

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(cookieInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    String token = sessionAttributes != null ? (String) sessionAttributes.get("jwt_token") : null;

                    if (token != null && jwtUtil.validateToken(token)) {
                        String userId = jwtUtil.getId(token);
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userId, null, null);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        accessor.setUser(authentication);
                    }
                }
                return message;
            }
        });
    }
}