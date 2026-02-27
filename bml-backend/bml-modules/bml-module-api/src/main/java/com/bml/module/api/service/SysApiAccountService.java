package com.bml.module.api.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bml.core.common.exception.BusinessException;
import com.bml.module.api.dto.SysApiAccountDTO;
import com.bml.module.api.entity.SysApiAccount;
import com.bml.module.api.mapper.SysApiAccountMapper;
import com.bml.module.api.vo.ApiCredentialVO;
import com.bml.module.api.vo.SysApiAccountVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysApiAccountService extends ServiceImpl<SysApiAccountMapper, SysApiAccount> {

    private final ApiSecretCryptoService secretCryptoService;

    public SysApiAccountService(ApiSecretCryptoService secretCryptoService) {
        this.secretCryptoService = secretCryptoService;
    }

    public List<SysApiAccountVO> selectAccountList(SysApiAccountDTO dto) {
        return this.list(new LambdaQueryWrapper<SysApiAccount>()
                .like(dto.getAccountName() != null, SysApiAccount::getAccountName, dto.getAccountName())
                .eq(dto.getStatus() != null, SysApiAccount::getStatus, dto.getStatus())
                .orderByDesc(SysApiAccount::getCreateTime))
                .stream()
                .map(this::toVo)
                .toList();
    }

    public SysApiAccountVO getAccountInfo(Long id) {
        SysApiAccount account = this.getById(id);
        if (account == null) {
            throw new BusinessException("API 账号不存在");
        }
        return toVo(account);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiCredentialVO insertAccount(SysApiAccountDTO dto) {
        if (dto.getAccountName() == null || dto.getAccountName().trim().isEmpty()) {
            throw new BusinessException("账号名称不能为空");
        }

        String plaintextSecret = RandomUtil.randomString(32);
        SysApiAccount account = new SysApiAccount();
        account.setAccountName(dto.getAccountName());
        account.setAccountType(dto.getAccountType() == null ? 1 : dto.getAccountType());
        account.setRateLimit(dto.getRateLimit() == null ? 1000 : dto.getRateLimit());
        account.setExpireTime(dto.getExpireTime());
        account.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        account.setRemark(dto.getRemark());
        account.setAccessKey("ak_" + IdUtil.simpleUUID());
        account.setSecretKey(secretCryptoService.encrypt(plaintextSecret));

        this.save(account);
        return ApiCredentialVO.builder()
                .id(account.getId())
                .accountName(account.getAccountName())
                .accessKey(account.getAccessKey())
                .secretKey(plaintextSecret)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateAccount(SysApiAccountDTO dto) {
        SysApiAccount account = this.getById(dto.getId());
        if (account == null) {
            throw new BusinessException("API 账号不存在");
        }

        if (dto.getAccountName() != null) {
            account.setAccountName(dto.getAccountName());
        }
        if (dto.getAccountType() != null) {
            account.setAccountType(dto.getAccountType());
        }
        if (dto.getRateLimit() != null) {
            account.setRateLimit(dto.getRateLimit());
        }
        if (dto.getExpireTime() != null) {
            account.setExpireTime(dto.getExpireTime());
        }
        if (dto.getStatus() != null) {
            account.setStatus(dto.getStatus());
        }
        if (dto.getRemark() != null) {
            account.setRemark(dto.getRemark());
        }

        return this.updateById(account);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiCredentialVO resetSecret(Long id) {
        SysApiAccount account = this.getById(id);
        if (account == null) {
            throw new BusinessException("API 账号不存在");
        }

        String newSecret = RandomUtil.randomString(32);
        account.setSecretKey(secretCryptoService.encrypt(newSecret));
        this.updateById(account);

        return ApiCredentialVO.builder()
                .id(account.getId())
                .accountName(account.getAccountName())
                .accessKey(account.getAccessKey())
                .secretKey(newSecret)
                .build();
    }

    private SysApiAccountVO toVo(SysApiAccount account) {
        SysApiAccountVO vo = new SysApiAccountVO();
        vo.setId(account.getId());
        vo.setAccountName(account.getAccountName());
        vo.setAccessKey(account.getAccessKey());
        vo.setAccountType(account.getAccountType());
        vo.setRateLimit(account.getRateLimit());
        vo.setExpireTime(account.getExpireTime());
        vo.setStatus(account.getStatus());
        vo.setRemark(account.getRemark());
        vo.setCreateTime(account.getCreateTime());
        vo.setUpdateTime(account.getUpdateTime());
        return vo;
    }
}
