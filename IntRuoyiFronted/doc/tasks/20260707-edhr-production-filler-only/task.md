# eDHR 普通工序仅配置生产填写人

## 任务目标

按质量口径调整工艺路线批记录用途配置：普通 eDHR 工序只需要配置生产填写人；单元格填满并完成电子签名后即可流转到下一个表单。审核和批准不在工序级人员设置里配置，只在放行阶段处理。

## 非目标

- 不修改放行阶段审核/批准流程。
- 不新增独立记录本页面。
- 不引入 fallback、mock 数据或吞异常逻辑。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`，中文读写必须显式 UTF-8。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，沿用蓝白中性运维控制台样式。
- 前端特性：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`，保留现有路由和保存接口边界。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，移除工序级审核/批准配置入口，避免普通工序被误配审批链。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: 普通工序只配置生产填写人 -> Given 用户在批记录用途配置页查看工序表单人员配置 / When 查看行内状态或打开配置弹窗 / Then 页面只展示生产填写人和电子签名流转说明，不展示审核人或批准人配置。

BDD: 工序级保存不携带审核批准规则 -> Given 用户保存普通工序填写人设置 / When 前端提交人员设置 / Then 保存请求仍保留 fillRule，但 signatureRules 固定为空数组，审核和批准留给放行阶段。

## 里程碑

- [x] M1：更新静态回归测试，先锁定普通工序不显示审核/批准。
- [x] M2：更新 `RouteUsePage.vue` 行内入口、弹窗文案和保存 payload。
- [x] M3：运行静态测试、eslint、类型检查，记录 GREEN 证据。
- [x] M4：收尾清理预览并提交本任务直接改动。

## 预期验证

- `node tests/e2e/edhr-production-filler-only-static.spec.js`
- `node tests/e2e/edhr-process-form-permission-static.spec.js`
- `pnpm exec eslint src/views/mes/pro/route-use/RouteUsePage.vue tests/e2e/edhr-production-filler-only-static.spec.js tests/e2e/edhr-process-form-permission-static.spec.js`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check`

## 当前状态

completed

## 完成记录

- 实现：普通工序行内入口改为 `生产填写`、`生产填写人`、`设置生产填写人`，并显示“填满单元格并电子签名后流转”。
- 弹窗：标题改为 `工序表单生产填写人设置`，只保留生产填写人来源、填写范围、完成策略和处理时限。
- 保存：工序级保存 payload 固定 `signatureRules: []`，审核/批准不再在普通工序配置，留到放行阶段。
- 验证：普通工序生产填写人静态合同、既有权限合同、eslint 和 `pnpm ts:check` 均已通过。

## Cleanup Keep

- `doc/tasks/20260707-edhr-production-filler-only/frontend-feature-evidence.md`
