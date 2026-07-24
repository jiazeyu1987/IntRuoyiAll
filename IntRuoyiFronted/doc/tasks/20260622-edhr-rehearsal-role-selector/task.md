# 任务：eDHR 演练预检人员选择器

## 任务目标

- 优化已新增的 eDHR 演练预检面板，让执行人、审批人、归档员通过正式用户列表选择，而不是只能手填用户 ID。
- 保持预检只读，不写用户、角色、权限、签名或 BPM 数据。
- 让操作者在同一入口看到用户名/昵称/ID，降低演练前人工查库成本。

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260622-edhr-rehearsal-readiness-panel\task.md`
- 状态：`COMPLETED`
- 处理：上一任务已完成并提交，不阻塞本次可用性增强。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 前端页面继续遵循 IntPP 操作台样式，不做无关视觉重设计。
  - 前端请求失败必须暴露真实错误，不得静默降级为手填成功。
  - 本切片只读取系统用户精简列表和 readiness API，不写真实租户数据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，使用正式用户列表减少演练前查库和 ID 误填。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 预检责任人可从正式用户列表选择 -> Given 系统用户精简列表可读取 / When 用户打开演练预检面板 / Then 执行人、审批人、归档员字段提供可搜索用户选择。`
- `BDD: 用户列表加载失败必须可见 -> Given 系统用户列表接口失败 / When 打开演练预检面板 / Then 页面显示真实错误，不把空列表当作正常可演练状态。`
- `BDD: 提交仍使用真实用户 ID -> Given 用户通过选择器选中三类责任人 / When 点击开始预检 / Then 请求仍传递 executorUserId、approverUserId、archiverUserId 给 readiness API。`

## 里程碑

1. M1：创建任务包与 RED 静态合同。`DONE`
2. M2：接入系统用户列表选择器并保持错误可见。`DONE`
3. M3：运行静态合同、类型检查和证据校验。`DONE`
4. M4：收尾清理预览并提交。`IN_PROGRESS`

## 预期验证

- `node tests/e2e/edhr-rehearsal-role-selector-static.spec.js`
- `node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260622-edhr-rehearsal-role-selector\frontend-feature-evidence.md`

## 当前状态

`COMPLETED`

已将 eDHR 演练预检对话框中的三类责任人字段改为正式系统用户列表选择器，选项展示用户名、昵称和 ID；打开对话框时加载用户列表，加载失败直接显示错误。

## Cleanup Keep

- `doc/tasks/20260622-edhr-rehearsal-role-selector/frontend-feature-evidence.md`
