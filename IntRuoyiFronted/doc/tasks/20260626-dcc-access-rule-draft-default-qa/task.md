# 任务：DCC 未保存目录初始仅显示 QA 规则

## 任务目标

- 当用户从 `DCC 访问规则` 页的“新增目录”树中选中一个未绑定目录时，右侧不再一次性加载该目录继承到的全部真实规则。
- 未保存目录进入维护态时，初始仅显示一条默认规则：主体类型为 `部门`，授权对象为 `QA`；如需更多规则，用户自行点击“新增规则”补充。
- 已绑定目录继续按真实已保存规则全量展示；不引入 fallback、静默降级或额外后端契约改动。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个 frontend 任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-dcc-access-rule-manual-bound-list\task.md`
- 状态：`COMPLETED`
- 处理：上一任务已完成“左侧仅显示手动保存目录”的口径收紧，本次继续收敛未保存目录的初始规则展示，不阻塞启动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 本次只调整未保存目录的规则初始化行为，不做无关样式或布局改造。
  - 不通过前端启发式比较父目录/继承规则做复杂推断；仅基于“是否未保存目录”切换初始化规则来源。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。未保存目录直接使用单条默认 QA 规则，不保留“继承规则展示兜底”。
- `是否从根因和长期维护角度解决`：是。把未保存目录与已绑定目录的初始规则来源明确分流，避免用户在草稿态看到整组继承规则。
- `是否存在临时补丁或绕过`：否。不会用隐藏行、折叠行或局部过滤伪装成只显示一条。

## BDD 场景

- `BDD: 未保存目录初始仅显示一条 QA 规则 -> Given 用户从新增目录树选择一个不在左侧列表中的目录 When 页面切换到该未保存目录 Then 右侧规则表仅初始化一条主体类型为部门且授权对象为 QA 的规则。`
- `BDD: 已绑定目录继续显示真实保存规则 -> Given 用户点击左侧已绑定目录 When 页面加载规则 Then 右侧继续展示该目录真实保存的全部规则。`
- `BDD: 用户仍可手动补充更多规则 -> Given 未保存目录已初始化单条 QA 规则 When 用户点击新增规则 Then 页面在现有 QA 规则基础上继续新增可编辑规则行。`

## 里程碑

1. M1：创建任务文档并补回归 RED 约束。`COMPLETED`
2. M2：实现未保存目录默认 QA 规则初始化逻辑。`COMPLETED`
3. M3：运行静态回归与类型检查，补齐 evidence。`COMPLETED`

## 预期验证

- `node tests/e2e/dcc-access-rule-draft-default-qa-static.spec.js`
- `node tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js`
- `node tests/e2e/dcc-access-rule-save-validation-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## 最终验证结果

- PASS：`node tests/e2e/dcc-access-rule-draft-default-qa-static.spec.js`
- PASS：`node tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js`
- PASS：`node tests/e2e/dcc-access-rule-save-validation-static.spec.js`
- PASS：`node tests/e2e/dcc-access-rule-header-context-static.spec.js`
- PASS：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## Cleanup Keep

- `doc/tasks/20260626-dcc-access-rule-draft-default-qa/task.md`
- `doc/tasks/20260626-dcc-access-rule-draft-default-qa/execution-log.md`
- `doc/tasks/20260626-dcc-access-rule-draft-default-qa/bug-regression-evidence.md`
