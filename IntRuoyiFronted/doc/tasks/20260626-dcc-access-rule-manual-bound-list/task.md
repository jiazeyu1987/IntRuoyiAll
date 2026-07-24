# 任务：DCC 访问规则左侧仅显示手动保存目录

## 任务目标

- 保持 `DCC 访问规则` 页面左侧仍由 `getAccessRuleDirectories()` 驱动，但列表口径收紧为“通过访问规则页显式保存过的目录”。
- 继承/克隆了规则但不在手动保存列表中的目录，仍允许从树形新增入口进入维护，并以“未保存目录”草稿态展示。
- 不修改现有规则表格字段结构，不用前端根据规则内容推断已绑定状态，不引入 fallback、mock 成功或静默降级。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个 frontend 任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-batch-template-simulate-fill\task.md`
- 状态：`已完成`
- 处理：上一前端任务已完成，不阻塞本次 DCC 访问规则口径修正。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 页面继续遵循 IntPP 紧凑运维台样式，本次只改目录口径与草稿态契约，不做无关视觉重构。
  - 前端不得用本地规则集合、`changeReason`、父目录比较或任何 fallback 分支伪造“已绑定目录”。
  - 若执行真实登录/真实 E2E，第一条登录相关命令必须先运行 `node scripts/preflight/login-preflight.mjs ...`，且高风险真实验证前必须在 `execution-log.md` 先记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。左侧列表只认后端手动保存目录接口，不新增启发式兜底。
- `是否从根因和长期维护角度解决`：是。前端只消费后端显式来源建模，继续把“未进入左侧列表”的目录当作草稿态处理。
- `是否存在临时补丁或绕过`：否。不会用前端缓存或规则内容相等判断假装目录已绑定。

## BDD 场景

- `BDD: 左侧仅显示手动保存目录路径 -> Given 后端 access-rule-directories 只返回手动保存目录 When 页面加载完成 Then 左侧只显示这些目录的完整路径字符串。`
- `BDD: 继承目录进入页面时保持未保存目录态 -> Given 用户通过树形新增入口选择一个当前不在左侧列表中的目录 and 后端该目录已存在继承规则 When 页面完成切换 Then 右侧加载真实规则但标题区仍显示 未保存目录。`
- `BDD: 手动保存后目录进入左侧列表 -> Given 用户正在维护一个未保存目录草稿 When 点击保存规则成功 Then 页面刷新左侧列表并把当前目录切换为已绑定状态。`
- `BDD: 删除整组规则后目录从左侧消失 -> Given 当前目录属于左侧手动保存列表 When 用户删除该目录全部访问规则 Then 左侧列表刷新后不再显示该目录。`
- `BDD: 已绑定目录误判回归继续受保护 -> Given 当前目录已经在左侧手动保存列表中 When 页面完成初始化或刷新规则 Then 标题区不得错误显示 未保存目录。`

## 里程碑

1. M1：创建任务台账并补静态 RED 骨架。`COMPLETED`
2. M2：扩展访问规则静态契约，锁定手动目录列表与继承目录草稿态。`COMPLETED`
3. M3：按后端新语义验证前端现有逻辑，必要时最小修复目录/草稿/刷新行为。`COMPLETED`
4. M4：运行 GREEN 静态验证、类型检查与真实登录/E2E 验收并补齐 evidence。`COMPLETED`

## 预期验证

- `node tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js`
- `node tests/e2e/dcc-access-rule-save-validation-static.spec.js`
- `node tests/e2e/dcc-access-rule-header-context-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-dcc-access-rule-manual-bound-list\frontend-feature-evidence.md`

## 最终验证结果

- PASS：`node tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js`
- PASS：`node tests/e2e/dcc-access-rule-save-validation-static.spec.js`
- PASS：`node tests/e2e/dcc-access-rule-header-context-static.spec.js`
- PASS：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- PASS：`node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /dcc/controlled-file/access-rules --target-text 访问规则 --timeout 90000`
- PASS：真实登录后抓取页面实际 `GET /admin-api/dcc/directories/access-rule-directories` 响应，当前返回 `code=0`、`data=[]`，左侧同步渲染 `0` 项，证明页面已仅消费手动保存目录列表。

## Cleanup Keep

- `doc/tasks/20260626-dcc-access-rule-manual-bound-list/task.md`
- `doc/tasks/20260626-dcc-access-rule-manual-bound-list/execution-log.md`
- `doc/tasks/20260626-dcc-access-rule-manual-bound-list/frontend-feature-evidence.md`
