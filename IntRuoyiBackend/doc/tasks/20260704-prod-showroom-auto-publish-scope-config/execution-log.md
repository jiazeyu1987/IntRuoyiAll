# 执行日志

## BDD / TDD
- BDD: 自动发布 scope 配置缺失时应失败 -> Given 正式服存在待发布状态且未配置自动发布 siteKey/stage, When 定时任务执行, Then 记录明确错误并停止自动发布。
- BDD: 自动发布 scope 配置完整时应使用真实公开站点发布 -> Given 正式服配置了 siteKey 和 stage 且公开站点绑定存在, When 定时任务执行, Then 不再出现缺 scope 错误并按配置处理 dirty release。
- RED: production-log-existing -> FAIL, 正式服日志已有 `Dirty showroom release requires configured auto-publish site key and stage`。

## 经验门禁
- GREEN: experience-preflight -> PASS, 已按项目门禁读取 PowerShell 编码要求、正式服访问与发布备份约束；本次只允许最小配置修复。

## 定位证据
- 代码定位：`ShowroomReleaseAutoPublishScheduler` 读取 `showroom.release.auto-publish.site-key` 与 `showroom.release.auto-publish.stage` 后调用自动发布服务。
- 正式服配置修复前：`infra_config` 只有 `showroom.release.auto-publish.state`，没有 `site-key` 与 `stage`。
- 正式服公开站点绑定：`showroom_public_site_binding` 存在 `tenant_id=1, site_key=yingtai-showroom, stage=TEST, enabled=1, deleted=0`。
- 正式服日志：20:56 至 21:24 每分钟出现 `Dirty showroom release requires configured auto-publish site key and stage`。

## GREEN / REGRESSION
- GREEN: prod-config-write -> PASS, 写入 `showroom.release.auto-publish.site-key=yingtai-showroom` 与 `showroom.release.auto-publish.stage=TEST`，写入前校验绑定唯一且启用。
- GREEN: prod-config-readback -> PASS, `infra_config` 读回两条 scope 配置和原 state 配置。
- GREEN: prod-scheduler-next-cycle -> PASS, 写入后等待 75 秒，调度日志未再出现缺少 auto-publish scope 配置错误。
- GREEN: prod-log-regression -> PASS, 最终验证最近 3 分钟 `Dirty showroom release requires configured auto-publish site key and stage` 计数为 0。

## 清理
- REGRESSION: task-closeout-preview -> PASS, preview 仅保留 task.md 与 execution-log.md，无删除项。
- REGRESSION: task-closeout-apply-initial -> BLOCKED, 收尾脚本要求 task.md 以 completed 开头，已补正任务文档状态格式。