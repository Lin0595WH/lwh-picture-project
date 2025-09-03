package com.lwh.pictureproject.api.imagesearch.sub;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.lwh.pictureproject.exception.BusinessException;
import com.lwh.pictureproject.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lin
 * @version 1.0.0
 * @description 获取以图搜图页面地址（step 1）
 * @date 2025/3/10 20:27
 */
@Slf4j
public class GetImagePageUrlApi {
    /**
     * 获取以图搜图页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl) {
        // image: https%3A%2F%2Fwww.codefather.cn%2Flogo.png
        //tn: pc
        //from: pc
        //image_source: PC_UPLOAD_URL
        //sdkParams:
        // 1. 准备请求参数
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        // 获取当前时间戳
        long uptime = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;
        try {
            // 2. 发送请求
            HttpResponse httpResponse = HttpRequest.post(url)
                    .form(formData)
                    .header("Acs-Token", "1741581075275_1741617348552_ElDxBUtssTMjNC3ASmMtlVhsQ0haipdDwqfVz2xxbzHBhxBMp76yW63e9Je61/8vuETY3OvX4XVyYMBp1b/yOS8fe32Zqn4UvIgmU8OJbtMgTp+ordexaS9Zl3ZyQrzGqSqAHpXy++QFvNX62pphGGtS561qLTHonHIXhV1GQjh6XscSME8lTblD1QzRzYok3fJCW5wvpw1jZCznOKGHJGNRLkpna1LtZaeKAX5XOva6awXDSUY6b2hfiV5nfcdkHDUTGv2zwnVtaNMrvY6Xo0xZhiHrIeh6F7ZoHvjpNmnC6O5Gf5QFH0IwOzhknu5syJeYReLdQQkeroxMbMMTCmbtcshO9J+eQc895g1SyCiD1wbu7sDQAqvVXwGyxgLgTRM3djJO+l+7EG4wAPeEFk0dqCC9Mlhhu8AzZ3yZnnpnCHFuvpwPaVjHsI+NdIqPeFxK1NLg0+W3Z8iI6gWH7RfX7sM9A/1VUk1Cdb0WoPk=")
                    .timeout(5000)
                    .execute();
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            // 解析响应
            // {"status":0,"msg":"Success","data":{"url":"https://graph.baidu.com/sc","sign":"1262fe97cd54acd88139901734784257"}}
            String body = httpResponse.body();
            Map<String, Object> result = JSONUtil.toBean(body, Map.class);
            // 3. 处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            // 对 URL 进行解码
            String rawUrl = (String) data.get("url");
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            // 如果 URL 为空
            if (StrUtil.isBlank(searchResultUrl)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效的结果地址");
            }
            return searchResultUrl;
        } catch (Exception e) {
            log.error("调用百度以图搜图接口失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    public static void main(String[] args) {
        String url = "https://www.codefather.cn/logo.png";
        String imageFirstUrl = getImagePageUrl(url);
        System.out.println(imageFirstUrl);
    }
}
