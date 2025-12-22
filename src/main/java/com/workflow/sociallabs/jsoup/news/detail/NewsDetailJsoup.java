package com.workflow.sociallabs.jsoup.news.detail;

public interface NewsDetailJsoup {

    void parse();

    static NewsDetailJsoup of(String url) {
        return switch (url) {
            case "lviv.media" -> new LvivMediaDetailJsoupImpl();
            case "expres.online" -> new ExpressDetailJsoupImpl();
            default -> null;
        };
    }
}
