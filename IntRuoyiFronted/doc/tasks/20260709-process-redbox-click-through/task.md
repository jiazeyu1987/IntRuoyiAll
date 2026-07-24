# Task: 工序设置红框内容可点击跳转

## 任务目标

- 将工序设置列表中“批记录表单 / 生产填写人 / 质量填写人 / 设备填写人”四列改为可点击实体入口。
- 前端按后端返回的真实对象类型和 ID 跳转目标页面，并在目标页面初始化筛选。
- 不通过名称猜目标，不引入 fallback、降级或静默兼容逻辑。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；中文文件读写必须显式 UTF-8，命令不使用 `&&`。
- 项目经验索引：已读取 `docs/experience-index.md`；本任务命中 PowerShell、前端页面 / 表格 / 样式、BDD/TDD 门禁。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；列表保持紧凑运维控制台样式，红框列使用 link-style 行内入口。
- 高风险动作：真实 E2E 前必须读取 `docs/login-access.md` 并完成登录 preflight；未完成前不执行长链路真实 E2E。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；前端只消费后端正式结构化对象。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 批记录表单可点击查看 -> Given 工序绑定了批记录表单 / When 用户点击批记录表单名 / Then 跳转到电子批记录模板页并过滤打开对应表单。
- BDD: 权限角色填写人可点击过滤 -> Given 填写人来源是 ROLE / When 用户点击填写人 / Then 跳转到权限角色页并过滤到对应角色。
- BDD: 部门填写人可点击过滤 -> Given 填写人来源是 DEPT 或 DEPT_LEADER / When 用户点击填写人 / Then 跳转到部门管理并过滤到对应部门。
- BDD: 用户填写人可点击过滤 -> Given 填写人来源是 USER 或 USERS / When 用户点击填写人 / Then 跳转到用户管理并过滤到对应用户。

## 里程碑

- [x] M1：建立任务记录并读取经验门禁。
- [x] M2：补 RED 前端静态契约。
- [x] M3：接入后端结构化字段类型。
- [x] M4：实现红框列点击跳转与目标页 query 过滤。
- [x] M5：运行目标验证、closeout preview 并处理提交边界。

## 预期验证

- `node tests/e2e/mes-pro-process-redbox-click-through-static.spec.js`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-process-redbox-click-through/frontend-feature-evidence.md`

## 当前状态

COMPLETED_WITH_COMMIT_BLOCKER：前端静态测试与类型检查已通过；当前混合工作区存在大量既有脏改与目标文件重叠，暂不提交以避免夹带无关改动。

## Cleanup Keep

- `doc/tasks/20260709-process-redbox-click-through/frontend-feature-evidence.md`
