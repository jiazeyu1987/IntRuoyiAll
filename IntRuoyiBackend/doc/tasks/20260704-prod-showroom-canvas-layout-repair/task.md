# 正式服展柜画布布局缺失修复

## 任务目标

修复正式服 `yingtai-showroom/TEST` 发布 scope 下手动发布展厅失败的问题。初始 blocker 是展柜展项映射缺少 `layout_x/layout_y/layout_width/layout_height`；修复后发布链路继续暴露展柜语音、展柜预览图、奖项描述等严格发布前置缺口，本任务按真实链路逐层补齐并完成发布验证。

## Current Status

completed

## 里程碑

1. 确认正式服发布 scope 对应租户、缺失布局范围和修复 SQL 预览。
2. 为目标租户缺失布局的 `showroom_hall_item` 生成完整、不重叠、覆盖 100% 画布的默认布局。
3. 执行受控 SQL 更新并只读回查，禁止跨租户误改。
4. 逐层验证并修复发布链路继续暴露的展柜语音、展柜预览图、奖项描述缺口。
5. 验证手动发布展厅接口完成 release 生成。
6. 记录证据并提交本次任务文档。

## 预期验证

- 只更新 `yingtai-showroom/TEST` 绑定租户范围内缺失布局的展项映射。
- 更新后目标租户 `showroom_hall_item` 缺失 layout 计数为 0。
- 布局按展柜独立覆盖完整画布，矩形不重叠。
- 手动发布接口不再因 `hall canvas layout is required` 失败。
- 正式服真实前端路径点击“手动发布展厅”返回 `code=0`，生成 release。

## 经验门禁

- `docs/powershell-memory.md`：涉及 PowerShell、SSH、SQL 文本传递时使用 UTF-8 文件/Base64 或远端脚本，不用 PowerShell 管道直接传中文/SQL。
- `docs/server-access.md`：正式服写入已由用户“继续”授权在本任务范围内执行；必须先确认目标主机、目标库、目标租户和 SQL 作用范围。
- `docs/release-backup-restore.md`：正式服写入和发布链路验证必须记录环境、版本和回查证据；不得用健康检查代替业务验证。
- `docs/login-access.md`：若走真实前端 E2E，先跑标准登录前置；本次优先使用后端接口/日志作发布结果核验，避免重复页面误操作。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；补齐 release 严格要求的数据前置，而不是绕开校验。
- 是否存在临时补丁或绕过：否。

## 修复结果

- `showroom_hall_item`：目标租户展柜布局按 Java 默认布局语义重算，目标展柜面积覆盖为 `1.000000`。
- `infra_config`：同步正式服阿里云 NLS TTS `access-token/appkey/voice` 到本机已验证配置，解决旧 token 被阿里云拒绝的问题。
- 展柜语音：通过正式服业务接口为 10 个展柜生成并发布中英文语音。
- 展柜预览图：通过正式服业务接口为 `hall_09`、`hall_10` 发布 preview asset，补齐 release 所需 live preview。
- 奖项描述：仅对 `yingtai-showroom/TEST` 绑定租户当前奖项版本，将已发布中英文奖项讲解稿回填到空缺的 `description_zh/description_en`，46 条更新后剩余缺失为 0。
- 最终验证：正式服真实页面点击“手动发布展厅”成功，releaseId `20260704T132351Z-be276b74dfa8-856a86f095c1`。

## Cleanup Candidates

- `doc/tasks/20260704-prod-showroom-canvas-layout-repair/prod-manual-publish-verify.mjs`
- `doc/tasks/20260704-prod-showroom-canvas-layout-repair/prod-manual-publish-result.json`
- `doc/tasks/20260704-prod-showroom-canvas-layout-repair/prod-manual-publish-after.png`
