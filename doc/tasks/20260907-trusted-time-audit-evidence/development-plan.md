# 开发计划：可信时间最小闭环

### 里程碑 1：签名时间边界修复

目标：正式签名展示时间固定使用服务器 `signedAt`，用户选择时间仅作为业务发生时间。

涉及文件：

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionSignatureService.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionSignatureServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java`
- `IntRuoyiFronted/src/views/mes/pro/edhr/ApprovalDetailPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr/SignaturePage.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr/signatureTime.ts`

交付物：

- 服务端签名展示时间修复及回归测试。
- 用户可见“业务发生时间”文案与正式签名展示读取修复。

### Implementation Steps

1. 在 `MesProBatchRecordExecutionSignatureServiceTest` 增加 RED：选择业务时间时，正式展示时间仍应等于服务器时间。
2. 修改 `buildSignatureTimeEvidence`，固定 `signatureDisplayAt = signedAt`。
3. 检查批记录详情、归档和 PDF 标签，将选择时间明确显示为“业务发生时间”。
4. 不修改历史值、数据库 schema 或历史哈希。

### Acceptance

- AC-01。

### Verification Gates

- MES 定向测试 GREEN。
- `git diff --check` 通过。

### 里程碑 2：时间巡检与证据导出

目标：复用 Runtime Control 保存正式服、审查服时间检查，并从指定巡检生成固定三文件 ZIP。

涉及文件：

- `IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/`
- `IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/controller/admin/runtimecontrol/`
- `IntRuoyiBackend/yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/`
- `IntRuoyiBackend/script/deploy/`
- `doc/tasks/20260907-trusted-time-audit-evidence/backend-api-evidence.md`

交付物：

- 时间状态解析与巡检必检项。
- 指定巡检报告的时间戳证据 ZIP 下载接口。

### Implementation Steps

1. 在 infra Runtime Control 中增加最小 chrony 输出解析和远程时间检查。
2. 复用现有远程执行配置，读取 `chronyc tracking`、`chronyc sources`、`timedatectl`、`date -u` 和 MySQL 时间。
3. 把正式服、审查服结果加入 `RuntimeOpsInspectionServiceImpl` 必检项并保存到 `inspection-runs.json`。
4. 使用 JDK `ZipOutputStream`、`MessageDigest` 导出指定巡检 ID 的 HTML、JSON 和 SHA-256 清单。
5. 新增 `GET /infra/runtime-control/inspection-runs/{id}/time-evidence.zip`。

### Acceptance

- AC-02、AC-03、AC-05、AC-06。

### Verification Gates

- chrony 正常、缺失、无选中源、偏差超限和远端不可达测试 GREEN。
- ZIP 三文件和哈希合同测试 GREEN。

### 里程碑 3：运行控制台最小 UI

目标：在现有运行控制台展示时间检查，并允许导出当前已保存巡检。

涉及文件：

- `IntRuoyiFronted/src/api/infra/runtimeControl/index.ts`
- `IntRuoyiFronted/src/views/infra/runtime-control/index.vue`
- `IntRuoyiFronted/tests/e2e/runtime-control-trusted-time-static.spec.js`
- `doc/tasks/20260907-trusted-time-audit-evidence/frontend-feature-evidence.md`

交付物：

- 时间检查展示。
- “导出时间戳证据”按钮及失败提示。

### Implementation Steps

1. 扩展现有 Runtime Control API 类型和页面巡检区域。
2. 显示环境、节点、时间源、偏差、Leap、检查时间和状态。
3. 增加“导出时间戳证据”按钮，只导出当前已保存巡检 ID。
4. 无巡检 ID 时禁用；下载失败显示真实错误。

### Acceptance

- AC-04。

### Verification Gates

- 前端静态测试和类型检查 GREEN。

### 里程碑 4：回归与环境闭环

目标：完成任务定向回归与端口门禁，并在获得授权后完成真实环境验证。

涉及文件：

- `IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/framework/runtimecontrol/config/RuntimeControlProperties.java`
- `IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeControlOperationAction.java`
- `IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeRemoteRootDiskServiceImpl.java`
- `IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/controller/admin/runtimecontrol/vo/RuntimeControlRemoteRootCleanupReqVO.java`
- `IntRuoyiBackend/yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/`
- `IntRuoyiFronted/src/views/infra/runtime-control/components/shared.ts`
- `IntRuoyiFronted/src/views/infra/runtime-control/index.vue`
- `IntRuoyiFronted/tests/e2e/runtime-control-trusted-time-static.spec.js`
- `doc/tasks/20260907-trusted-time-audit-evidence/execution-log.md`
- `doc/tasks/20260907-trusted-time-audit-evidence/test-report.md`

交付物：

- 定向回归与分支门禁证据。
- 经授权的真实环境/E2E证据或明确未授权边界。

### Implementation Steps

1. 复跑 MES、infra 和前端定向测试。
2. 运行 branch runtime port guard。
3. 获得授权后才配置/验证正式服、审查服 chrony。
4. 获得明确 E2E 授权后才使用 Playwright 走真实页面巡检和下载。
5. 运行控制台面向用户的环境、动作和错误文案统一显示“审查服”；保留 `backup`、`BackupServerHost`、`promote-backup` 等技术接口名。
6. 最近操作保留服务端历史审计原值，但环境、动作、原因和摘要在展示层统一旧称，禁止改写历史记录。

### Acceptance

- AC-01 至 AC-06。

### Verification Gates

- 本地定向回归通过。
- 后端合同测试及真实页面均不得显示旧称 `Backup`、`备份服` 或 `备用服务器`。
- 远程与 E2E 未获授权时明确记录为未执行，不能冒充完整环境闭环。

## Rollback or Stop Conditions

- chrony 输出契约不明确、远程执行配置缺失、阈值缺失或目标 worktree 发生非任务改动时停止。
- 不得添加 mock、客户端时间 fallback、默认 PASS 或第二套证据存储。
