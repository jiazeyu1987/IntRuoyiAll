# 任务：MES 待归属页单页归属与整批确认前端改造

## 任务目标

- 将“待归属 -> 跳正式报工逐条补录/逐条提交”的旧流程，改成待归属页单页闭环。
- 以当前导入返回的 `importRecordIds` 作为本次 Excel / 模拟导入批次范围，在待归属页完成归属、字段补齐与整批确认。
- 归属后只保留 `修改归属`，移除 `查看正式报工`；顶部新增当前批次摘要与 `确认报工`。
- 行内直接填写 `报工人`、`报工时间`、`当前审批人`、`备注`；确认报工时整批校验并提交。
- “其他订单”行明确标记为跳过，不显示草稿编辑字段，不阻断其他真实工序草稿确认。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-signature-cell-electronic-signature\task.md`
- 状态：`BLOCKED`
- 处理说明：该任务已因用户切换主题显式阻塞；本次仅继续 MES 报工待归属工作台相关页面、静态测试与任务文档，不回退工作区其他无关改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 待归属页仍需保持 IntPP 紧凑运营台样式，不新增营销式卡片或脱离列表场景的重视觉布局。
  - 本轮先做本机源码、静态契约和类型层改造；如后续进入真实 Playwright 登录或写入验证，必须先补 `experience-preflight` 与官方登录预检记录。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。存在未归属、漏填字段、草稿状态异常时必须明确阻断整批确认。
- `是否从根因和长期维护角度解决`：是。直接把导入批次工作流收敛到待归属页，避免继续依赖正式报工列表逐条补录/逐条提交。
- `是否存在临时补丁或绕过`：否。不保留旧的导入草稿单条提交入口作为兼容路径。

## BDD 场景

- `BDD: 当前批次待归属工作台 -> Given 用户刚完成一次 Excel 导入或模拟报工 / When 页面切到待归属页 / Then 列表必须按本次 importRecordIds 锁定，并在顶部展示来源文件、条数、已归属数、未归属数和可确认草稿数。`
- `BDD: 已归属行在待归属页内补齐草稿字段 -> Given 某条导入记录已归属真实工序并生成 PREPARE 草稿 / When 用户查看待归属列表 / Then 行内直接显示报工人、报工时间、当前审批人和备注编辑控件，不再跳到正式报工页。`
- `BDD: 批量确认阻断未归属或漏填 -> Given 当前批次仍有 PENDING 行或已归属真实工序行缺少报工人/报工时间/当前审批人 / When 用户点击确认报工 / Then 页面必须给出整批阻断清单，且不允许部分提交。`
- `BDD: 其他订单行跳过确认 -> Given 当前批次中存在归属到其他订单的记录 / When 用户查看待归属列表并执行确认报工 / Then 该行显示为“其他订单/本批跳过”，不展示草稿编辑字段，也不阻断真实工序草稿整批提交。`
- `BDD: 待归属页不再引导单条正式报工 -> Given 用户查看已归属记录 / When 页面渲染操作列和正式报工列表入口 / Then 待归属行仅显示选择归属或修改归属，来源于导入记录的 PREPARE 草稿不再暴露单条提交入口。`
- `BDD: 确认报工成功后返回正式报工页签 -> Given 用户在待归属页成功确认当前导入批次 / When 后端整批提交成功 / Then 页面弹框提示“报工成功”，并自动切回正式报工 tab。`

## 里程碑

1. M1：创建任务包、补 RED 静态测试与页面合同。
2. M2：实现待归属页批次摘要、行内编辑字段、跳过其他订单展示与操作收敛。`COMPLETED`
3. M3：实现顶部确认报工、整批阻断提示与导入草稿单条提交入口移除。`COMPLETED`
4. M4：运行前端定向验证并回写证据。`COMPLETED`

## 预期验证

- `node tests/e2e/mes-feedback-import-current-batch-static.spec.js`
- `node tests/e2e/mes-feedback-attribution-continuation-static.spec.js`
- `node tests/e2e/mes-feedback-tracking-static.spec.js`
- `node tests/e2e/mes-feedback-pending-batch-confirm-static.spec.js`

## 最终验证结果

- `node tests/e2e/mes-feedback-pending-batch-confirm-static.spec.js` -> PASS
- `node tests/e2e/mes-feedback-import-diagnostics-hidden-static.spec.js` -> PASS
- `node tests/e2e/mes-feedback-tracking-static.spec.js` -> PASS

## Cleanup Keep

- `doc/tasks/20260626-mes-feedback-pending-batch-confirm/frontend-feature-evidence.md`
