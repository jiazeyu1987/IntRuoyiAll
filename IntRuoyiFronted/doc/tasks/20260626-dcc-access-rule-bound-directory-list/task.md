# 任务：DCC 访问规则已绑定目录列表 + 树形新增

## 任务目标

- 将 `DCC 访问规则` 页面从“左侧目录树驱动”改为“左侧已绑定目录列表驱动”。
- 左侧仅显示已经存在访问规则的目录，显示值使用完整路径字符串，格式与“绑定目录”一致。
- 保留右侧规则表的主体类型、授权对象、启用/查看/预览/下载字段契约，不改保存字段结构。
- 新增目录时使用树形下拉选择；选择未绑定目录时先进入未保存草稿态，保存后才进入左侧列表。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个 frontend 任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-dcc-basic-data-product-catalog-tab\task.md`
- 状态：`COMPLETED`
- 处理：上一前端任务已完成，不阻塞本次访问规则页面改造。
- 当前前端仓库存在 MES / 展厅相关未归属脏改；本任务只修改 DCC 访问规则相关代码、测试与本任务文档，不覆盖其他改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 页面布局继续遵循 IntPP 运维台风格，左侧列表与右侧规则区保持紧凑、可扫描的操作台样式，不做无关视觉重构。
  - 前端不得通过 mock、placeholder、fallback、静默 catch 或空数据兜底掩盖后端真实接口错误。
  - 若执行真实登录验收，第一条登录相关命令必须先运行官方 `login-preflight.mjs`，高风险真实 E2E 前需在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。左侧目录列表与右侧规则区均直接消费真实接口，不新增兼容分支。
- `是否从根因和长期维护角度解决`：是。通过新增“已绑定目录列表”接口与前端显式草稿态，避免前端遍历整棵目录逐个拉规则。
- `是否存在临时补丁或绕过`：否。不会把目录树简单隐藏后仍用旧选择流，也不会用前端缓存伪造已绑定列表。

## BDD 场景

- `BDD: 左侧仅显示已绑定目录路径 -> Given 管理员打开 DCC 访问规则页 and 系统中只有部分目录存在访问规则 When 页面加载完成 Then 左侧只显示这些已绑定目录，并以完整路径字符串展示，而不是目录树节点。`
- `BDD: 新增目录时未绑定目录先进入草稿态 -> Given 管理员点击新增目录 and 选择一个尚无访问规则的目录 When 页面切换到该目录 Then 右侧进入空规则草稿态，保存成功前左侧不出现该目录。`
- `BDD: 新增目录选择已绑定目录只切换不重复 -> Given 管理员点击新增目录 and 选择一个已经在左侧列表中的目录 When 页面切换 Then 左侧不新增重复项，只高亮并加载该目录现有规则。`
- `BDD: 左侧删除删除整个目录规则集合 -> Given 管理员在左侧列表删除一个已绑定目录 When 删除成功 Then 该目录全部访问规则被移除，刷新后左侧不再显示该目录。`
- `BDD: 当前目录规则保存契约保持不变 -> Given 管理员在当前目录内新增、修改或删除单条规则 When 点击保存规则 Then 页面继续提交原有真实字段，不改变查看、预览、下载、启用绑定。`

## 里程碑

1. M1：创建任务文档、记录门禁与 RED/GREEN 计划。`COMPLETED`
2. M2：补静态 RED 合同，锁定左侧已绑定目录列表、树形新增入口与草稿态文案。`COMPLETED`
3. M3：实现前端目录列表驱动、树形新增与整组删除交互。`COMPLETED`
4. M4：运行静态、类型检查与真实登录验收，并补齐证据。`COMPLETED`

## 预期验证

- `node tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js`
- `node --check tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-dcc-access-rule-bound-directory-list\frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-dcc-access-rule-bound-directory-list\frontend-feature-evidence.md` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /dcc/controlled-file/access-rules --target-text 访问规则` -> PASS

## Cleanup Keep

- `doc/tasks/20260626-dcc-access-rule-bound-directory-list/task.md`
- `doc/tasks/20260626-dcc-access-rule-bound-directory-list/execution-log.md`
- `doc/tasks/20260626-dcc-access-rule-bound-directory-list/frontend-feature-evidence.md`
