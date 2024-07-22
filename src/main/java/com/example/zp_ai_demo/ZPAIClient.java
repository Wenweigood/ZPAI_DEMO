package com.example.zp_ai_demo;

import com.alibaba.fastjson.JSON;
import com.example.zp_ai_demo.entity.*;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;


import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.*;

@Service
@Slf4j
public class ZPAIClient implements InitializingBean {

//    private static final Logger log = LoggerFactory.getLogger(ZPAIClient.class);
    @Value("${zhipu.api.key}")
    private String apiKey;

    @Value("${zhipu.api.secret}")
    private String apiSecret;

    @Value("${zhipu.api.path}")
    private String zhipuPath;

    @Resource
    private RestTemplate restTemplate;

    @Value("${assistant_id}")
    private String assistant_id;

    /**
     * token 有效期10天
     */
    private String accessToken = "";

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("api_key", apiKey);
        paramMap.put("api_secret", apiSecret);
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(zhipuPath +"get_token", paramMap, Map.class);
        try {
            accessToken = Optional.of(responseEntity).map(ResponseEntity::getBody)
                    .map(it -> it.get("result"))
                    .map(it -> (Map<String, String>) it)
                    .map(it -> it.get("access_token"))
                    .map(it -> (String) it)
                    .orElse("");
        } catch (ClassCastException e) {
            log.info(e.getMessage());
        }
    }

    /**
     * 一个简单的请求
     * @param message
     * @return
     */
    public Object sendRequest(String message){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // 设置请求体类型为JSON
        headers.add("Authorization", "Bearer "+accessToken); // 添加自定义header

        paramMapBuilder paramMapBuilder = new paramMapBuilder();
        Map<String, Object> paramMap = paramMapBuilder.addParam("prompt", message)
                .addParam("assistant_id", assistant_id)
                .build();

        HttpEntity<Object> entity = new HttpEntity<>(paramMap, headers);
        ResponseEntity<ZPAIResponse> responseEntity = restTemplate.postForEntity(zhipuPath +"stream_sync", entity, ZPAIResponse.class);
        return handleResponse(responseEntity);
    }

    /**
     * 带文件请求
     * @param message
     * @param filePath
     * @return
     */
    public Object sendRequest(String message, String filePath){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // 设置请求体类型为JSON
        headers.add("Authorization", "Bearer "+accessToken); // 添加自定义header

        String file_id = uploadFile(filePath);
        List<Dict> file_list = new ArrayList<>();
        file_list.add(new Dict(file_id));
        paramMapBuilder paramMapBuilder = new paramMapBuilder();
        Map<String, Object> paramMap = paramMapBuilder.addParam("prompt", message)
                .addParam("assistant_id", assistant_id)
                .addParam("file_list", file_list)
//                .addParam("meta_data", Dict.class)
                .build();



        HttpEntity<Object> entity = new HttpEntity<>(paramMap, headers);
        ResponseEntity<ZPAIResponse> responseEntity = restTemplate.postForEntity(zhipuPath +"stream_sync", entity, ZPAIResponse.class);
        return handleResponse(responseEntity);
    }

    public void sendStreamingRequestWithHttpClient(String message){

        paramMapBuilder paramMapBuilder = new paramMapBuilder();
        Map<String, Object> paramMap = paramMapBuilder.addParam("prompt", message)
                .addParam("assistant_id", assistant_id)
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(zhipuPath + "stream"))
                .header("Accept", "text/event-stream")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer "+accessToken)
                // 假设需要POST一些数据
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(paramMap)))
                .build();

        client.sendAsync(request, BodyHandlers.ofInputStream())
                .thenApply(HttpResponse::body)
                .thenAccept(inputStream -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // 假设每行都是一个完整的JSON对象
                            // 这里可以使用Jackson或Gson来解析line到Java对象
//                            System.out.println("Received: " + line);
                            if(!line.trim().isEmpty() && line.startsWith("data:")){
                                Result result = JSON.parseObject(line.substring(5).trim(), Result.class);
                                System.out.println(handleResponse(result));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).join();
    }

    public void sendStreamingRequestWithAWebClient(String message){

        WebClient webClient = WebClient.create(zhipuPath);

        webClient = webClient.mutate()
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .defaultHeader("Authorization", "Bearer "+accessToken)
                .build();

        paramMapBuilder paramMapBuilder = new paramMapBuilder();
        Map<String, Object> paramMap = paramMapBuilder.addParam("prompt", message)
                .addParam("assistant_id", assistant_id)
                .build();

        Mono<String> responseMono = webClient.post()
                .uri("stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(paramMap) // 将POJO对象序列化为JSON并作为请求体发送
                .retrieve()
                .bodyToMono(String.class);

        responseMono.subscribe(
                response -> System.out.println("Server response: " + response),
                error -> System.err.println("Error occurred: " + error.getMessage()),
                () -> System.out.println("POST request completed")
        );
    }

    static class paramMapBuilder implements Serializable {
        private HashMap<String, Object> paramMap = new HashMap<>();

        public paramMapBuilder addParam(String key, Object value) {
            if (StringUtils.isEmpty(key)) {
                throw new RuntimeException("参数设置不正确！");
            }
            paramMap.put(key, value);
            return this;
        }

        public Map<String, Object> build(){
            return paramMap;
        }
    }

    /**
     * 处理返回体
     * @param responseEntity
     * @return
     */
    @Nonnull
    public String handleResponse(ResponseEntity<ZPAIResponse> responseEntity) {
        try {
            return Optional.ofNullable(responseEntity).map(ResponseEntity::getBody)
                    .map(ZPAIResponse::getResult)
                    .map(Result::getOutput)
                    .orElseGet(ArrayList::new)
                    .stream().map(Output::getContent)
                    .flatMap(List::stream)
                    .map(Content::getResult)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
        } catch (NullPointerException e){
            log.info(e.getMessage());
        }
        return "";
    }

    /**
     * 处理流式返回体
     * @param result
     * @return
     */
    @Nonnull
    public String handleResponse(Result result) {
        try {
            return Optional.ofNullable(result).map(Result::getMessage)
                    .map(Message::getContent)
                    .map(Content::getResult)
                    .orElse("");
        } catch (NullPointerException e){
            log.info(e.getMessage());
        }
        return "";
    }

    @Data
    @AllArgsConstructor
    static class Dict implements Serializable {
        String file_id;
    }

    /**
     * 上传文件方法
     * @return file_id
     */
    @Nullable
    public String uploadFile(String filePath){
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        FileSystemResource file = new FileSystemResource(filePath);
        formData.add("file", file);
        // 设置HTTP头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Authorization", "Bearer "+accessToken); // 添加自定义header
        // 创建HttpEntity
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(formData, headers);
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(zhipuPath + "file_upload ", requestEntity, Map.class);
        return Optional.ofNullable(responseEntity).map(ResponseEntity::getBody).map(it->(Map)it.get("result")).map(it->(String)it.get("file_id")).orElse(null);
    }

}
