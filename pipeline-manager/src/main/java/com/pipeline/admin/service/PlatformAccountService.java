package com.pipeline.admin.service;

import com.pipeline.admin.entity.PlatformAccount;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface PlatformAccountService {

    Page<PlatformAccount> list(int page, int size, String platform);

    PlatformAccount get(Long id);

    PlatformAccount create(PlatformAccount account);

    PlatformAccount update(Long id, PlatformAccount account);

    void delete(Long id);
}