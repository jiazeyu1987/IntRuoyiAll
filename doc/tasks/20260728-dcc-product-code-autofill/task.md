# DCC 受控文件产品编号自动生成

## Task Goal

在受控文件提交页面中，DHF/DMR 类文件的“产品编号”字段必须来自已选择的 DCC 项目代码，权威来源为 `dcc_project_code.project_code` / `DccProjectCodeRespVO.projectCode`；不得临时生成编号，不得改用其它业务数据源、空值、默认值或表单槽位数据替代。

## Milestones

1. [completed] 梳理当前 DCC 提交流程中产品编号字段、分类规则和接口契约。
2. [completed] 记录 BDD 场景并补充最小 RED 静态/单元测试。
3. [completed] 将上传页产品编号改为只读字段，并按 DCC 项目代码自动生成。
4. [completed] 将后端 DHF/DMR 受控上传提交的产品编号来源切换为 DCC 项目代码。
5. [completed] 运行目标验证、相邻回归和只读真实页面 E2E。
6. [pending] 完成收尾、经验沉淀、提交与推送。

## Expected Verification

- 目标静态合同或单元测试先 RED 后 GREEN。
- 前端目标范围静态验证通过；若全量验证存在无关历史阻塞，记录阻塞范围。
- 后端 DCC 模块编译和目标服务测试通过。
- 真实页面 E2E 通过：选择启用 DCC 项目后，“产品编号”自动生成该项目代码；选择 DHF/DMR 类别后保持一致；不发送 DCC 写请求，不查询其它业务数据源选项。

## Current Status

ready_for_closeout

- 用户已澄清：红框产品编号只认 DCC 项目代码数据，DCC 项目代码是权威数据。
- 实现已按 DCC 项目代码来源修正，前端、后端目标测试和只读真实页面 E2E 均已通过。
- `pnpm ts:check` 仍被无关 MES 历史问题阻塞：`src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue` 第 117、121 行引用不存在的 `assistPreviewRows`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，DHF/DMR 上传的产品编号直接来自 DCC 项目代码权威字段。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs/experience-index.md`。
- 适用门禁：前端静态契约隔离门禁；本任务使用 `dcc-upload-product-autofill-static.spec.js` 锁定红框产品编号来源，避免被无关大契约阻塞。
- 适用门禁：严格无 fallback；缺少 DCC 项目代码时前后端 fail-fast，不使用其它数据源补齐。

## Cleanup Keep

- doc/tasks/20260728-dcc-product-code-autofill/frontend-feature-evidence.md
- doc/tasks/20260728-dcc-product-code-autofill/backend-api-evidence.md
- doc/tasks/20260728-dcc-product-code-autofill/database-schema-evidence.md
