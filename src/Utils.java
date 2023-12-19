import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Utils {
    private static int prevHour = -1;
    private static List<Topic> cache = new ArrayList<>();
    private static String getFormDataAsString(Map<String, String> formData) {
        StringBuilder formBodyBuilder = new StringBuilder();
        for (Map.Entry<String, String> singleEntry : formData.entrySet()) {
            if (formBodyBuilder.length() > 0)
                formBodyBuilder.append("&");
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getKey(), StandardCharsets.UTF_8)).append("=").append(URLEncoder.encode(singleEntry.getValue(), StandardCharsets.UTF_8));
        }
        return formBodyBuilder.toString();
    }

    public static List<Topic> getTopics(String date, Calendar topicCalendar) throws IOException, InterruptedException {
        List<String> result = new ArrayList<>();
        String tbodySelector = "#ZajeciaTable > tbody";
        String url = "https://planzajec.pjwstk.edu.pl/PlanOgolny3.aspx?fbclid=IwAR3kmHBVLgHu2lFAeLn62j0Gge5zPKiAy1C68dZteL2yzLfZv8BKyK46hT0";
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = null;
        if(date == null){
            request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(url))
                    .setHeader("User-Agent", "Mozilla/5.0") // add request header
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("referer", "https://planzajec.pjwstk.edu.pl/PlanOgolny3.aspx")
                    .build();
        }else
        {
            Map<String, String> formData = new HashMap<>();
            formData.put("DataPicker$dateInput", date);
            formData.put("DataPicker_dateInput_ClientState", "{\"enabled\":true,\"emptyMessage\":\"\",\"validationText\":\""+date+"-00-00-00\",\"valueAsString\":\""+date+"-00-00-00\",\"minDateStr\":\"1980-01-01-00-00-00\",\"maxDateStr\":\"2099-12-31-00-00-00\",\"lastSetTextBoxValue\":\""+date+"\"}");
            formData.put("__EVENTTARGET", "DataPicker");

            request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
                    .uri(URI.create("https://planzajec.pjwstk.edu.pl/PlanOgolny3.aspx"))
                    .setHeader("User-Agent", "Mozilla/5.0") // add request header
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("referer", "https://planzajec.pjwstk.edu.pl/PlanOgolny3.aspx")
                    .build();
        }

        String resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        Document doc = Jsoup.parse(resp, Parser.htmlParser().toString());
        Element selectTBody = doc.select(tbodySelector).first();
        Elements childs = selectTBody.children();
        List<Topic> allTopics = new ArrayList<>();
        for(int i = 0; i < childs.size(); ++i){
            Element child = childs.get(i);
            Elements tds = child.children();
            List<Topic> topics = extractTopics(tds, topicCalendar);
            allTopics.addAll(topics);
        }
        return allTopics.stream().distinct().collect(Collectors.toList());
    }



    public static String getSchedule(String date) throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        Map<String, String> formData = new HashMap<>();
        formData.put("DataPicker$dateInput", date);
        formData.put("DataPicker_dateInput_ClientState", "{\"enabled\":true,\"emptyMessage\":\"\",\"validationText\":\""+date+"-00-00-00\",\"valueAsString\":\""+date+"-00-00-00\",\"minDateStr\":\"1980-01-01-00-00-00\",\"maxDateStr\":\"2099-12-31-00-00-00\",\"lastSetTextBoxValue\":\""+date+"\"}");
        formData.put("__EVENTTARGET", "DataPicker");

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
                .uri(URI.create("https://planzajec.pjwstk.edu.pl/PlanOgolny3.aspx"))
                .setHeader("User-Agent", "Mozilla/5.0") // add request header
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("referer", "https://planzajec.pjwstk.edu.pl/PlanOgolny3.aspx")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }
    private static boolean isNumber(String str){
        try{
            Integer.parseInt(str);
        }catch (java.lang.NumberFormatException e){
            return false;
        }
        return true;
    }
    private static List<Topic> extractTopics(Elements tds, Calendar calendar){
        if(tds.size() < 3){
            return new ArrayList<>();
        }
        List<Topic> result = new ArrayList<>();
        Element hourRow = tds.get(0);
        Element minuteRow = tds.get(1);

        String hourStr = hourRow.text();
        String minuteStr = minuteRow.text();
        if(!isNumber(hourStr)){
            return new ArrayList<>();
        }
        boolean isMinutes = hourRow.attr("class").contains("minuty");

        if(isMinutes && calendar!=null){
            calendar.set(Calendar.HOUR_OF_DAY,prevHour);
            calendar.set(Calendar.MINUTE,Integer.parseInt(hourStr));
        }else if(calendar != null){
            if(isNumber(hourStr) && isNumber(minuteStr)){
                prevHour = Integer.parseInt(hourStr);
                calendar.set(Calendar.MINUTE,Integer.parseInt(hourStr));
                calendar.set(Calendar.HOUR_OF_DAY,Integer.parseInt(minuteStr));
            }
        }
        for(Element td: tds){
            boolean isTopicTd = td.attr("align").equals("center");
            if(isTopicTd){
                String[] inner = td.text().split("<br>");
                Topic topic = new Topic(inner[0], calendar == null ? new Date() : calendar.getTime());
                updateCache(topic);
                result.add(topic);
            }
        }
        return result;
    }

    public static void clearCache(){
        Utils.cache.clear();
    }

    public static void updateCache(Topic topic){
        Utils.cache.add(topic);
    }
    public static void updateCache(List<Topic> topics){
        Utils.cache.addAll(topics);
    }

    public static List<Topic> getCache(){
        return Utils.cache;
    }

    public static List<Topic> getAllFromRange(Date from, Date to) throws IOException, InterruptedException {
        List<Topic> alltopics = new ArrayList<>();
        long diffInMillies = Math.abs(from.getTime() - to.getTime());
        long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        SimpleDateFormat date = new SimpleDateFormat();
        date.applyPattern("yyyy-MM-dd");

        Calendar c = Calendar.getInstance();
        c.setTime(from);
        for(int i = 0; i < diff; ++i){
            System.out.println("diff");
            c.add(Calendar.DATE, 1);
            List<Topic> topicsInDay = Utils.getTopics(date.format(c.getTime()), c);
            alltopics.addAll(topicsInDay);
        }
        Utils.clearCache();
        Utils.updateCache(alltopics);

        return alltopics;
    }
}

