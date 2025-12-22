package com.workflow.sociallabs.jsoup.news.list;

import com.workflow.sociallabs.jsoup.news.detail.NewsDetailJsoup;
import com.workflow.sociallabs.utility.UrlExtractor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("LvivNewsJsoupImpl")
@Slf4j
public class LvivNewsJsoupImpl implements NewsJsoup {

    @Value("${scheduling.url.news.lviv}")
    private String url;
    @Value("${scheduling.user.agent}")
    private String userAgent;

    private static final String NEWS_BLOCK_SELECTOR = "#main > div:nth-child(2) > div > article > noindex > section";
    private static final String NEWS_BLOCK_SELECTOR_LINK = "div > div > a";

    @SneakyThrows
    @Override
    public void parse() {
        Document document = Jsoup.connect(url).userAgent(userAgent).timeout(15000).get();

        Elements news = document.select(NEWS_BLOCK_SELECTOR);

        for (Element article : news) {
            Element linkElement = article.selectFirst(NEWS_BLOCK_SELECTOR_LINK);

            if (linkElement == null) continue;

            String title = linkElement.text();

            String url = linkElement.absUrl("href");

            if (url == null || !url.matches("^https?://.+")) continue;

            String location = UrlExtractor.extractUrl(url);

            if (location == null) continue;

            NewsDetailJsoup detailJsoup = NewsDetailJsoup.of(location);

            if (detailJsoup == null) continue;

            detailJsoup.parse();

            System.out.println(linkElement);
        }

    }
}
