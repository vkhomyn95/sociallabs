package com.workflow.sociallabs.repository;

import com.workflow.sociallabs.model.SocialAccount;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SocialAccountRepository extends PagingAndSortingRepository<SocialAccount, Long> {


}
