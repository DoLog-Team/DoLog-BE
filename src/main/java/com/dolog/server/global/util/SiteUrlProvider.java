package com.dolog.server.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SiteUrlProvider {

    private final String siteUrl;

    public SiteUrlProvider(@Value("${dolog.site-url}") String siteUrl) {
        this.siteUrl = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
    }

    public String exhibitionUrl(String slug) {
        return siteUrl + "/" + slug;
    }
}
