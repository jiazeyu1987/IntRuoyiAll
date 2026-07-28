# Task: eDHR 详情页辅助模式预览 Switch

## Task Goal

在 eDHR 批次详情页右侧栏顶部增加“辅助模式”Switch，只控制中间预览区在“原表模式”和“辅助模式”之间切换；无辅助配置时 Switch 保留但禁用，不改变右侧卡片打开/查看动作和填写页既有行为。

## Milestones

1. [x] 建立任务记录、经验门禁和 BDD/TDD 证据。
2. [x] 新增前端静态 RED 合同，锁定右侧 Switch、只读辅助预览和不改打开载体。
3. [x] 新增后端 RED 回归，证明未打开主生产表预览需要正式 `executionSnapshotJson.assistRows`。
4. [x] 实施后端预览快照增强，不新增接口、不混用动态表单来源。
5. [x] 实施前端 Switch 与只读辅助预览。
6. [x] 运行目标 GREEN、相邻回归和证据校验。
7. [x] 收尾清理检查并记录剩余阻塞。
8. [x] 解决收尾 blocker：填写页辅助模式填写人来源与全量前端类型检查。
9. [x] 修复无辅助配置时右侧 Switch 禁用提示文字被挤压/截断的问题。

## Expected Verification

- `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js`
- `node tests/e2e/edhr-dynamic-form-card-preview-static.spec.js`
- `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js`
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js`
- `node tests/e2e/edhr-loss-form-open-action-static.spec.js`
- `node src/test/js/mes-edhr-assist-filler-switch-snapshot-static.spec.cjs`
- `pnpm ts:check`
- `pnpm build:local`
- `mvn -o -pl yudao-module-mes -am "-DskipTests" compile`
- `mvn -o -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#previewTask_returnsUnopenedBatchRecordWithExecutionSnapshotAssistRows" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -o -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 经验门禁

### eDHR 路线表单跳过口径门禁

- Trigger: eDHR 批次详情右侧路线表单卡片、动态表单卡片、`/task/preview`、只读查看和辅助预览。
- Preflight check: 主生产表预览继续走正式批记录报表来源；动态表单预览继续走 FormCenter 上下文，不得互相替代。
- Blocker: 若 Switch 改变右侧卡片打开动作、写入动作或把动态表单当批记录报表处理，必须停止。
- Verification: 前端静态合同同时断言 Switch 只影响中间预览，后端测试断言主生产表预览快照包含辅助行。
- Forbidden action: 禁止吞错误、默认成功、API-only 替代页面路径或把必填表单改成可跳过。
- Evidence: `docs/e2e-rules.md#eDHR 路线表单跳过口径门禁`。

### 前端静态契约隔离门禁

- Trigger: 当前任务需要 RED/GREEN 静态合同且仓库存在大量历史或并行改动。
- Preflight check: 新增聚焦静态合同覆盖当前 Switch 行为，避免修改无关大合同。
- Blocker: 无法稳定 RED/GREEN 时不得宣称完成。
- Verification: `execution-log.md` 记录目标 RED/GREEN 和相邻回归。
- Forbidden action: 禁止修改无关合同绕过历史失败。
- Evidence: `docs/frontend-development.md#前端静态契约隔离门禁`。

### PowerShell 分号串联测试退出码门禁

- Trigger: 需要批量运行多个 Node 静态合同或其它测试命令。
- Preflight check: 验收命令必须逐条运行，或显式检查 `$LASTEXITCODE`，避免中间失败被最后一条 PASS 掩盖。
- Blocker: 中间命令输出断言失败但最终退出码为 0 时，必须单独复跑并记录真实失败。
- Verification: 本任务已单独复跑四条相邻静态合同并记录 `edhr-assist-fill-mode-static.spec.js` 的非本任务失败。
- Forbidden action: 禁止把分号串联命令的最终 0 退出码当作全部通过。
- Evidence: `docs/powershell-memory.md#powershell-分号串联测试退出码门禁`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，预览数据从正式 runtime snapshot 来源生成，前端只做只读展示。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

## Cleanup Keep

- doc/tasks/20260728-edhr-detail-assist-preview-switch/frontend-feature-evidence.md
- doc/tasks/20260728-edhr-detail-assist-preview-switch/backend-api-evidence.md
- doc/tasks/20260728-edhr-detail-assist-preview-switch/bug-regression-evidence.md
