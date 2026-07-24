# 任务：eDHR 批次模板预览入口

## 任务目标

- 在 `/mes/pro/feedback/edhr-batch-execution` 列表的 `复盘` 右边新增 `模板` 按钮。
- 新增只读模板说明页，左侧展示当前批次内全部带模板的工序/表格，右侧展示模板布局与单元格用途提示。
- 复用现有批次详情、批记录模板规则与签名位接口，不新增后端契约，不做真实数据写入。

## 当前状态

已完成。

## 上一任务检查

- 上一个 frontend 任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-remove-preflight-toolbar-button\task.md`
- 状态：`已完成`
- 处理：上一任务文档已完成，不阻塞本次需求。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 本次仅修改前端源码、静态测试与任务文档，不做真实登录、真实写入、服务器操作或租户数据修改。
  - 模板入口、模板页布局与表格工作台继续遵循 IntPP 紧凑运维台样式，保持蓝白中性色、致密列表与静音行级操作风格。
  - 不通过 fallback、mock 模板、静默空态或吞异常掩盖模板布局/规则/权限错误；缺少模板布局或规则时必须明确报错。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。模板布局、规则或签名位缺失时直接明确报错。
- `是否从根因和长期维护角度解决`：是。复用现有批次详情与批记录模板规则契约，抽离可复用类型映射，避免复制多套规则字典。
- `是否存在临时补丁或绕过`：否。不新增 mock 数据，不用“只看空模板”替代真实规则说明。

## BDD 场景

- `BDD: 模板入口可见 -> Given 用户打开 eDHR 批次执行列表 When 查看某行批次操作 Then 在 复盘 右边可看到 模板 按钮。`
- `BDD: 模板页展示全量模板工序 -> Given 当前批次详情中存在多个带 batchRecordReportId 的任务 When 打开模板页 Then 左侧按工序顺序展示全部带模板任务，而不是只显示已填写工序。`
- `BDD: 模板页显示单元格用途 -> Given 用户选中某张模板 When 右侧渲染模板表格 Then 可填写和签名单元格直接显示 文字/数字/日期/日期时间/勾选/签名/附件 等中文用途提示与关键规则。`
- `BDD: 模板规则缺失明确报错 -> Given 所选模板缺少有效布局或单元格规则 When 模板页加载 Then 页面明确显示错误，不静默降级成空模板。`

## 里程碑

1. M1：创建任务文档、更新命令记录并补模板入口静态 RED 用例。
2. M2：实现模板按钮、隐藏路由、模板页与只读模板说明组件。
3. M3：运行 GREEN 静态验证、类型检查与 frontend evidence 校验。

## 预期验证

- `node tests/e2e/edhr-batch-template-preview-static.spec.js`
- `node tests/e2e/edhr-batch-history-static.spec.js`
- `node tests/e2e/edhr-inline-signature-cells-static.spec.js`
- `node tests/e2e/edhr-batch-review-summary-labels-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-batch-template-preview\frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/edhr-batch-template-preview-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-history-static.spec.js` -> PASS
- `node tests/e2e/edhr-inline-signature-cells-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-review-summary-labels-static.spec.js` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-batch-template-preview\frontend-feature-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-edhr-batch-template-preview --mode preview` -> READY，仅建议清理 `frontend-feature-evidence.md`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> BLOCKED，本地 `node_modules` 缺少 `@volar/typescript/lib/quickstart/runTsc`
