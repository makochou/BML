package com.bml.module.api.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bml.core.common.exception.BusinessException;
import com.bml.module.api.dto.SysApiAccountDTO;
import com.bml.module.api.entity.SysApiAccount;
import com.bml.module.api.mapper.SysApiAccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * API账号服务
 *
 * @author BML Team
 */
@Service
public class SysApiAccountService extends ServiceImpl<SysApiAccountMapper, SysApiAccount> {

    /**
     * 查询API账号列表
     */
    public List<SysApiAccount> selectAccountList(SysApiAccountDTO dto) {
        return this.list(new LambdaQueryWrapper<SysApiAccount>()
                .like(dto.getAccountName() != null, SysApiAccount::getAccountName, dto.getAccountName())
                .eq(dto.getStatus() != null, SysApiAccount::getStatus, dto.getStatus())
                .orderByDesc(SysApiAccount::getCreateTime));
    }

    /**
     * 新增API账号
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean insertAccount(SysApiAccountDTO dto) {
        if (dto.getAccountName() == null || dto.getAccountName().trim().isEmpty()) {
            throw new BusinessException("账号名称不能为空");
        }
        SysApiAccount account = new SysApiAccount();
        // 复制属性
        account.setAccountName(dto.getAccountName());
        account.setAccountType(dto.getAccountType());
        account.setRateLimit(dto.getRateLimit());
        account.setExpireTime(dto.getExpireTime());
        account.setStatus(dto.getStatus());
        account.setRemark(dto.getRemark());

        // 自动生成密钥
        account.setAccessKey("ak_" + IdUtil.simpleUUID());
        account.setSecretKey(RandomUtil.randomString(32));

        return this.save(account);
    }

    /**
     * 修改API账号
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAccount(SysApiAccountDTO dto) {
        SysApiAccount account = this.getById(dto.getId());
        if (account == null) {
            throw new BusinessException("API账号不存在");
        }

        if (dto.getAccountName() != null)
            account.setAccountName(dto.getAccountName());
        if (dto.getAccountType() != null)
            account.setAccountType(dto.getAccountType());
        if (dto.getRateLimit() != null)
            account.setRateLimit(dto.getRateLimit());
        if (dto.getExpireTime() != null)
            account.setExpireTime(dto.getExpireTime());
        if (dto.getStatus() != null)
            account.setStatus(dto.getStatus());
        if (dto.getRemark() != null)
            account.setRemark(dto.getRemark());

        return this.updateById(account);
    }

    /**
     * 重置密钥
     */
    @Transactional(rollbackFor = Exception.class)
    public String resetSecret(Long id) {
        SysApiAccount account = this.getById(id);
        if (account == null) {
            throw new BusinessException("API账号不存在");
        }

        String newSecret = RandomUtil.randomString(32);
        account.setSecretKey(newSecret);
        this.updateById(account);

        return newSecret;
    }
}
