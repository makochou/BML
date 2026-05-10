package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.core.common.result.PageResult;
import com.bml.module.system.dto.SysNoticeDTO;
import com.bml.module.system.entity.SysNotice;
import com.bml.module.system.vo.SysNoticeVO;

public interface SysNoticeService extends BaseService<SysNotice> {

    PageResult<SysNoticeVO> selectNoticePage(SysNoticeDTO dto);

    SysNoticeVO selectNoticeById(Long id);

    boolean insertNotice(SysNoticeDTO dto);

    boolean updateNotice(SysNoticeDTO dto);

    boolean publishNotice(Long id);

    boolean revokeNotice(Long id);
}
