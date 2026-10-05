package com.mate.meeting_room_reservation.security;

import com.mate.meeting_room_reservation.entity.AppUser;
import com.mate.meeting_room_reservation.entity.UserRole;
import com.mate.meeting_room_reservation.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public String getUsername() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    public AppUser getCurrentUser() {
        return appUserRepository.findByUsername(getUsername())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found."));
    }

    public boolean isAdmin(AppUser user) {
        return user.getRole() == UserRole.ADMIN;
    }
}
