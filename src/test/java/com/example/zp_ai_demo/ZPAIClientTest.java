package com.example.zp_ai_demo;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ZPAIClientTest {

    @Resource
    ZPAIClient ZPAIClient;

    @Test
    void uploadTest() {
        ZPAIClient.uploadFile("C:\\Users\\温威\\Pictures\\IMG_8085.PNG");
    }

    @Test
    void sendTest() {
        Object response = ZPAIClient.sendRequest("请告诉我俄罗斯的国土面积");
        System.out.println(response.toString());
    }

    @Test
    void sendTest2() {
        Object response = ZPAIClient.sendRequest("请帮我画一个喜羊羊");
        System.out.println(response.toString());
    }

    /**
     * 失败的测试，总是报无法读取图片
     */
    @Test
    void sendWithFileTest() {
        ZPAIClient.sendRequest("附件中的图片是数独，请告诉这个图片中有哪些数字，以右上角为0，0为坐标起点，向右则y+1，向下则x+1，请给我所有数字的坐标，并且只需要直接告诉我对应的json格式，不需要任何其他信息", "C:\\Users\\温威\\Pictures\\IMG_8085.PNG");
    }

    @Test
    void sendFileTest() {
        ZPAIClient.sendRequest("请告诉我附件的图片是什么", "C:\\Users\\温威\\Pictures\\IMG_8085.PNG");
    }

    @Test
    void sendStreamRequestTest() {
        ZPAIClient.sendStreamingRequestWithHttpClient("中国目前的人口数量");
    }

    @Test
    void sendStreamRequestTest2() {
        ZPAIClient.sendStreamingRequestWithAWebClient("中国目前的人口数量");
    }
}