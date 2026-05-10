package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.core.common.result.PageResult;
import com.bml.module.system.dto.SysJobDTO;
import com.bml.module.system.dto.SysJobLogQuery;
import com.bml.module.system.entity.SysJob;
import com.bml.module.system.vo.SysJobLogVO;
import com.bml.module.system.vo.SysJobVO;

import java.util.List;

public interface SysJobService extends BaseService<SysJob> {

    PageResult<SysJobVO> selectJobPage(SysJobDTO dto);

    SysJobVO selectJobById(Long id);

    boolean insertJob(SysJobDTO dto);

    boolean updateJob(SysJobDTO dto);

    boolean deleteJob(Long id);

    boolean runOnce(Long id);

    boolean changeStatus(Long id, Integer status);

    PageResult<SysJobLogVO> selectLogPage(SysJobLogQuery query);

    boolean cleanLogs();

    List<String> registeredTargets();
}
