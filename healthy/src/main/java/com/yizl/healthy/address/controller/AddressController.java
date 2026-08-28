package com.yizl.healthy.address.controller;

import com.yizl.healthy.address.dto.request.AddressRequest;
import com.yizl.healthy.address.dto.response.AddressResponse;
import com.yizl.healthy.address.service.AddressService;
import com.yizl.healthy.common.api.ApiResponse;
import com.yizl.healthy.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/addresses")
@PreAuthorize("hasRole('USER')")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> getAddresses(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(addressService.getAddresses(currentUser.userId()));
    }

    @PostMapping
    public ApiResponse<AddressResponse> addAddress(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Validated(AddressRequest.Create.class) @RequestBody AddressRequest request
    ) {
        return ApiResponse.success(addressService.addAddress(currentUser.userId(), request));
    }

    @PutMapping("/{addressId}")
    public ApiResponse<AddressResponse> updateAddress(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Positive @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request
    ) {
        return ApiResponse.success(
                addressService.updateAddress(currentUser.userId(), addressId, request)
        );
    }

    @DeleteMapping("/{addressId}")
    public ApiResponse<Void> deleteAddress(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Positive @PathVariable Long addressId
    ) {
        addressService.deleteAddress(currentUser.userId(), addressId);
        return ApiResponse.success(null);
    }
}
