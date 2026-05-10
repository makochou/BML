package com.bml.module.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.result.PageResult;
import com.bml.module.system.dto.SysLoginLogQuery;
import com.bml.module.system.entity.SysLoginLog;
import com.bml.module.system.mapper.SysLoginLogMapper;
import com.bml.module.system.service.SysLoginLogService;
import com.bml.module.system.vo.SysLoginLogVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 系统登录日志服务实现。
 * <p>
 * 统一封装认证事件写入与审计查询。登录日志记录属于安全审计辅助能力，写入失败只记录服务端告警日志，不阻断登录、登出或修改密码主流程。
 * </p>
 *
 * @author BML Team
 */
@Slf4j
@Service
public class SysLoginLogServiceImpl extends BaseServiceImpl<SysLoginLogMapper, SysLoginLog>
        implements SysLoginLogService {

    private static final int DEFAULT_PAGE_NUM = 1;

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 200;

    private static final String UNKNOWN = "unknown";

    @Override
    public PageResult<SysLoginLogVO> selectLoginLogPage(SysLoginLogQuery query) {
        SysLoginLogQuery safeQuery = query == null ? new SysLoginLogQuery() : query;
        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        Page<SysLoginLog> page = this.page(new Page<>(pageNum, pageSize), buildQueryWrapper(safeQuery));
        List<SysLoginLogVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public SysLoginLogVO selectLoginLogById(Long id) {
        SysLoginLog loginLog = id == null ? null : this.getById(id);
        return loginLog == null ? null : toVO(loginLog);
    }

    @Override
    public void recordLoginEvent(String username, boolean success, String message, HttpServletRequest request) {
        try {
            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUsername(StrUtil.blankToDefault(username, UNKNOWN));
            loginLog.setIpaddr(request == null ? null : getClientIp(request));
            loginLog.setLoginLocation("内网/未知");
            loginLog.setBrowser(resolveBrowser(request));
            loginLog.setOs(resolveOs(request));
            loginLog.setStatus(success ? 1 : 0);
            loginLog.setMsg(StrUtil.blankToDefault(message, success ? "认证成功" : "认证失败"));
            loginLog.setLoginTime(LocalDateTime.now());
            this.save(loginLog);
        } catch (Exception ex) {
            log.warn("系统登录日志记录失败，不影响认证主流程", ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLoginLogs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return this.removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanLoginLogs() {
        return this.baseMapper.delete(new QueryWrapper<SysLoginLog>().isNotNull("id"));
    }

    private LambdaQueryWrapper<SysLoginLog> buildQueryWrapper(SysLoginLogQuery query) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getUsername()), SysLoginLog::getUsername, query.getUsername())
                .like(StrUtil.isNotBlank(query.getIpaddr()), SysLoginLog::getIpaddr, query.getIpaddr())
                .like(StrUtil.isNotBlank(query.getMsg()), SysLoginLog::getMsg, query.getMsg())
                .eq(query.getStatus() != null, SysLoginLog::getStatus, query.getStatus())
                .ge(query.getBeginTime() != null, SysLoginLog::getLoginTime, query.getBeginTime())
                .le(query.getEndTime() != null, SysLoginLog::getLoginTime, query.getEndTime())
                .orderByDesc(SysLoginLog::getLoginTime, SysLoginLog::getId);
        return wrapper;
    }

    private SysLoginLogVO toVO(SysLoginLog loginLog) {
        SysLoginLogVO vo = new SysLoginLogVO();
        BeanUtils.copyProperties(loginLog, vo);
        return vo;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String getClientIp(HttpServletRequest request) {
        return Arrays.stream(new String[] {
                request.getHeader("X-Forwarded-For"),
                request.getHeader("X-Real-IP"),
                request.getHeader("Proxy-Client-IP"),
                request.getHeader("WL-Proxy-Client-IP"),
                request.getRemoteAddr()
        })
                .filter(StrUtil::isNotBlank)
                .filter(ip -> !UNKNOWN.equalsIgnoreCase(ip))
                .map(ip -> ip.split(",")[0].trim())
                .findFirst()
                .orElse(null);
    }

    private String resolveBrowser(HttpServletRequest request) {
        String userAgent = request == null ? "" : StrUtil.blankToDefault(request.getHeader("User-Agent"), "").toLowerCase();
        if (userAgent.contains("edg/")) {
            return "Edge";
        }
        if (userAgent.contains("chrome/")) {
            return "Chrome";
        }
        if (userAgent.contains("firefox/")) {
            return "Firefox";
        }
        if (userAgent.contains("safari/") && !userAgent.contains("chrome/")) {
            return "Safari";
        }
        return "未知浏览器";
    }

    private String resolveOs(HttpServletRequest request) {
        String userAgent = request == null ? "" : StrUtil.blankToDefault(request.getHeader("User-Agent"), "").toLowerCase();
        if (userAgent.contains("windows")) {
            return "Windows";
        }
        if (userAgent.contains("mac os")) {
            return "macOS";
        }
        if (userAgent.contains("android")) {
            return "Android";
        }
        if (userAgent.contains("iphone") || userAgent.contains("ipad")) {
            return "iOS";
        }
        if (userAgent.contains("linux")) {
            return "Linux";
        }
        return "未知系统";
    }
}
