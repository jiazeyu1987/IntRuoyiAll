# Verification Report

## Bug

表单模板右侧“编辑”应在表单模板模块内打开当前模板自己的 Jimu 编辑器。用户复验时看到页面像空白，是因为 Jimu 画布已经加载，但外层 IFrame loading 遮罩仍在等待行高适配延迟，导致内容被压淡，看起来像没有进入正确编辑器。

## Expected

点击表单模板右侧“编辑”后：

- 页面仍停留在 `/mdm/form-center/template` 表单模板模块。
- URL 使用 `mode=designer&reportMode=edit`，并携带当前模板 `templateId/versionNo/reportId`。
- iframe 进入当前模板自己的 `/jmreport/index/FORMTPL:*` Jimu 编辑器。
- Jimu 编辑器显示当前模板内容，外层 loading 遮罩消失，不显示空白页，也不跳到批记录表单模块。
- 在 Jimu 编辑器内点击保存后，后端把最新画布同步回表单模板版本正式 `jimuSchemaJson/sheetLayoutJson`。
- 只有草稿版本允许被 Jimu 保存写入；已发布、停用、作废等正式版本必须明确拒绝写入。

## Reproduction

真实路径：登录本机 `int_main`，进入 `/mdm/form-center/template` 表单模板列表，选中“按压式压力泵过程检验记录 / V21.0”，点击右侧“编辑”。

## Root Cause

前置路由修复已经让“编辑”留在表单模板页并进入 `/jmreport/index/FORMTPL:*`。本轮缺陷的根因是通用 IFrame 的 `jmreport-designer-edit` 适配流程在检测到 Jimu 画布已绘制后，仍等待 `DESIGNER_EDIT_PAINT_STABLE_DELAY_MS` 行高稳定延迟才关闭外层 loading。用户在这段时间看到的是被 loading 遮罩压淡的 Jimu 页面，视觉上接近空白。

## RED

`node tests/e2e/jmreport-designer-edit-row-height-static.spec.js` -> FAIL，旧实现未在 `isDesignerEditCanvasPainted(frameWindow)` 后立即释放外层 loading mask。

历史 RED：`node tests/e2e/form-template-independent-button-actions-static.spec.js` -> FAIL，旧实现曾把表单模板“编辑”导向 `/mes/pro/batch-record-form-list?mode=designer&reportMode=edit`。

## GREEN

`node tests/e2e/form-template-jimu-save-back-static.spec.js` -> PASS

`node tests/e2e/jmreport-designer-edit-row-height-static.spec.js` -> PASS

`node tests/e2e/form-template-independent-button-actions-static.spec.js` -> PASS

`node tests/e2e/form-template-button-interaction-parity-static.spec.js` -> PASS

`node tests/e2e/form-template-edit-designer-parity-static.spec.js` -> PASS

`node tests/e2e/form-center-static.spec.js` -> PASS

`node tests/e2e/form-template-edit-designer-parity-real.e2e.js` -> PASS

`node tests/e2e/form-template-jimu-save-back-real.e2e.js` -> PASS

`mvn -pl yudao-module-bpm "-Dtest=FormCenterRuntimeContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 12 tests / 0 failures / 0 errors

`pnpm exec eslint src/api/form-center/template.ts src/views/form-center/template/index.vue tests/e2e/form-template-jimu-save-back-static.spec.js tests/e2e/form-template-jimu-save-back-real.e2e.js tests/e2e/form-template-edit-designer-parity-real.e2e.js tests/e2e/form-template-button-interaction-parity-static.spec.js tests/e2e/form-template-edit-designer-parity-static.spec.js` -> PASS

`pnpm ts:check` -> PASS

`git diff --check -- <task-owned form-template/Jimu files>` -> PASS，仅 CRLF 提示，无 whitespace error。

## Real E2E Evidence

- Frontend: `http://127.0.0.1:8081/`
- Backend health: `http://127.0.0.1:48081/actuator/health` -> `UP`
- Tenant/account label: `芋道源码/admin`
- Result URL: `http://127.0.0.1:8081/mdm/form-center/template?templateId=33&versionNo=V21.0&mode=designer&reportId=FORMTPL:54&reportMode=edit`
- iframe src: `/jmreport/index/FORMTPL:54?tenantId=1&token=[redacted]`
- Jimu data: `rowCount=47`, `textCellCount=222`
- Outer loading mask: `0`
- iframe screenshot non-white pixels: `12211 / 62475 sampled`
- Workspace: `form-template`
- Screenshots: `doc/tasks/20260828-form-template-edit-button-batch-record-designer/template-edit-jimu-editor.png`, `doc/tasks/20260828-form-template-edit-button-batch-record-designer/template-edit-jimu-editor-iframe.png`

## 2026-08-29 Recheck

- 结论：已按用户最新反馈复核，点击表单模板右侧“编辑”后进入的是表单模板模块内的当前模板 Jimu 编辑器，不是批记录表单页面。
- 页面路径：`/mdm/form-center/template?templateId=33&versionNo=V21.0&mode=designer&reportId=FORMTPL:54&reportMode=edit`
- iframe：`/jmreport/index/FORMTPL:54?tenantId=1&token=[redacted]`
- 模板内容：`rowCount=47`，`textCellCount=222`
- 外层遮罩：`0`
- 非空截图采样：`12211 / 62475`
- 截图：`doc/tasks/20260828-form-template-edit-button-batch-record-designer/template-edit-jimu-editor.png`

