package br.com.starter.application.api.user.dto;

import java.util.UUID;

import br.com.starter.domain.user.User;

public record AuthenticatedUserResponse(UUID id, String username, String name, String role) {
    public static AuthenticatedUserResponse fromUser(User user) {
        String name = user.getProfile() != null ? user.getProfile().getName() : null;
        String role = user.getRole() != null ? user.getRole().getName() : null;

        return new AuthenticatedUserResponse(
                user.getId(),
                user.getAuth().getUsername(),
                name,
                role
        );
    }
}
