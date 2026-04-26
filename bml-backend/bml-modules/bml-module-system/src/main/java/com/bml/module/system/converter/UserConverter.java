package com.bml.module.system.converter;

import com.bml.module.system.dto.SysUserDTO;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.vo.SysUserVO;

import java.util.ArrayList;
import java.util.List;

/**
 * User mapper implemented manually to avoid MapStruct/Lombok inheritance issues
 * on generated classes.
 */
public final class UserConverter {

    public static final UserConverter INSTANCE = new UserConverter();

    private UserConverter() {
    }

    public SysUser toEntity(SysUserDTO dto) {
        if (dto == null) {
            return null;
        }

        SysUser user = new SysUser();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender());
        user.setAvatar(dto.getAvatar());
        user.setStatus(dto.getStatus());
        user.setDeptId(dto.getDeptId());
        user.setRemark(dto.getRemark());
        return user;
    }

    public SysUserVO toVO(SysUser entity) {
        if (entity == null) {
            return null;
        }

        SysUserVO vo = new SysUserVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setNickname(entity.getNickname());
        vo.setEmail(entity.getEmail());
        vo.setPhone(entity.getPhone());
        vo.setGender(entity.getGender());
        vo.setAvatar(entity.getAvatar());
        vo.setStatus(entity.getStatus());
        vo.setOrgId(entity.getOrgId());
        vo.setDeptId(entity.getDeptId());
        vo.setPostId(entity.getPostId());
        vo.setEmployeeNo(entity.getEmployeeNo());
        vo.setEntryDate(entity.getEntryDate());
        vo.setCreateTime(entity.getCreateTime());
        vo.setLoginIp(entity.getLoginIp());
        vo.setLoginDate(entity.getLoginDate());
        vo.setRemark(entity.getRemark());
        return vo;
    }

    public List<SysUserVO> toVOList(List<SysUser> list) {
        if (list == null) {
            return null;
        }

        List<SysUserVO> result = new ArrayList<>(list.size());
        for (SysUser user : list) {
            result.add(toVO(user));
        }
        return result;
    }
}
