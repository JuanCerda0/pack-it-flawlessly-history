package dev.noby.history.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoGroupAuthoritiesConverterTests {

    private final CognitoGroupAuthoritiesConverter converter = new CognitoGroupAuthoritiesConverter();

    @Test
    void mapsOnlySupportedCognitoGroups() {
        Jwt token = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("cognito:groups", List.of("ADMIN", "SUPERVISOR", "OTHER"))
                .build();

        assertThat(converter.convert(token)).containsExactlyInAnyOrder(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_SUPERVISOR"));
    }
}
