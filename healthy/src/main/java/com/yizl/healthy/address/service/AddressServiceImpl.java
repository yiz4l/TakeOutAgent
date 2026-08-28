package com.yizl.healthy.address.service;

import com.yizl.healthy.address.converter.AddressConverter;
import com.yizl.healthy.address.dto.request.AddressRequest;
import com.yizl.healthy.address.dto.response.AddressResponse;
import com.yizl.healthy.address.entity.UserAddressEntity;
import com.yizl.healthy.address.mapper.AddressMapper;
import com.yizl.healthy.common.exception.BusinessException;
import com.yizl.healthy.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AddressServiceImpl implements AddressService{

    private final AddressMapper addressMapper;

    public AddressServiceImpl(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    @Override
    public List<AddressResponse> getAddresses(Long userId) {
        return addressMapper.selectByUserId(userId).stream()
                .map(AddressConverter::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest addressRequest) {
        UserAddressEntity entity = AddressConverter.toEntity(userId, addressRequest);
        addressMapper.insert(entity);
        return AddressConverter.toResponse(entity);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest addressRequest) {
        UserAddressEntity entity = requireOwnedAddress(userId, addressId);
        AddressConverter.applyUpdate(entity, addressRequest);
        addressMapper.updateById(entity);
        return AddressConverter.toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        if (addressMapper.deleteOwnedById(userId, addressId) == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "地址不存在");
        }
    }

    private UserAddressEntity requireOwnedAddress(Long userId, Long addressId) {
        UserAddressEntity entity = addressMapper.selectOwnedById(userId, addressId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "地址不存在");
        }
        return entity;
    }
}
