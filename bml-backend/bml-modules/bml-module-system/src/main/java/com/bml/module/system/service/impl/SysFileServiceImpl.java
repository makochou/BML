package com.bml.module.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.result.PageResult;
import com.bml.module.system.dto.SysFileDTO;
import com.bml.module.system.entity.SysFile;
import com.bml.module.system.mapper.SysFileMapper;
import com.bml.module.system.service.SysFileService;
import com.bml.module.system.vo.SysFileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SysFileServiceImpl extends BaseServiceImpl<SysFileMapper, SysFile> implements SysFileService {

    @Value("${bml.file.storage-dir:data/files}")
    private String fileStorageDir;

    @Override
    public PageResult<SysFileVO> selectFilePage(SysFileDTO dto) {
        SysFileDTO safeDto = dto == null ? new SysFileDTO() : dto;
        int pageNum = normalizePageNum(safeDto.getPageNum());
        int pageSize = normalizePageSize(safeDto.getPageSize());
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<SysFile>()
                .like(StrUtil.isNotBlank(safeDto.getOriginalName()), SysFile::getOriginalName, safeDto.getOriginalName())
                .eq(StrUtil.isNotBlank(safeDto.getFileExt()), SysFile::getFileExt, safeDto.getFileExt())
                .eq(safeDto.getStatus() != null, SysFile::getStatus, safeDto.getStatus())
                .orderByDesc(SysFile::getCreateTime, SysFile::getId);
        Page<SysFile> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        List<SysFileVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public SysFileVO selectFileById(Long id) {
        return id == null ? null : toVO(this.getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFileVO uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择上传文件");
        }
        String originalName = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
        String ext = extractExt(originalName);
        String storedName = UUID.randomUUID().toString().replace("-", "") + (StrUtil.isBlank(ext) ? "" : "." + ext);
        Path dir = Paths.get(fileStorageDir);
        Path target = dir.resolve(storedName);
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("保存文件失败：" + ex.getMessage(), ex);
        }
        SysFile entity = new SysFile();
        entity.setOriginalName(originalName);
        entity.setFileName(storedName);
        entity.setFilePath(target.toString());
        entity.setFileUrl("/api/system/file/download/" + storedName);
        entity.setFileExt(ext);
        entity.setFileSize(file.getSize());
        entity.setMimeType(file.getContentType());
        entity.setStorageType(1);
        entity.setStatus(1);
        this.save(entity);
        entity.setFileUrl("/api/system/file/download/" + entity.getId());
        this.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFile(Long id) {
        SysFile file = id == null ? null : this.getById(id);
        if (file == null) {
            return false;
        }
        return this.removeById(id);
    }

    @Override
    public Path resolveFilePath(Long id) {
        SysFile file = id == null ? null : this.getById(id);
        if (file == null || StrUtil.isBlank(file.getFilePath())) {
            return null;
        }
        return Paths.get(file.getFilePath());
    }

    private SysFileVO toVO(SysFile entity) {
        if (entity == null) {
            return null;
        }
        SysFileVO vo = new SysFileVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private String extractExt(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 200);
    }
}
