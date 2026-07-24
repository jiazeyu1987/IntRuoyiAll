# 任务：展厅版本中心实现

## 任务目标

- 按已放行设计文档实现展厅公司/产品版本中心。
- 实现范围覆盖：
  - `showroom_version_bundle` 数据模型与回填
  - `history / detail / republish` 后端链路
  - 与 `showroom release` 的一步到位发布集成
- 本任务由主 reviewer 负责放行，多个子 agent 并行开发；只有完全符合设计文档、BDD/TDD 证据完整、通过 reviewer 复审的改动才可合入。

## 非目标

- 不改动 `Website` 前台应用代码。
- 不引入 fallback、mock 成功、静默降级或兼容分支。
- 不重做现有审批体系，只复用既有权限/发布门槛。

## 前序任务检查

- 已检查设计任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-version-center-design-docs\task.md`
- 该任务状态为 `已完成`
- 设计文档已由 reviewer 复审放行，不阻塞本次实现启动

## 里程碑

- [x] M1：建立后端实现任务包与执行日志。
- [x] M2：完成 schema/backfill RED 测试与实现。
- [x] M3：完成 backend APIs/services RED 测试与实现。
- [x] M4：完成后端联调、真实 release 链路验证与 reviewer 复审。

## 预期验证

- 后端任务必须保留以下证据：
  - `BDD: <scenario> -> Given/When/Then`
  - `RED: <command> -> FAIL, <expected reason>`
  - `GREEN: <command> -> PASS`
  - `REGRESSION: <command> -> PASS`
- 预期至少覆盖：
  - bundle 回填成功 / 阻断
  - history/detail 合同
  - republish 复制历史版本并生成新 release
  - global release blocker 显式失败
  - 公司 snapshot 缺失阻断

## 当前状态

- 状态：已完成
- 已完成：
  - 已在独立 worktree `task/20260523-showroom-version-center-impl` 中启动实现
  - 已明确主 reviewer + 并行 worker 的执行模式
  - 已完成 `showroom_version_bundle` schema/DO/mapper/service 与公司 revision snapshot 字段持久化
  - 已完成独立 `ShowroomVersionCenterController`，接通 `history/detail/republish` 三条后端链路
  - 已完成 reviewer 预审要求的 4 个高风险修复：
    - detail 权限真实覆写
    - history published/bundle 一致性 fail-fast
    - republish staged global release blocker 判定
    - 公司历史 preview alt 使用 snapshot
  - 已完成 focused test 闭环：foundation/schema/content/version-center/controller 共 30 项测试通过
  - 已完成 backfill 合同执行验证：`ShowroomVersionCenterBackfillContractTest` -> PASS
  - 已通过主 reviewer 复审，当前实现满足设计文档要求
- 待完成：
  - 无
- 阻塞与影响：
  - 标准 Maven test 路径仍被与本任务无关的 `ShowroomHttpApiIntegrationTest` 重复测试方法阻断；当前已用 focused manual JUnit 路径完成真实回归，但在该重复方法清理前，`mvn ... test` 无法作为最终统一命令。

## Cleanup Keep

- doc/tasks/20260523-showroom-version-center-implementation/backend-api-evidence.md
- doc/tasks/20260523-showroom-version-center-implementation/database-schema-evidence.md
