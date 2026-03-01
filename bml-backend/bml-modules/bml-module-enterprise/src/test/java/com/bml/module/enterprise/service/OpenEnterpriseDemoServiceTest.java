package com.bml.module.enterprise.service;

import com.bml.core.common.exception.BusinessException;
import com.bml.core.common.result.PageResult;
import com.bml.module.enterprise.dto.EnterpriseCompanyPageQuery;
import com.bml.module.enterprise.dto.EnterpriseSystemAccountPageQuery;
import com.bml.module.enterprise.vo.OpenEnterpriseCompanyVO;
import com.bml.module.enterprise.vo.OpenEnterpriseSystemAccountVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenEnterpriseDemoServiceTest {

    private final OpenEnterpriseDemoService service = new OpenEnterpriseDemoService();

    @Test
    void shouldFilterCompaniesByKeywordAndIndustry() {
        EnterpriseCompanyPageQuery query = new EnterpriseCompanyPageQuery();
        query.setKeyword("智联");
        query.setIndustry("供应链");
        query.setPageNum(1);
        query.setPageSize(10);

        PageResult<OpenEnterpriseCompanyVO> result = service.pageCompanies(query);

        assertEquals(1L, result.getTotal());
        assertEquals("华东智联供应链", result.getRecords().get(0).getCompanyName());
    }

    @Test
    void shouldPageSystemAccountsAndRejectUnknownDetail() {
        EnterpriseSystemAccountPageQuery query = new EnterpriseSystemAccountPageQuery();
        query.setCompanyId(1001L);
        query.setPageNum(1);
        query.setPageSize(1);

        PageResult<OpenEnterpriseSystemAccountVO> result = service.pageSystemAccounts(query);

        assertEquals(2L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertThrows(BusinessException.class, () -> service.getSystemAccountDetail(999999L));
    }
}
