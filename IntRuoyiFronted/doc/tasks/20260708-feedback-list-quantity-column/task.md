# 任务：生产报工正式列表补充报工个数列

## 任务目标

按用户截图反馈，在 `生产报工 -> 正式报工` 列表中补充工序对应的报工个数，使用后端正式报工接口已返回的 `feedbackQuantity` 字段展示，不调整待归属列表和报工归属逻辑。

## 经验门禁

- 已读取 `docs/powershell-memory.md`：本轮涉及 PowerShell 与中文文件读写，必须显式 UTF-8，不使用 `&&`。
- 已读取 `docs/experience-index.md`：命中“前端页面 / 表格 / 样式”，需遵循统一前端样式来源。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：本次只补充表格必要字段，不做无关视觉重设计。
- 已读取 `frontend-feature-delivery` 与 `bug-regression-fix-loop`：用户截图反馈属于页面缺字段修复，必须记录 BDD、RED/GREEN 和回归证据。
- 本轮只修改本机前端源码、静态测试和任务文档；不操作服务器、不修改数据库、不改真实租户数据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；直接复用正式报工接口字段 `feedbackQuantity`，在正式列表展示。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 正式报工列表展示报工个数 -> Given 用户打开生产报工正式报工列表 / When 查看产品、工序、人员和日期信息 / Then 表格同时显示“报工个数”，并绑定正式报工接口的 `feedbackQuantity` 字段。
- BDD: 不影响待归属列表 -> Given 用户切换到待归属页签 / When 查看导入待归属记录 / Then 原有“报工数量”列和归属编辑流程保持不变。

## 里程碑

1. M1：建立任务文档与静态契约。`DONE`
2. M2：补充正式报工表格“报工个数”列。`DONE`
3. M3：运行聚焦静态验证与必要类型检查。`DONE`
4. M4：记录证据并汇总结果。`DONE`

## 预期验证

- RED：`node tests/e2e/mes-feedback-list-excel-columns-static.spec.js` 在旧页面上失败，证明正式报工列表缺少“报工个数”列。
- GREEN：`node tests/e2e/mes-feedback-list-excel-columns-static.spec.js` 通过。
- TYPE：`pnpm ts:check:schedule` 通过或明确记录阻塞。

## 当前状态

COMPLETED：正式报工列表已新增“报工个数”列，静态契约、类型检查和证据校验均已通过。

## 验证结果

- RED：`node tests/e2e/mes-feedback-list-excel-columns-static.spec.js` -> FAIL，旧页面缺少按截图顺序展示的“报工个数”列。
- GREEN：`node tests/e2e/mes-feedback-list-excel-columns-static.spec.js` -> PASS。
- GREEN：`pnpm ts:check:schedule` -> PASS。

## Cleanup Keep

- `doc/tasks/20260708-feedback-list-quantity-column/frontend-feature-evidence.md`
