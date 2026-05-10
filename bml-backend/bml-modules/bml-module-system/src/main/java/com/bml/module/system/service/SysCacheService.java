package com.bml.module.system.service;

import com.bml.module.system.vo.SysCacheVO;

import java.util.List;

public interface SysCacheService {

    SysCacheVO overview(String pattern);

    List<String> keys(String pattern);

    boolean deleteKey(String key);

    boolean clearPrefix(String prefix);
}
