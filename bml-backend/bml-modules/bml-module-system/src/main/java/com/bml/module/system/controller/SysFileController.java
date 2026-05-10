package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.PageResult;
import com.bml.core.common.result.Result;
import com.bml.core.framework.operlog.BusinessType;
import com.bml.core.framework.operlog.OperationLog;
import com.bml.module.system.dto.SysFileDTO;
import com.bml.module.system.service.SysFileService;
import com.bml.module.system.vo.SysFileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Tag(name = "文件管理")
@RestController
@RequestMapping("/system/file")
public class SysFileController extends BaseController {

    @Resource
    private SysFileService fileService;

    @Operation(summary = "分页查询文件")
    @PreAuthorize("@ss.hasPermi('system:file:list')")
    @GetMapping("/page")
    public Result<PageResult<SysFileVO>> page(SysFileDTO dto) {
        return Result.ok(fileService.selectFilePage(dto));
    }

    @Operation(summary = "查询文件详情")
    @PreAuthorize("@ss.hasPermi('system:file:query')")
    @GetMapping("/{id}")
    public Result<SysFileVO> getInfo(@PathVariable Long id) {
        return Result.ok(fileService.selectFileById(id));
    }

    @Operation(summary = "上传文件")
    @OperationLog(title = "文件管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('system:file:upload')")
    @PostMapping("/upload")
    public Result<SysFileVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.uploadFile(file));
    }

    @Operation(summary = "删除文件")
    @OperationLog(title = "文件管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('system:file:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        return toAjax(fileService.deleteFile(id));
    }

    @Operation(summary = "下载文件")
    @PreAuthorize("@ss.hasPermi('system:file:download')")
    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        SysFileVO file = fileService.selectFileById(id);
        Path path = fileService.resolveFilePath(id);
        if (file == null || path == null || !Files.exists(path)) {
            response.setStatus(404);
            return;
        }
        response.setContentType(file.getMimeType() == null ? "application/octet-stream" : file.getMimeType());
        String encodedName = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        Files.copy(path, response.getOutputStream());
        response.getOutputStream().flush();
    }
}
