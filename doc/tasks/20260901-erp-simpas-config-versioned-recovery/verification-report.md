# Verification Report

## Conclusion

PASS。ERP SimPas 配置已具备 Git 版本化恢复能力：仓库保存 AES-256-GCM 加密快照，当前 Windows 用户本机保存独立密钥，恢复脚本仅影响三条 ERP 配置记录并已完成原位恢复验证。

## Verification

- `backup-erp-kingdee-simpas.ps1 -Mode Backup` -> PASS。
- `backup-erp-kingdee-simpas.ps1 -Mode Verify` -> PASS。
- `backup-erp-kingdee-simpas.ps1 -Mode Restore` -> PASS，恢复后指纹一致。
- 快照 JSON 只包含加密信封字段，不包含明文业务字段。

## Recovery Scope

- 仅恢复本机 Docker 数据库中的正式 ERP 保存配置、当前连接选择和共享同步配置。
- 不操作测试服务器、正式服务器或其他系统配置。
- 若金蝶 SimPas 因其时间戳规则拒绝历史签名，仍需按金蝶规则生成新签名；快照负责恢复本机系统保存值。
