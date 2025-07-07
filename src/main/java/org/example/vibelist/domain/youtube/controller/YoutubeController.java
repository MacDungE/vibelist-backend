package org.example.vibelist.domain.youtube.controller;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.xml.datatype.DatatypeConfigurationException;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.Date;

@RestController
public class YoutubeController {

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/youtube")
    public String getYoutubeTop10() {
        try {
            String apiKey = "AIzaSyBNnRaosfB6xt3wZsjhmpLfz3XM3mhCQqU"; // 🔐 본인의 API 키로 교체
            String artist = "iu";
            String title = "좋은날";

            String keyword = artist + " " + title;

            // 1. 10개 검색
            String searchUrl = "https://www.googleapis.com/youtube/v3/search"
                    + "?part=snippet"
                    + "&q=" + URLEncoder.encode(keyword, "UTF-8")
                    + "&type=video"
                    + "&maxResults=10"
                    + "&key=" + apiKey;

            ResponseEntity<String> searchResponse = restTemplate.getForEntity(searchUrl, String.class);
            JSONObject searchJson = new JSONObject(searchResponse.getBody());

            JSONArray items = searchJson.getJSONArray("items");

            StringBuilder result = new StringBuilder();

            for (int i = 0; i < items.length(); i++) {
                String videoId = items.getJSONObject(i).getJSONObject("id").getString("videoId");

                // 2. 개별 영상 길이 조회
                String videoUrl = "https://www.googleapis.com/youtube/v3/videos"
                        + "?part=contentDetails"
                        + "&id=" + videoId
                        + "&key=" + apiKey;

                ResponseEntity<String> videoResponse = restTemplate.getForEntity(videoUrl, String.class);
                JSONObject videoJson = new JSONObject(videoResponse.getBody());
                String duration = videoJson
                        .getJSONArray("items")
                        .getJSONObject(0)
                        .getJSONObject("contentDetails")
                        .getString("duration");

                result.append("[").append(i + 1).append("] ")
                        .append("https://www.youtube.com/watch?v=").append(videoId)
                        .append(" (").append(parseDuration(duration)).append(")\n");
            }

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    private String parseDuration(String iso) throws DatatypeConfigurationException {
        Duration d = javax.xml.datatype.DatatypeFactory.newInstance()
                .newDuration(iso)
                .getTimeInMillis(new Date()) != 0
                ? Duration.parse(iso)
                : Duration.ZERO;
        long minutes = d.toMinutes();
        long seconds = d.minusMinutes(minutes).getSeconds();
        return minutes + "분 " + seconds + "초";
    }
}
