package com.lwh.pictureproject.manager.websocket.strategy;


import cn.hutool.core.text.CharSequenceUtil;
import com.lwh.pictureproject.manager.websocket.strategy.impl.ErrorMessageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author Lin
 * @Date 2025/9/20 22:49
 * @Descriptions 消息处理策略工厂
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PictureEditMessageStrategyFactory {

    // 注入所有策略接口的实现类（Spring自动扫描所有@Component标注的实现类）
    private final List<PictureEditMessageStrategy> strategyList;

    // 默认错误策略
    private final ErrorMessageStrategy errorMessageStrategy;

    // 存储策略的Map，初始化后不会修改
    private final Map<String, PictureEditMessageStrategy> strategies = new HashMap<>();

    /**
     * 初始化策略Map，自动注册所有策略
     */
    @PostConstruct
    public void init() {
        // 遍历所有策略实现类，自动注册
        for (PictureEditMessageStrategy strategy : strategyList) {
            String messageType = strategy.getMessageType();
            // 合并所有需要跳过的条件
            if (CharSequenceUtil.isBlank(messageType)) {
                log.error("消息类型不能为空，请检查策略实现类: {}", strategy.getClass().getSimpleName());
                continue;
            }
            // 避免重复注册（如果有多个相同类型的策略，后者会覆盖前者）
            if (strategies.containsKey(messageType)) {
                log.warn("消息类型[{}]存在重复的策略实现，已覆盖", messageType);
            }
            strategies.put(messageType, strategy);
            log.info("已注册消息策略：类型={}, 策略类={}", messageType, strategy.getClass().getSimpleName());
        }
    }

    /**
     * 获取消息处理策略（增加日志和校验）
     *
     * @param messageType 消息类型
     * @return 对应的策略，默认返回错误策略
     */
    public PictureEditMessageStrategy getStrategy(String messageType) {
        PictureEditMessageStrategy strategy = strategies.get(messageType);
        if (strategy == null) {
            log.error("未找到消息类型[{}]对应的处理策略，将使用默认错误策略", messageType);
            return errorMessageStrategy;
        }
        return strategy;
    }
}
