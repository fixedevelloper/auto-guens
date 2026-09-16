package com.ussdauto.api.security;

import com.ussdauto.api.repository.DeviceRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authentification par API key :
 * - header "X-Api-Key" : clé marchand (config), pour tout ce qui n'est pas un callback device.
 * - header "X-Device-Api-Key" : clé d'un device enregistré, pour le callback de statut et la
 *   lecture de sa propre config SIM.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String MERCHANT_API_KEY_HEADER = "X-Api-Key";
    public static final String DEVICE_API_KEY_HEADER = "X-Device-Api-Key";

    private final DeviceRepository deviceRepository;
    private final String merchantApiKey;

    public ApiKeyAuthFilter(DeviceRepository deviceRepository, String merchantApiKey) {
        this.deviceRepository = deviceRepository;
        this.merchantApiKey = merchantApiKey == null ? "" : merchantApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String merchantKey = request.getHeader(MERCHANT_API_KEY_HEADER);
        String deviceKey = request.getHeader(DEVICE_API_KEY_HEADER);

        if (deviceKey != null) {
            deviceRepository.findByApiKey(deviceKey).ifPresent(device ->
                    authenticate(device.getApiKey(), "ROLE_DEVICE"));
        } else if (merchantKey != null && !merchantApiKey.isBlank() && merchantKey.equals(merchantApiKey)) {
            authenticate(merchantKey, "ROLE_MERCHANT");
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String principal, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
