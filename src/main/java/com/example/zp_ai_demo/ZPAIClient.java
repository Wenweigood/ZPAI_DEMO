package com.example.zp_ai_demo;

import com.example.zp_ai_demo.entity.Content;
import com.example.zp_ai_demo.entity.Output;
import com.example.zp_ai_demo.entity.Result;
import com.example.zp_ai_demo.entity.ZPAIResponse;
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

import java.io.Serializable;
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
