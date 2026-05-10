package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file_info")
@Schema(description = "系统文件实体")
public class SysFile extends BaseEntity {

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "存储文件名")
    private String fileName;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "访问地址")
    private String fileUrl;

    @Schema(description = "文件扩展名")
    private String fileExt;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "MIME 类型")
    private String mimeType;

    @Schema(description = "存储类型")
    private Integer storageType;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
