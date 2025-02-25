package com.github.catvod.spider;

import android.content.Context;

import com.github.catvod.bean.live.Channel;
import com.github.catvod.bean.live.Group;
import com.github.catvod.bean.xtream.Config;
import com.github.catvod.bean.xtream.XCategory;
import com.github.catvod.bean.xtream.XStream;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.HttpUrl;

public class XtreamCode extends Spider {

    private List<Group> groups;
    private Config config;

    @Override
    public void init(Context context, String extend) throws Exception {
        config = Config.objectFrom(extend);
        groups = new ArrayList<>();
    }

    @Override
    public String liveContent(String url) {
        config.setUrl(url);
        List<XCategory> categoryList = getCategoryList(config);
        List<XStream> streamList = getStreamList(config);
        Map<String, String> categoryMap = new HashMap<>();
        for (XCategory category : categoryList) {
            categoryMap.put(category.getCategoryId(), category.getCategoryName());
        }
        for (XStream stream : streamList) {
            if (!categoryMap.containsKey(stream.getCategoryId())) continue;
            Group group = Group.find(groups, Group.create(categoryMap.get(stream.getCategoryId())));
            Channel channel = group.find(Channel.create(stream.getName()));
            if (!stream.getStreamIcon().isEmpty()) channel.setLogo(stream.getStreamIcon());
            if (!stream.getEpgChannelId().isEmpty()) channel.setTvgName(stream.getEpgChannelId());
            channel.getUrls().addAll(stream.getPlayUrl(config));
        }
        int number = 0;
        for (Group group : groups) {
            for (Channel channel : group.getChannel()) {
                if (channel.getNumber().isEmpty()) channel.setNumber(++number);
            }
        }
        return new Gson().toJson(groups);
    }

    public static HttpUrl.Builder getBuilder(Config config) {
        HttpUrl url = HttpUrl.parse(config.getUrl());
        return new HttpUrl.Builder().scheme(url.scheme()).host(url.host()).port(url.port());
    }

    private String getApiUrl(Config config, String action) {
        return getBuilder(config).addPathSegment("player_api.php").addQueryParameter("username", config.getName()).addQueryParameter("password", config.getPass()).addQueryParameter("action", action).build().toString();
    }

    private List<XCategory> getLiveCategoryList(Config config) {
        return XCategory.arrayFrom(OkHttp.string(getApiUrl(config, "get_live_categories")));
    }

    private List<XStream> getLiveStreamList(Config config) {
        return XStream.arrayFrom(OkHttp.string(getApiUrl(config, "get_live_streams")));
    }

    private List<XCategory> getVodCategoryList(Config config) {
        return XCategory.arrayFrom(OkHttp.string(getApiUrl(config, "get_vod_categories")));
    }

    private List<XStream> getVodStreamList(Config config) {
        return XStream.arrayFrom(OkHttp.string(getApiUrl(config, "get_vod_streams")));
    }

    private List<XCategory> getCategoryList(Config config) {
        List<XCategory> categoryList = getLiveCategoryList(config);
        if (config.isVod()) categoryList.addAll(getVodCategoryList(config));
        return categoryList;
    }

    private List<XStream> getStreamList(Config config) {
        List<XStream> streamList = getLiveStreamList(config);
        if (config.isVod()) streamList.addAll(getVodStreamList(config));
        return streamList;
    }
}