## 2026-08-29 Final Recheck

- 结论：已按“编辑按钮仍在表单模板页内，右侧编辑逻辑与批记录表单一致”的口径完成复核。当前点击“编辑”后左侧菜单仍选中“表单模板”，面包屑仍为“基础数据 / 表单模板”，中间嵌入当前模板自己的 Jimu 编辑器。
- 页面路径：`/mdm/form-center/template?templateId=33&versionNo=V21.0&mode=designer&reportId=FORMTPL:54&reportMode=edit`
- iframe：`/jmreport/index/FORMTPL:54?tenantId=1&token=[redacted]`
- 模板内容：`rowCount=47`，`textCellCount=222`
- 外层遮罩：`0`
- 非空截图采样：`12130 / 62475`
- 截图：`doc/tasks/20260828-form-template-edit-button-batch-record-designer/template-edit-jimu-editor.png`

## 2026-08-29 Save Back Recheck

- 结论：表单模板 Jimu 编辑器内的保存已经接入正式回写链路。Jimu 原生 `/jmreport/save` 成功后，后端会把最新画布同步回同一表单模板版本的正式 `jimuSchemaJson.sheetLayoutJson`，并且只允许草稿版本被写入。
- 真实路径：本机 `int_main`，`芋道源码/admin` 登录，表单模板页选中 `按压式压力泵过程检验记录 / V10.0` 草稿，点击右侧“编辑”进入 `/mdm/form-center/template?templateId=33&versionNo=V10.0&mode=designer&reportId=FORMTPL:43&reportMode=edit`。
- 保存验证：E2E 在 Jimu 画布临时新增单元格 `targetRow=47,targetCol=0`，调用 Jimu 原生保存后，通过表单模板正式详情接口读回，确认 `sheetLayoutJson` 已包含临时标记。
- 恢复验证：同一 E2E 再次通过 Jimu 原生保存恢复原画布，读回确认临时标记已删除，避免测试数据残留。
- 外层配置：`cellRules`、`assistRows`、`signatureCellMarkers`、`fillAssignments` 在保存和恢复后均保持不变，证明回写只替换 `sheetLayoutJson`，不覆盖模板外层规则配置。
- 草稿门禁：真实写入目标版本状态为 `DRAFT`；后端合同测试覆盖非草稿版本必须被 `TEMPLATE_VERSION_IMMUTABLE` 拒绝。
- 命令：`node tests\e2e\form-template-jimu-save-back-real.e2e.js` -> PASS，输出 `markerVerifiedThenRemoved=true`。

## Backend Restart

本轮开始时 `48081` 未监听，真实 E2E 无法登录。按 int_main 本机规则执行标准后端启动，Maven package `BUILD SUCCESS`；脚本末尾因运行 Jar 文件锁返回 Copy-Item 错误，但随后 48081 已由 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260829-050948.jar` 启动并通过 health `UP`。首次 E2E 在启动窗口登录 500 超时，后端稳定后重跑同一真实页面路径通过。

2026-08-29 保存回写复验时，当前运行包不含新增 `FormTemplateJimuReportSaveSyncFilter` 和 `FormTemplateEditableDraftRespVO`，因此需要刷新后端。标准 `restart-int-ruoyi-local.ps1 -Component backend` 在无关 `yudao-module-mes` 编译错误处失败并停止旧 48081；为继续验证本任务，按运行态门禁生成本机验证包 `output\runtime\int_main\backend-runtime-control-20260829-120804-form-template-saveback.jar`，仅替换已通过定向 Maven 验证的 BPM 内嵌 Jar。该验证包已确认 BPM 嵌套 Jar 未压缩存储、关键类存在，48081 health 为 `UP` 后才运行真实 E2E。

## External Resource Notes

Playwright 过程中出现外部头像和百度统计请求 `ERR_ABORTED/ERR_CONNECTION_REFUSED`。这些请求不属于目标链路；目标模板页、按钮点击、Jimu iframe、画布内容和遮罩状态均已通过。

## Git / Push

工作区仍存在其它并行改动，且当前 `int_main` 已领先 `origin/int_main` 1 个非本任务提交。本轮未执行 push，避免把非本需求改动或非本任务提交一并推送。

## Blockers

暂无本需求实现 blocker。目标真实 E2E、保存回写真实 E2E、表单模板静态合同、BPM 模块接口合同、ESLint、全量 `pnpm ts:check` 和 diff 检查均已通过。标准完整后端重启仍被无关 MES 编译错误阻塞；当前 48081 使用本任务验证包运行。远端推送仍需先处理当前分支已有非本任务领先提交与工作区其它改动的归属边界。

## Closeout

2026-08-29 收尾已完成：经验门禁已写入 `docs/backend-development.md`、`docs/frontend-development.md` 和 `docs/experience-index.md`；`task-closeout-cleanup` preview/apply 已清理本任务临时解包目录 `output\tmp-jimureport-jar-inspect`，保留任务记录和正式验证截图。任务状态已标记为 `completed`。
