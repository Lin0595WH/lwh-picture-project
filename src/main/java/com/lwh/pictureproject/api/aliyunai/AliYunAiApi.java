package com.lwh.pictureproject.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.lwh.pictureproject.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.lwh.pictureproject.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.lwh.pictureproject.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.lwh.pictureproject.exception.BusinessException;
import com.lwh.pictureproject.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Aliyun DashScope asynchronous image outpainting API.
 */
@Slf4j
@Component
public class AliYunAiApi {

    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    @Value("${aliYunAi.workspaceId:}")
    private String workspaceId;

    private static final String CREATE_TASK_PATH = "/api/v1/services/aigc/image-generation/generation";
    private static final String GET_TASK_PATH = "/api/v1/tasks/%s";
    private static final String DEFAULT_MODEL = "qwen-image-2.1-turbo";
    private static final String OUTPAINTING_PROMPT =
            "请对输入图片进行扩图（outpainting）：保持原图主体、人物、物体、文字、风格和细节完全不变，"
                    + "只自然补全画布四周新增区域，不要裁剪、缩放或重绘原图主体。";

    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest request) {
        if (request == null || request.getInput() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图参数为空");
        }
        try (HttpResponse httpResponse = HttpRequest.post(apiBaseUrl() + CREATE_TASK_PATH)
                .header("Authorization", "Bearer " + apiKey)
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(buildRequestBody(request)))
                .execute()) {
            if (!httpResponse.isOk()) {
                throwHttpError("创建", httpResponse);
            }
            JSONObject responseJson = JSONUtil.parseObj(httpResponse.body());
            throwIfApiError(responseJson);
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(responseJson, CreateOutPaintingTaskResponse.class);
            if (response == null || response.getOutput() == null || StrUtil.isBlank(response.getOutput().getTaskId())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图响应缺少任务 ID");
            }
            return response;
        }
    }

    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 ID 不能为空");
        }
        String encodedTaskId = URLEncoder.encode(taskId, StandardCharsets.UTF_8);
        try (HttpResponse httpResponse = HttpRequest.get(apiBaseUrl() + String.format(GET_TASK_PATH, encodedTaskId))
                .header("Authorization", "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                throwHttpError("查询", httpResponse);
            }
            JSONObject responseJson = JSONUtil.parseObj(httpResponse.body());
            throwIfApiError(responseJson);
            return toOutPaintingTaskResponse(responseJson);
        }
    }

    JSONObject buildRequestBody(CreateOutPaintingTaskRequest request) {
        CreateOutPaintingTaskRequest.Input input = request.getInput();
        JSONArray contentItems = JSONUtil.createArray();
        contentItems.add(JSONUtil.createObj().set("image", input.getImageUrl()));
        contentItems.add(JSONUtil.createObj().set("text", OUTPAINTING_PROMPT));
        JSONObject content = JSONUtil.createObj()
                .set("role", "user")
                .set("content", contentItems);
        JSONArray messages = JSONUtil.createArray();
        messages.add(content);

        JSONObject body = JSONUtil.createObj()
                .set("model", StrUtil.isBlank(request.getModel()) ? DEFAULT_MODEL : request.getModel())
                .set("input", JSONUtil.createObj()
                        .set("messages", messages))
                .set("parameters", JSONUtil.createObj()
                        .set("size", buildOutputSize(input.getImageWidth(), input.getImageHeight(), request.getParameters()))
                        .set("prompt_extend", false)
                        .set("watermark", Boolean.TRUE.equals(request.getParameters() == null
                                ? null : request.getParameters().getAddWatermark())));
        return body;
    }

    private static String buildOutputSize(Integer imageWidth, Integer imageHeight,
                                          CreateOutPaintingTaskRequest.Parameters parameters) {
        if (imageWidth == null || imageHeight == null || imageWidth <= 0 || imageHeight <= 0) {
            return "auto";
        }
        float xScale = parameters == null || parameters.getXScale() == null ? 1F : parameters.getXScale();
        float yScale = parameters == null || parameters.getYScale() == null ? 1F : parameters.getYScale();
        validateScale(xScale, "xScale");
        validateScale(yScale, "yScale");
        int leftOffset = parameters == null || parameters.getLeftOffset() == null ? 0 : parameters.getLeftOffset();
        int rightOffset = parameters == null || parameters.getRightOffset() == null ? 0 : parameters.getRightOffset();
        int topOffset = parameters == null || parameters.getTopOffset() == null ? 0 : parameters.getTopOffset();
        int bottomOffset = parameters == null || parameters.getBottomOffset() == null ? 0 : parameters.getBottomOffset();
        int targetWidth = clampDimension(Math.round(imageWidth * xScale) + Math.max(0, leftOffset) + Math.max(0, rightOffset));
        int targetHeight = clampDimension(Math.round(imageHeight * yScale) + Math.max(0, topOffset) + Math.max(0, bottomOffset));
        return targetWidth + "*" + targetHeight;
    }

    private static void validateScale(float scale, String name) {
        if (scale < 1F || scale > 3F) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, name + " 必须在 1 到 3 之间");
        }
    }

    private static int clampDimension(int dimension) {
        return Math.max(512, Math.min(2048, dimension));
    }

    static GetOutPaintingTaskResponse toOutPaintingTaskResponse(JSONObject responseJson) {
        GetOutPaintingTaskResponse response = JSONUtil.toBean(responseJson, GetOutPaintingTaskResponse.class);
        JSONObject outputJson = responseJson.getJSONObject("output");
        if (response == null || response.getOutput() == null || outputJson == null) {
            return response;
        }
        response.getOutput().setOutputImageUrl(extractOutputImageUrl(outputJson));
        response.getOutput().setCode(outputJson.getStr("code"));
        response.getOutput().setMessage(outputJson.getStr("message"));
        if ("SUCCEEDED".equals(response.getOutput().getTaskStatus())
                && StrUtil.isBlank(response.getOutput().getOutputImageUrl())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图成功但未返回图片地址");
        }
        return response;
    }

    private static String extractOutputImageUrl(JSONObject outputJson) {
        JSONArray choices = outputJson.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        JSONObject choice = choices.getJSONObject(0);
        if (choice == null) {
            return null;
        }
        JSONObject message = choice.getJSONObject("message");
        JSONArray content = message == null ? null : message.getJSONArray("content");
        if (content == null) {
            return null;
        }
        for (Object item : content) {
            if (item instanceof JSONObject) {
                String image = ((JSONObject) item).getStr("image");
                if (StrUtil.isNotBlank(image)) {
                    return image;
                }
            }
        }
        return null;
    }

    private static void throwHttpError(String action, HttpResponse response) {
        String body = response.body();
        JSONObject error = StrUtil.isNotBlank(body) && body.trim().startsWith("{")
                ? JSONUtil.parseObj(body) : JSONUtil.createObj();
        String code = error.getStr("code");
        String message = safeMessage(error.getStr("message"));
        String requestId = error.getStr("request_id");
        log.error("AI 扩图{}失败，HTTP 状态码={}，错误码={}，requestId={}，message={}",
                action, response.getStatus(), code, requestId, message);
        String errorMessage = StrUtil.isBlank(message)
                ? "AI 扩图" + action + "请求失败（HTTP " + response.getStatus() + "）"
                : "AI 扩图失败" + (StrUtil.isBlank(code) ? "" : "，错误码：" + code) + "，" + message;
        throw new BusinessException(ErrorCode.OPERATION_ERROR, errorMessage);
    }

    private static void throwIfApiError(JSONObject responseJson) {
        String code = responseJson.getStr("code");
        if (StrUtil.isNotBlank(code)) {
            String message = safeMessage(responseJson.getStr("message"));
            log.error("AI 扩图 API 返回错误，code={}，requestId={}，message={}",
                    code, responseJson.getStr("request_id"), message);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "AI 扩图失败，错误码：" + code + (StrUtil.isBlank(message) ? "" : "，" + message));
        }
    }

    private static String safeMessage(String message) {
        return message == null ? null : message.replaceAll("https?://\\S+", "[URL]");
    }

    String apiBaseUrl() {
        return StrUtil.isBlank(workspaceId)
                ? "https://dashscope.aliyuncs.com"
                : "https://" + workspaceId + ".cn-beijing.maas.aliyuncs.com";
    }
}
