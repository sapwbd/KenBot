package com.sapwbd.kenbot.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class OpenDotaUtility {

    // Endpoints
    public static final String COMMON_PREFIX_SEGMENT = "https://api.opendota.com/api/";
    public static final String RESOURCE_PLAYER = "players/%s";
    public static final String RESOURCE_MATCHES = "matches";
    public static final String CONSTANTS = "constants";
    public static final String HEROES = "heroes";

    private static final Map<String, String> heroIdNameMap = new HashMap<>();

    public static String getOpenDotaURL(String... pathFragments) {
        StringBuilder sb = new StringBuilder(COMMON_PREFIX_SEGMENT);
        for (String pathFragment : pathFragments) {
            sb.append(pathFragment);
            if (pathFragment.charAt(pathFragment.length() - 1) != '/') {
                sb.append("/");
            }
        }
        return sb.toString();
    }

    @PostConstruct
    public void fetchHeroConstants() {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        ObjectMapper mapper = new ObjectMapper();
        try {
            httpClient.execute(ClassicRequestBuilder.get(getOpenDotaURL(CONSTANTS, HEROES)).build(), response -> {
                if (response.getCode() == HttpStatus.SC_OK) {
                    JsonNode jsonNode = mapper.readTree(response.getEntity().getContent());
                    jsonNode.fields()
                            .forEachRemaining(heroIdHeroNodeEntry -> heroIdNameMap.put(heroIdHeroNodeEntry.getKey(), heroIdHeroNodeEntry.getValue()
                                                                                                                                        .get("localized_name")
                                                                                                                                        .asText()));
                    return true;
                }
                throw new HttpResponseException(response.getCode(), response.getReasonPhrase());
            });
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == HttpStatus.SC_TOO_MANY_REQUESTS) {
                // Log rate limiting
            }
        } catch (IOException e) {
            // Propagate request upward
            throw new RuntimeException(e);
        }
    }

    public static String getHeroName(String heroId) {
        return heroIdNameMap.get(heroId);
    }

}
