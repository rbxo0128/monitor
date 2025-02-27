import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

public class App {
    public static void main(String[] args) {
        Monitoring monitoring = new Monitoring();
        monitoring.getNews(System.getenv("KEYWORD"), 10, 1, SortType.date);
    }
}

enum SortType{
    sim("sim"), date("date");

    final String value;

    SortType(String value){
        this.value = value;
    }
}


class Monitoring{
    private final Logger logger;

    public Monitoring(){
        this.logger = Logger.getLogger( this.getClass().getName());
        logger.setLevel(Level.SEVERE);
        logger.setLevel(Level.WARNING);
        logger.info("Monitoring 객체 생성");
    }

    public void getNews(String keyword, int display, int start, SortType sort ){
        String url = "https://openapi.naver.com/v1/search/news.json";
        String params = "?query=%s&display=%d&start=%d&sort=%s".formatted(keyword, display, start, sort.value);

        HttpClient client = HttpClient.newHttpClient();

        try{
            String response = getDataFromAPI("news.json",keyword, display, start, sort);
            String[] tmp = response.split("title\":\"");
            String[] result = new String[tmp.length-1];
            for (int i = 1; i < tmp.length; i++) {
                result[i-1] = tmp[i].split("\"")[0];
            }
            logger.info(Arrays.toString(result));
            logger.info("타이틀 파일 생성");
            File file = new File("news_%s.txt".formatted(keyword));
            if (!file.exists()) {
                logger.info(file.createNewFile() ? "신규 생성" : "이미 있음");
            }

            try (FileWriter fw = new FileWriter(file)){
                for (String s : result) {
                    fw.write(s+"\n");
                }
            }catch (Exception e){
                logger.warning(e.getMessage());
            }

            String imageResponse = getDataFromAPI("image",keyword, display, start, SortType.sim);
            String imageLink = imageResponse
                    .split("\"link\":\"")[1].split("\"")[0]
                    .replace("\\", "");
            logger.info(imageLink);

            
            String tmp2 = imageLink.split("\\.")[0];
            Path path = Path.of("%d_%s.%s".formatted(new Date().getTime(), keyword, "jpg"));

            HttpRequest imageRequest = HttpRequest.newBuilder()
                    .uri(URI.create(imageLink))
                    .build();
            try {
                HttpResponse<Path> imageresponse = client.send(imageRequest, HttpResponse.BodyHandlers.ofFile(path));
            }
            catch (Exception e){
                logger.warning(e.getMessage());
            }
        }catch (Exception e){
            logger.warning(e.getMessage());
        }
    }

    private String getDataFromAPI(String path, String keyword, int display, int start, SortType sort ) throws Exception {
        String url = "https://openapi.naver.com/v1/search/%s".formatted(path);
        String params = "?query=%s&display=%d&start=%d&sort=%s".formatted(keyword, display, start, sort.value);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url+params))
                .header("Content-Type", "plain/text")
                .header("X-Naver-Client-Id", System.getenv("NAVER_CLIENT_ID"))
                .header("X-Naver-Client-Secret", System.getenv("NAVER_CLIENT_SECRET"))
                .GET()
                .build();
        try{
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            logger.info(Integer.toString(response.statusCode()));
            logger.info(response.body());

            return response.body();
        }catch (Exception e){
            logger.severe(e.getMessage());
            throw new Exception();
        }
    }
}
