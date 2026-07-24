# 阶段二：迁移体验增强前端设计

## 子任务边界

- 仅在 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_version\yudao-ui-admin-vue3` 内调研和设计。
- 不修改当前“导入 Word”运行接口，不把阶段二状态硬塞进阶段一尚未完成的页面。
- 不使用 `page.route`、mock 响应、`.skip`、默认成功或接口绕过登录。

## 现有入口

- 页面：`src/views/mes/pro/batchrecordtemplate/index.vue`
- API：`src/api/mes/pro/batchrecordreport/index.ts`
- 当前导入调用：`BatchRecordReportApi.recognizeUploadedRoute(file, routeKey, batchRecordName, upgrade, productNames)`
- 现有 eDHR 页面可复用样式：
  - `src/views/mes/pro/edhr-init-batch/InitBatchPage.vue`
  - `src/views/mes/pro/edhr-release/ReleasePage.vue`
  - `src/views/mes/pro/edhr-unified-change/UnifiedChangePage.vue`

## 推荐新增前端结构

- `src/api/mes/pro/batchrecordversion/index.ts`
  - `getMigrationDiff(versionId)`
  - `confirmMigrationItems(versionId, data)`
  - `reuploadDraft(versionId, file, data)`
- `src/views/mes/pro/batchrecordtemplate/components/MigrationDiffDrawer.vue`
  - 展示结构化 diff、迁移证据和阻断项。
- `src/views/mes/pro/batchrecordtemplate/components/MigrationConfirmDialog.vue`
  - 只处理 `CONFIRM_REQUIRED` 授权确认。
- `src/views/mes/pro/batchrecordtemplate/components/DraftReuploadDialog.vue`
  - 仅草稿、预检失败、驳回状态可见。

## 页面状态要求

- `BLOCKER`：
  - 显示红色阻断标签、后端阻断原因、修复建议。
  - 禁止展示“确认继续”或任何绕过入口。
- `CONFIRM_REQUIRED`：
  - 显示黄色确认标签、匹配证据、确认按钮。
  - 确认必须要求填写意见。
  - 确认后刷新 diff，并展示确认人、确认时间。
- `INFO`：
  - 只展示差异和证据，不要求操作。
- 后端错误：
  - 页面必须以 `el-alert` 或消息弹窗暴露真实错误。
  - 不允许空 `catch {}`、静默 toast 缺失或默认成功。

## 真实 E2E 路径设计

- 写入验证均使用本地测试租户 `测试租户/aoteman`。
- 最终只读复验使用本机 `芋道源码/admin`。
- `admin` 只读复验必须监听网络请求，若出现以下写请求则失败：
  - `/migration-confirm`
  - `/draft-reupload`
  - `/recognize-uploaded`
  - `/submit`
  - `/approve`
- 每个功能点至少一个真实 E2E：
  - `edhr-batch-version-migration-diff-real-flow.e2e.js`
  - `edhr-batch-version-confirm-required-real-flow.e2e.js`
  - `edhr-batch-version-draft-reupload-real-flow.e2e.js`
  - `edhr-batch-version-migration-evidence-readonly-real-flow.e2e.js`

## 待阶段一合入后启用

TODO(PHASE2_WAIT_PHASE1): 阶段一版本快照、迁移证据、审批门禁 API 合入前，本前端阶段只保留设计，不启用写入入口或真实 E2E 通过声明。

- 阶段一 API 返回 `versionId` 后，导入成功页跳转或打开 diff 抽屉。
- 阶段一版本列表页面可用后，再挂载“迁移差异”入口。
- 阶段一审批状态完整后，再启用“已提交审批禁止重传”的页面断言。
