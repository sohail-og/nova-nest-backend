package com.novanest.dto;

public class CodPaymentRequest {
    private AddressDto address;

    public CodPaymentRequest() {}

    public CodPaymentRequest(AddressDto address) {
        this.address = address;
    }

    public AddressDto getAddress() {
        return address;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }
}
