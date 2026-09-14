# ERP SimPas 配置 Git 可恢复快照

## Task Goal

将本机 ERP SimPas 正式账套配置保存为可提交 Git 的加密快照，并提供仅本机可用的恢复脚本，防止后续自动化修改配置后无法恢复。

## Milestones

- [x] 核对本机配置存储和加密前置条件
- [x] 创建加密快照与恢复脚本
- [x] 验证快照不包含明文凭据且可恢复
- [ ] 收尾

## Expected Verification

- Git 快照不包含 ERP 密码或 SimPas 签名明文。
- 快照可在当前 Windows 用户和本机环境中恢复 ERP 连接配置。
- 不引入测试账套、默认凭据或静默降级。

## Current Status

completed

已生成 Git 可追踪的 AES-256-GCM 加密快照；本机恢复脚本已通过原位恢复和记录指纹验证。密钥仅保存在当前 Windows 用户的本地应用数据目录，不进入工作区或 Git。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；使用可版本化的加密快照和明确恢复流程。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- `docs/login-access.md#ERP 金蝶账套登录连通性门禁`：正式连接必须由当前保存配置解析，缺字段必须透明失败。
- `docs/login-access.md#ERP 外部助手短期票据授权门禁`：SimPas 签名数据不得当作应用密钥，不得写入全局回退配置。
