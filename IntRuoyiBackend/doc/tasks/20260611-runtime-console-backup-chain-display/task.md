# 20260611-runtime-console-backup-chain-display

## 任务目标

实现阶段 5：运行控制台展示备份恢复链关键信息。备份点接口必须暴露全量/增量、对象新增/修改/删除/复用、DCC 链状态、演练状态和不可恢复原因，供前端运行控制台直接展示，避免运维只能打开 manifest 判断备份是否可恢复。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。manifest 缺字段时明确进入不可恢复原因，不用默认成功值掩盖。
- 是否从根因和长期维护角度解决：是。后端 VO 直接表达展示所需契约，前端只负责呈现，不解析备份文件。
- 是否存在临时补丁或绕过：否。本阶段只修改本地前后端代码和测试，不访问正式服务器。

## BDD 场景

- BDD: 备份点展示全量增量与链状态 -> Given 备份点包含 DCC manifest / When 运行控制台加载备份点列表 / Then 每个备份点展示 DCC 备份模式和 chainStatus。
- BDD: 备份点展示对象变化数量 -> Given manifest 包含 objectDeltaStats / When 用户查看备份策略表格 / Then 新增、修改、删除、复用数量直接显示。
- BDD: 备份点展示演练状态 -> Given manifest.validation 包含 rehearsalStatus / When 用户查看备份点 / Then 显示演练状态和最近验证时间。
- BDD: 不可恢复原因可见 -> Given 备份点缺 manifest、checksum 或链状态不完整 / When 用户查看运行控制台 / Then 不可恢复原因直接出现在表格提示中。

## 里程碑

- [x] M1：后端补 VO 字段和服务映射测试。
- [x] M2：前端补 API 类型和运行控制台表格展示。
- [x] M3：运行后端/前端验证，更新长期经验。

## 预期验证

- `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest" test`
- `pnpm ts:check`
- `git diff --check`

## 当前状态

completed

## Verification Result

- `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest" test` -> PASS，6 tests。
- 前端 `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- 阶段经验已写入 root worktree `docs/release-backup-restore.md`。
