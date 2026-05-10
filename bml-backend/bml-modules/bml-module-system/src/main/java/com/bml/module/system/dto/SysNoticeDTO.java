package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "通知公告传输对象")
public class SysNoticeDTO {

    private Long id;

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    @NotBlank(message = "公告标题不能为空")
    private String noticeTitle;

    @NotNull(message = "公告类型不能为空")
    private Integer noticeType;

    @NotBlank(message = "公告内容不能为空")
    private String noticeContent;

    private Integer status;

    private String remark;
}
