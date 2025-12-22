package com.workflow.sociallabs.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue(TableNameDiscriminator.TikTokSocialAccount)
public class TikTokSocialAccount extends SocialAccount {

    @Column(name = "api_key")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
