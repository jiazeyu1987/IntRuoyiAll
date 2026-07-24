# 预览重排同编码快照工序别名归属修复

## Task Goal
修复预览重排时报错 `工艺路线工序身份不唯一，routeId=null，sourceProcessId=900400，processCode=Z3710，candidateRouteProcessIds=[922864, 922895]`，确保同编码中“已删除快照目标 + 唯一当前有效目标”可以明确归属，不能再被误判为不可解析歧义。

## Current Status
completed

## Milestones
1. 复现并固定 Z3710 同编码快照场景。
2. 最小修复身份映射规则。
3. 执行定向与相关回归验证。
4. 重启本机后端并确认真实运行态不再抛出同类错误。
5. 记录证据并提交任务变更。

## Milestone Status
1. completed：已定位运行日志在 `getProcessIdentityMap`，目标工序同时包含已删除快照 `922864` 与当前有效 `922895`，外部旧工序 `900400` 被误判为歧义；RED 定向测试已复现。
2. completed：按同编码目标中的唯一 `deleted=false` 目标归属外部别名，显式目标仍保持自身身份。
3. completed：定向 GREEN、路线工序服务全量回归和排产身份契约均通过。
4. completed：本机后端已重启到 `backend-runtime-control-20260711-202016.jar`，`48081` 健康检查 `UP`，运行 jar 内 `MesProRouteProcessServiceImpl.class` 与本地编译 class 哈希一致，启动日志未出现新增 `900400/Z3710/[922864,922895]` 歧义。
5. completed：实现变更已提交为 `2de996340c`；`task-closeout-cleanup` preview/apply 均通过，当前仓库为主 worktree，无需融合或删除 worktree，未删除任何文件；收尾记录将单独提交。

## Closeout Evidence
- implementation commit：`2de996340c 任务: 修复预览重排快照工序别名归属`
- cleanup preview：PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`，无删除项、无阻塞、无告警。
- cleanup apply：PASS，`linked=False current_branch=int_main main_branch=int_main`，无删除项、无阻塞、无告警。

## Expected Verification
- RED：新增定向回归先失败，复现 `900400/Z3710/[922864,922895]` 归属问题。
- GREEN：定向回归通过。
- REGRESSION：`MesProRouteProcessServiceImplTest` 全量通过。
- RUNTIME：本机 `48081` 重启后健康检查 UP，运行日志无新增同类 `Z3710` 歧义。

## 经验门禁
- PowerShell/中文文本：所有命令显式 UTF-8；不使用 `&&`。
- MES 路线工序身份：显式目标工序必须保留自身身份；别名只允许在唯一正式目标可判定时归属，否则继续 fail fast。
- 禁止 fallback：不按产品、备注、顺序或默认成功兜底；只使用 `deleted` 状态与工序编码形成正式身份规则。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，明确同编码快照目标与当前目标并存时的别名归属规则。
- 是否存在临时补丁或绕过：否。
