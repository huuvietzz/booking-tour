package com.example.tour.user.service;

import com.example.tour.user.dto.request.CustomerCreateRequest;
import com.example.tour.user.dto.request.CustomerFilterRequest;
import com.example.tour.user.dto.request.CustomerUpdateRequest;
import com.example.tour.user.dto.response.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    Page<CustomerResponse> getCustomers(CustomerFilterRequest filter, Pageable pageable);

    CustomerResponse getCustomer(Integer userId);

    CustomerResponse createCustomer(CustomerCreateRequest request);

    CustomerResponse updateCustomer(Integer userId, CustomerUpdateRequest request);

    CustomerResponse setActive(Integer userId, boolean active);

    void deleteCustomer(Integer userId);
}
