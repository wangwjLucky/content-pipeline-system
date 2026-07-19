package com.pipeline.admin.service;

import com.pipeline.admin.common.EncryptionUtil;
import com.pipeline.admin.entity.PlatformAccount;
import com.pipeline.admin.mapper.PlatformAccountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlatformAccountServiceImpl implements PlatformAccountService {
    private final PlatformAccountMapper platformAccountMapper;

    @Override
    public Page<PlatformAccount> list(int page, int size, String platform) {
        LambdaQueryWrapper<PlatformAccount> q = new LambdaQueryWrapper<PlatformAccount>()
                .eq(platform != null && !platform.isEmpty(), PlatformAccount::getPlatform, platform)
                .orderByDesc(PlatformAccount::getCreatedAt);
        return platformAccountMapper.selectPage(new Page<>(page, size), q);
    }

    @Override
    public PlatformAccount get(Long id) {
        return platformAccountMapper.selectById(id);
    }

    @Override
    public PlatformAccount create(PlatformAccount account) {
        account.setStatus("ENABLED");
        // 加密存储 Cookies
        if (account.getCookiesEncrypted() != null && !account.getCookiesEncrypted().isEmpty()) {
            account.setCookiesEncrypted(EncryptionUtil.encrypt(account.getCookiesEncrypted()));
        }
        platformAccountMapper.insert(account);
        return account;
    }

    @Override
    public PlatformAccount update(Long id, PlatformAccount account) {
        account.setId(id);
        platformAccountMapper.updateById(account);
        return platformAccountMapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        platformAccountMapper.deleteById(id);
    }
}