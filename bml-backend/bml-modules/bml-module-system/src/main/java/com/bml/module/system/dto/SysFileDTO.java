package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "文件查询参数")
public class SysFileDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    private String originalName;

    private String fileExt;

    private Integer status;
}
