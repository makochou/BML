package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 系统配置实体类
 * <p>
 * 以键值对形式存储系统级配置参数，如验证码开关、登录页背景图等。
 * 配置键名（configKey）全局唯一，支持通过中台管理平台动态修改。
 * </p>
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_config")
@Schema(description = "系统配置实体")
public class SysConfig extends BaseEntity {

    /** 配置名称（人类可读的描述） */
    @Schema(description = "配置名称")
    private String configName;

    /** 配置键名（全局唯一，用于程序读取） */
    @Schema(description = "配置键名")
    private String configKey;

    /** 配置键值 */
    @Schema(description = "配置键值")
    private String configValue;

    /** 是否系统内置：1 是，0 否（内置配置不允许删除） */
    @Schema(description = "是否系统内置：1 是，0 否")
    private Integer configType;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;
}
