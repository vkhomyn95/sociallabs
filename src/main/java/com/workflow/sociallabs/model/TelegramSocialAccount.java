package com.workflow.sociallabs.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue(TableNameDiscriminator.TelegramSocialAccountDiscriminator)
public class TelegramSocialAccount extends SocialAccount {

    @Column(name = "api_id")
    private String apiId;

    @Column(name = "api_hash")
    private String apiHash;

    @Column(name = "phone_number")
    private String phoneNumber;

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getApiHash() {
        return apiHash;
    }

    public void setApiHash(String apiHash) {
        this.apiHash = apiHash;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
