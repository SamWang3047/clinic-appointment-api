package com.sam.clinic.security;

import com.sam.clinic.account.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			ProblemAuthenticationEntryPoint authenticationEntryPoint,
			ProblemAccessDeniedHandler accessDeniedHandler,
			JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.POST,
								"/api/v1/auth/login",
								"/api/v1/patients/register").permitAll()
						.requestMatchers(HttpMethod.GET,
								"/api/v1/doctors",
								"/api/v1/doctors/*",
								"/api/v1/doctors/*/available-slots",
								"/actuator/health").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.oauth2ResourceServer(oauth2 -> oauth2
						.authenticationEntryPoint(authenticationEntryPoint)
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecretKey jwtSecretKey(JwtProperties properties) {
		String secret = properties.secret();
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
		}
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey secretKey) {
		return NimbusJwtEncoder.withSecretKey(secretKey)
				.algorithm(MacAlgorithm.HS256)
				.build();
	}

	@Bean
	JwtDecoder jwtDecoder(
			SecretKey secretKey,
			JwtProperties properties,
			UserAccountRepository accountRepository) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(List.of(
				JwtValidators.createDefaultWithIssuer(properties.issuer()),
				new ActiveAccountJwtValidator(accountRepository))));
		return decoder;
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName("role");
		authoritiesConverter.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
		authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return authenticationConverter;
	}
}
