package com.yizl.healthy.address.service;

import com.yizl.healthy.address.dto.request.AddressRequest;
import com.yizl.healthy.address.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {
    List<AddressResponse> getAddresses(Long userId);

    AddressResponse addAddress(Long userId, AddressRequest addressRequest);

    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest addressRequest);

    void deleteAddress(Long userId, Long addressId);
}
