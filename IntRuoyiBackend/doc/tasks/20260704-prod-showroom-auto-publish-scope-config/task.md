completed

# 正式服展厅自动发布 scope 配置修复

## 任务目标
- 分析并修复正式服定时自动发布报错：`Dirty showroom release requires configured auto-publish site key and stage`。
- 只覆盖正式服展厅自动发布 scope 配置，不改业务发布逻辑，不引入 fallback。

## 里程碑
1. 建立任务记录并读取经验门禁。- 已完成
2. 定位自动发布 siteKey/stage 配置来源与正式服当前值。- 已完成
3. 受控修复正式服缺失/异常配置并验证调度日志。- 已完成
4. 记录验证证据、清理任务产物并提交本任务改动。- 进行中

## 预期验证
- 正式服 `infra_config` 中存在自动发布所需的 `showroom.release.auto-publish.site-key` 与 `showroom.release.auto-publish.stage`。
- 配置值与正式服公开站点绑定一致。
- 下一轮定时任务不再出现缺少自动发布 scope 的错误。

## 经验门禁
- PowerShell：所有中文读写使用 UTF-8；不使用 `&&`；远程多行脚本用 UTF-8 base64 传递。
- 正式服：按 `docs/server-access.md` 使用正式服固定入口；只做本次授权的排障与最小配置修复。
- 发布/回滚：按 `docs/release-backup-restore.md`，涉及发布链路先核配置、保留可追溯证据，不碰 `/mnt/nas` 共享盘。
- 缺失前置条件：公开站点绑定、配置 schema 或权限不满足时 fail fast，不用 fallback 或静默跳过。

## 根因
- `ShowroomReleaseAutoPublishScheduler` 在 dirty release 状态下需要读取 `showroom.release.auto-publish.site-key` 和 `showroom.release.auto-publish.stage`。
- 正式服 `infra_config` 只有 `showroom.release.auto-publish.state`，缺少 `site-key` 与 `stage` 两个 scope 配置。
- 正式服存在且启用了公开站点绑定：`yingtai-showroom / TEST`，因此调度器每分钟因缺 scope 配置失败。

## 修复内容
- 在正式服 `infra_config` 中补齐：
  - `showroom.release.auto-publish.site-key = yingtai-showroom`
  - `showroom.release.auto-publish.stage = TEST`
- 写入前校验正式服 `showroom_public_site_binding` 中 `yingtai-showroom / TEST` 存在且启用。
- 未修改发布业务代码，未修改 `/mnt/nas`，未引入 fallback。

## 验证结果
- `infra_config` 已读回 `site-key=yingtai-showroom`、`stage=TEST`。
- 公开站点绑定读回：`tenant_id=1, site_key=yingtai-showroom, stage=TEST, enabled=1`。
- 写入后等待下一轮调度，最近 3 分钟 `Dirty showroom release requires configured auto-publish site key and stage` 计数为 0。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，修复调度所需的正式 scope 配置，保留缺配置即失败的强约束。
- 是否存在临时补丁或绕过：否。

## Cleanup Keep
- doc/tasks/20260704-prod-showroom-auto-publish-scope-config/task.md
- doc/tasks/20260704-prod-showroom-auto-publish-scope-config/execution-log.md

## 当前状态
- 状态：已完成。
- 最终结果：正式服自动发布 scope 配置已补齐，调度不再报缺配置错误。