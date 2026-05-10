package com.bml.module.system.service;

import com.bml.module.system.vo.SysOnlineUserVO;

import java.util.List;

public interface SysOnlineUserService {

    List<SysOnlineUserVO> listOnlineUsers(String username);

    boolean forceLogout(String userKey);
}
