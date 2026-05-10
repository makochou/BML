package com.bml.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "缓存监控视图对象")
public class SysCacheVO {

    private Long dbSize;

    private String redisVersion;

    private String usedMemoryHuman;

    private String connectedClients;

    private String uptimeInDays;

    private List<String> keys;

    private Map<String, Object> info;
}
