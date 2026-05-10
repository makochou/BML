package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
@Schema(description = "通知公告实体")
public class SysNotice extends BaseEntity {

    @Schema(description = "公告标题")
    private String noticeTitle;

    @Schema(description = "公告类型")
    private Integer noticeType;

    @Schema(description = "公告内容")
    private String noticeContent;

    @Schema(description = "公告状态")
    private Integer status;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "备注")
    private String remark;
}
