package gg.popn.http.common.config;

import gg.popn.http.common.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.Customizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import jakarta.servlet.DispatcherType;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity       // @PreAuthorize 사용 가능
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${popngg.swagger.username:admin}")
    private String swaggerUsername;

    @Value("${popngg.swagger.password:change-me}")
    private String swaggerPassword;

    @Value("${app.cors.allowed-origins:https://popn.gg,https://www.popn.gg}")
    private String allowedOrigins;

    @Value("${app.csrf.additional-origins:https://api.popn.gg}")
    private String additionalCsrfOrigins;

    @Value("${popngg.auth.cookie-secure:true}")
    private boolean cookieSecure;

    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(EndpointRequest.to("health", "prometheus")).permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/swagger-ui/**", "/v3/api-docs/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("SWAGGER"))
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder swaggerPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService swaggerUsers(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(User.withUsername(swaggerUsername)
                .password(passwordEncoder.encode(swaggerPassword))
                .roles("SWAGGER")
                .build());
    }

    @Bean
    @Order(3)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var csrfTokens = new CookieCsrfTokenRepository();
        // Secure browsers reject Domain/path variants of __Host- cookies from sibling subdomains.
        csrfTokens.setCookieName(cookieSecure ? "__Host-popngg-csrf" : "popngg-csrf");
        csrfTokens.setCookiePath("/");
        csrfTokens.setCookieCustomizer(cookie -> cookie.httpOnly(true).secure(cookieSecure).sameSite("Lax"));
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens)
                        .ignoringRequestMatchers(new ApiCsrfExemptions(allowedOrigins
                                + (additionalCsrfOrigins.isBlank() ? "" : "," + additionalCsrfOrigins))))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // 로그인/회원가입, Swagger 등은 허용
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/session",
                                "/api/v1/auth/csrf",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/register",
                                "/api/v1/auth/registrations/**",
                                "/api/v1/auth/password-reset/**",
                                "/api/v1/discord/interactions",
                                "/health"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/users/**",
                                "/api/v1/songs/**",
                                "/api/v1/charts/**",
                                "/api/v1/ratings/**",
                                "/api/v2/chart/**").permitAll()
                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"code\":\"UNAUTHENTICATED\",\"data\":null,\"message\":\"Authentication is required.\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Access is denied.\"}");
                        }))
                // JWT 필터 등록 (UsernamePasswordAuthenticationFilter 앞에)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
