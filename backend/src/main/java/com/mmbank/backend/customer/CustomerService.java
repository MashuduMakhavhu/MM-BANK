package com.mmbank.backend.customer;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {

        if (customerRepository.existsByCustomerNumber(
                request.customerNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Customer number already exists");
        }

        if (customerRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email already exists");
        }

        Customer customer = new Customer(
                request.customerNumber(),
                request.fullName(),
                request.email(),
                request.phoneNumber()
        );

        return toResponse(customerRepository.save(customer));
    }

    public CustomerResponse findCustomer(String customerNumber) {

        Customer customer = customerRepository
                .findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Customer not found"));

        return toResponse(customer);
    }

    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getCustomerNumber(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getCreatedAt()
        );
    }
}
