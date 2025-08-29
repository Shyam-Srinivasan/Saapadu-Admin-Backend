package com.saapadu.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SignUpRequest {
    @JsonProperty("college_name")
    public String collegeName;
    @JsonProperty("email_id")
    public String emailId;
    @JsonProperty("domain_address")
    public String domainAddress;
    @JsonProperty("address")
    public String address;
    @JsonProperty("contact_no")
    public String contactNo;
}
