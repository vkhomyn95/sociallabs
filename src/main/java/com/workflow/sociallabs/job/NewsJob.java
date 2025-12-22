package com.workflow.sociallabs.job;

import com.workflow.sociallabs.jsoup.news.list.NewsJsoup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsJob {

    @Qualifier("LvivNewsJsoupImpl")
    private final NewsJsoup lvivNewsJsoup;

    @Scheduled(cron = "${scheduling.cron.news.ukraine}")
    public void findPravdaArticles() {

    }

    @Scheduled(cron = "${scheduling.cron.news.lviv}")
    public void findLvivArticles() {
//        lvivNewsJsoup.parse();
    }
}
