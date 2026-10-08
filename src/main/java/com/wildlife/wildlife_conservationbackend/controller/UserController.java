package com.wildlife.wildlife_conservationbackend.controller;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.UserSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.service.UserService;
import com.wildlife.wildlife_conservationbackend.utility.EndPoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = EndPoint.BASE, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARK_MANAGER')")
public class UserController {
    private final UserService userService;

    @GetMapping(EndPoint.USERS)
    public ResponseEntity<StandardResponse<PageResponse<UserSummaryResponseDTO>>> list(
            @AuthenticationPrincipal CurrentUser actor, @RequestParam(required = false) @Size(max = 100) String parkId,
            @RequestParam(required = false) Role role, @Valid @ModelAttribute PageQuery page) {
        return userService.listUsers(actor, parkId, role, page);
    }
}
