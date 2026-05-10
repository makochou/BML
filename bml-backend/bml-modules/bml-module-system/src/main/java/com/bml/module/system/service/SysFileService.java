package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.core.common.result.PageResult;
import com.bml.module.system.dto.SysFileDTO;
import com.bml.module.system.entity.SysFile;
import com.bml.module.system.vo.SysFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface SysFileService extends BaseService<SysFile> {

    PageResult<SysFileVO> selectFilePage(SysFileDTO dto);

    SysFileVO selectFileById(Long id);

    SysFileVO uploadFile(MultipartFile file);

    boolean deleteFile(Long id);

    Path resolveFilePath(Long id);
}
