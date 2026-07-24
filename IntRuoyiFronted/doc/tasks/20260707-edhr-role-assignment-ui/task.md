# eDHR 工序人员设置入口可理解化

## 任务目标

将工艺路线批记录用途配置中当前难以理解的“规则状态 / 审批位 / 批准/复核位 / 权限/派工”改为面向业务的“填写人 / 审核人 / 批准人”设置入口。用户在批次执行配置里能明确知道当前工序由哪些人填写、哪些人审核、哪些人批准。

## 非目标

- 不新增独立 eDHR 记录本页面。
- 不修改后端 API 契约、保存接口、审计证据或签名证据模型。
- 不引入 fallback、mock 数据或吞异常逻辑。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`，中文读写必须显式 UTF-8。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，沿用蓝白中性运维控制台样式。
- 前端特性：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`，保留现有 API、权限、路由和状态边界。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，将配置入口和弹窗术语直接改为业务人员角色，避免继续暴露实现层“签名位/派工”术语。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: 工序人员设置可理解 -> Given 用户在批记录用途配置页为某个工序绑定批记录表 / When 查看该行人员设置 / Then 页面直接展示填写人、审核人、批准人的配置状态，并提供“设置人员”入口，不再显示“审批位”“批准/复核位”“权限/派工”。

BDD: 人员设置弹窗按业务角色分组 -> Given 用户点击“设置人员” / When 弹窗打开 / Then 表单按填写人、审核人、批准人分组，字段标签明确提示选择哪些人/范围，保存按钮显示“保存人员设置”。

## 里程碑

- [x] M1：新增静态回归测试，先复现当前术语不可理解问题。
- [x] M2：更新 `RouteUsePage.vue` 人员设置入口、弹窗标题和字段文案。
- [x] M3：运行静态测试、eslint、类型检查，记录 GREEN 证据。
- [x] M4：收尾清理预览并提交本任务直接改动。

## 预期验证

- `node tests/e2e/edhr-role-assignment-ui-static.spec.js`
- `node tests/e2e/edhr-process-form-permission-static.spec.js`
- `pnpm exec eslint src/views/mes/pro/route-use/RouteUsePage.vue tests/e2e/edhr-role-assignment-ui-static.spec.js tests/e2e/edhr-process-form-permission-static.spec.js`
- `pnpm ts:check`

## 完成记录

- 实现：将批记录用途配置中的规则状态、审批位、批准/复核位、权限/派工等实现层术语改为填写人、审核人、批准人和设置人员。
- 契约：保留现有 `APPROVAL`、`APPROVE`、`REVIEW` API 角色值和保存接口，不改后端契约。
- 验证：静态合同、eslint 和高堆内存类型检查均已通过。

## Cleanup Keep

- `doc/tasks/20260707-edhr-role-assignment-ui/frontend-feature-evidence.md`

## 当前状态

completed
