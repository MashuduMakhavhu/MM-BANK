package com.mmbank.backend.staff;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/staff-auth")
public class StaffAuthController {

    private final StaffAuthService authService;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public StaffAuthController(StaffAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public StaffAuthResponse login(
            @Valid @RequestBody StaffLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        StaffAccount staff = authService.authenticate(request);

        HttpSession oldSession = httpRequest.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }

        SecurityContextHolder.clearContext();
        httpRequest.getSession(true).setMaxInactiveInterval(900);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        staff.getStaffNumber(),
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + staff.getRole().name()
                        ))
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(
                context,
                httpRequest,
                httpResponse
        );

        return new StaffAuthResponse(
                staff.getStaffNumber(),
                staff.getFullName(),
                staff.getRole().name(),
                "Staff login successful"
        );
    }

    @GetMapping("/me")
    public StaffAuthResponse me() {
        var authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().startsWith("ROLE_"))) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in"
            );
        }

        StaffAccount staff = authService.findByStaffNumber(
                authentication.getName()
        );

        return new StaffAuthResponse(
                staff.getStaffNumber(),
                staff.getFullName(),
                staff.getRole().name(),
                "Authenticated"
        );
    }

    @PostMapping("/logout")
    public StaffAuthResponse logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return new StaffAuthResponse(
                null, null, null, "Logged out"
        );
    }
}
