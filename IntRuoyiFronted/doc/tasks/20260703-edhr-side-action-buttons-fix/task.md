# 任务：修正 eDHR 详情页右侧按钮逻辑

## 任务目标

将批记录详情页右侧红框中的「工序执行 / 审签归档」按钮从泛化证据导航修正为当前工序的业务动作入口：打开当前工序填写页、定位当前工序工作任务、查看当前工序追踪、签名、审批详情，并让单表归档进入当前执行详情的归档区域，而不是误跳到列表或泛化页。

## 里程碑

- [x] M1：读取 PowerShell、经验索引、前端交付技能与当前脏改边界。
- [x] M2：补充 RED 静态测试，锁定红框按钮的正确路由/处理逻辑。
- [x] M3：最小修改按钮配置与点击处理，不改后端合同。
- [x] M4：运行 targeted 前端静态验证与证据校验。
- [x] M5：更新任务记录并按验证结果提交本任务直接改动。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；中文文档写入使用显式 UTF-8，不使用 `&&`。
- 前端页面 / 表格 / 样式：本轮修正现有按钮行为，不做无关视觉重设计。
- 前端交付：已读取 `frontend-feature-delivery` 与 `references/frontend-contract.md`；需记录 BDD、RED/GREEN 与 evidence。
- 真实 E2E：本轮先做静态契约验证；如需真实登录链路，必须先读取 `docs/login-access.md` 并跑登录 preflight。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；缺少 executionId/workTaskId 时仍显式报错或禁用。
- 是否从根因和长期维护角度解决：是；修正红框入口的路由语义和上下文传递，避免按钮文案与实际跳转不一致。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 审签归档按钮打开当前工序动作详情 -> Given 用户在批记录详情页选中一道已有执行记录的工序 / When 点击签名记录、审批记录或单表归档 / Then 页面进入当前工序对应详情视图，不跳到泛化列表页。
- BDD: 工序执行按钮保持当前工序上下文 -> Given 用户在批记录详情页选中一道工序 / When 点击打开工序、工作任务或执行追踪 / Then 路由或接口携带 batchExecutionId、executionId、workTaskId、routeProcessId 与 reportId 等上下文。

## 预期验证

- `node tests/e2e/edhr-side-action-buttons-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-edhr-side-action-buttons-fix/frontend-feature-evidence.md`

## 当前状态

- 状态：completed
- 已完成：签名记录和单表归档入口改为进入当前执行详情页并通过 `focus` 定位；审批记录入口保留审批详情页并补齐当前工序上下文。`ExecutionPage.vue` 已消费 `focus=signature/archive`。验证通过：新增静态契约测试、冗余文案静态测试、前端证据校验。已知阻塞：旧 `edhr-process-form-action-columns-static.spec.js` 仍要求前序任务删除的冗余标题，不属于本轮按钮逻辑改动。

## Cleanup Keep

- doc/tasks/20260703-edhr-side-action-buttons-fix/frontend-feature-evidence.md
