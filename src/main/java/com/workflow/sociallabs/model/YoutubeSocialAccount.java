
package com.workflow.sociallabs.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue(TableNameDiscriminator.YoutubeSocialAccount)
public class YoutubeSocialAccount extends SocialAccount {

    @Column(name = "api_key")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
