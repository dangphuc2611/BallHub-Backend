package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.response.user.AddressResponse;
import com.ballhub.ballhub_backend.dto.request.user.CreateAddressRequest;
import com.ballhub.ballhub_backend.dto.request.user.UpdateAddressRequest;
import com.ballhub.ballhub_backend.entity.User;
import com.ballhub.ballhub_backend.entity.UserAddress;
import com.ballhub.ballhub_backend.exception.BadRequestException;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.repository.UserAddressRepository;
import com.ballhub.ballhub_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AddressService {

    @Autowired
    private UserAddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses(Integer userId) {
        List<UserAddress> addresses = addressRepository.findByUserUserId(userId);
        return addresses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AddressResponse createAddress(Integer userId, CreateAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        // If this is set as default, unset other default addresses
        if (request.getIsDefault() != null && request.getIsDefault()) {
            unsetDefaultAddresses(userId);
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .fullAddress(request.getFullAddress())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();

        UserAddress saved = addressRepository.save(address);
        return mapToResponse(saved);
    }

    public AddressResponse updateAddress(Integer userId, Integer addressId, UpdateAddressRequest request) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại"));

        // Verify ownership
        if (!address.getUser().getUserId().equals(userId)) {
            throw new BadRequestException("Địa chỉ không thuộc về bạn");
        }

        // If setting as default, unset other defaults
        if (request.getIsDefault() != null && request.getIsDefault()) {
            unsetDefaultAddresses(userId);
        }

        address.setFullAddress(request.getFullAddress());
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }

        UserAddress updated = addressRepository.save(address);
        return mapToResponse(updated);
    }

    public void deleteAddress(Integer userId, Integer addressId) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại"));

        // Verify ownership
        if (!address.getUser().getUserId().equals(userId)) {
            throw new BadRequestException("Địa chỉ không thuộc về bạn");
        }

        addressRepository.delete(address);
    }

    public AddressResponse setDefaultAddress(Integer userId, Integer addressId) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại"));

        // Verify ownership
        if (!address.getUser().getUserId().equals(userId)) {
            throw new BadRequestException("Địa chỉ không thuộc về bạn");
        }

        // Unset all other defaults
        unsetDefaultAddresses(userId);

        // Set this as default
        address.setIsDefault(true);
        UserAddress updated = addressRepository.save(address);

        return mapToResponse(updated);
    }

    private void unsetDefaultAddresses(Integer userId) {
        List<UserAddress> defaultAddresses = addressRepository.findByUserUserIdAndIsDefaultTrue(userId);
        for (UserAddress addr : defaultAddresses) {
            addr.setIsDefault(false);
            addressRepository.save(addr);
        }
    }

    private AddressResponse mapToResponse(UserAddress address) {
        return AddressResponse.builder()
                .addressId(address.getAddressId())
                .fullAddress(address.getFullAddress())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .build();
    }
}
