# 预览重排工艺路线工序身份不唯一修复

## Task Goal
修复预览重排时报错 `工艺路线工序身份不唯一，routeId=null，sourceProcessId=922894，processCode=Z2630，candidateRouteProcessIds=[900394, 922894]` 的问题，确保排产重排链路使用稳定路线工序身份，不用旧 `process_id` 或缺失路线口径造成误判。

## Current Status
completed

## Milestones
1. 复现并固定缺陷场景：补充 BDD 场景和 RED 回归测试。
2. 根因定位：确认 `routeId=null` 与 `sourceProcessId`/`routeProcessId` 口径错位的具体调用链。
3. 最小修复：在不引入 fallback、降级或吞异常的前提下固化路线工序身份解析。
4. 回归验证：执行目标 Java 测试、相关排产身份契约测试和必要静态检查。
5. 收尾准备：记录证据、风险和后续合并条件。

## Milestone Status
1. completed：已用 `getProcessIdentityMap_shouldPreserveExplicitTargetsWhenTargetCodesDuplicate` 复现用户报错。
2. completed：根因是同一排产预览上下文内两个显式目标工序共用 `Z2630` 编码时，被旧映射口径提前判成歧义。
3. completed：显式目标工序保留自身身份；只有同编码外部别名无法唯一映射时继续 fail fast。
4. completed：定向 Java 回归、完整路线工序服务回归、排产身份 Python 契约均通过。
5. completed：已提交、合入 `int_main`、完成合并结果复验并进入 worktree 清理。

## Closeout
- 实现提交：`43e35be9dd 任务: 修复预览重排路线工序身份歧义`。
- 验证报告提交：`6ead6de84e 任务: 补充预览重排身份验证报告`。
- 合并提交：`ef19a2d1f3 融合: 预览重排路线工序身份歧义修复`。
- 合并后验证：排产身份 Python 契约 5 passed；`MesProRouteProcessServiceImplTest` 11 tests PASS。

## Expected Verification
- 定向 Maven 回归测试覆盖用户报错场景。
- 原有路线工序身份解析测试保持通过。
- 排产身份契约脚本保持通过。
- 如进入真实 E2E，必须先完成 experience-preflight 并使用测试租户真实路径。

## 经验门禁
- PowerShell/中文文本：所有读写显式 UTF-8，禁止默认 `Get-Content`/`Set-Content` 处理中文。
- worktree：本任务在独立 worktree `replan_identity_20260711` 中开发；合并前检查主工作区脏改重叠，合并后在 `int_main` 复验。
- MES 旧工序 ID 与路线工序身份：排产重排、路线工序配置、报工联动必须以稳定 `route_process_id` / 路线版本 / 工序编码解析；禁止把可变 `process_id` 当唯一事实源。
- 缺失或歧义映射：必须 fail fast 并输出明确 blocker；禁止批量回写历史快照、默认成功、跳过校验或按产品/备注兜底匹配工序。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是修正排产重排预览链路的路线工序身份输入与解析口径。
- 是否存在临时补丁或绕过：否。
