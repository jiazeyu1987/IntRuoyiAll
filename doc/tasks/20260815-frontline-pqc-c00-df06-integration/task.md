# C00 / DF06 正式合同恢复与集成

## Task Goal

在最新 `int_main` 基线上完成以下剩余工作：

1. C00 历史回填只接受批准的活跃订单 QA 快照清单，不允许从 PQC 任务反推订单快照。
2. DF06 创建活跃订单时正式锁定 DCC 项目、QA 规程和 QA 发布版本，并原子生成四类 PQC 任务。
3. 复验 INT12 冻结测试并执行 VAL13 系统验收。
4. 验证通过后快进合并到 `int_main`，保留相关 worktree。

## Milestones

- [x] M0：建立最新 `int_main` 隔离集成 worktree。
- [x] M1：固化权威合同快照并建立 C00/DF06 RED。
- [x] M2：修复 C00 正式清单回填。
- [x] M3：恢复 DF06 活跃订单 QA 锁定与任务生成。
- [x] M4：通过 C00、DF06、INT12 后端与前端回归。
- [x] M5：通过独立验证和 VAL13。
- [ ] M6：快进合并 `int_main`，保留 worktree。

## Expected Verification

- `MesQaPqcSchemaTest`
- `MesTeamLeaderActiveOrderServiceTest`
- C00/DF06 相关后端回归
- INT12 七类冻结测试
- VAL13 十七类后端验收
- 一线 PQC 前端静态合同与 TypeScript 检查
- C00 MySQL 8 正式清单正反向夹具
- evidence validators、UTF-8、冲突标记、`git diff --check`
- `scripts/preflight/branch-runtime-port-guard.ps1`

## 适用经验门禁

- 一线 PQC 必须以 `activeOrderId` 和订单锁定的 DCC/QA 版本为正式身份；QA 工序不得映射或校验 MES 路线工序。
- C00 历史订单快照只能来自批准清单；唯一任务版本只能用于一致性校验，不能成为回填来源。
- Worktree 验证必须使用隔离 Maven 输出和已登记端口槽位；不得占用 `48081`。
- Maven `-D` 参数必须逐项加引号，测试失败不得被后续命令覆盖。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；修复正式数据来源和订单创建事务边界。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

实现、定向回归、VAL13 后端聚合、前端静态合同、TypeScript、evidence validators、UTF-8、冲突标记、`git diff --check` 和分支端口门禁均已通过。真实写入 Playwright 路径按用户 2026-08-15 明确指令豁免，不记为 PASS。剩余：独立验收记录、提交和快进合并；保留 worktree。

## Cleanup Keep

- doc/tasks/20260815-frontline-pqc-c00-df06-integration/independent-test-report.md
