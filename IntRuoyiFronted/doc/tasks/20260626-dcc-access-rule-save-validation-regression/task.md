# 任务：DCC 访问规则保存校验误报回归修复

## 任务目标

- 修复 `/dcc/controlled-file/access-rules` 页面在 `质量管理/1. QMS documents` 等已有目录点击 `保存规则` 时误报 `请完善授权对象后再保存` 的前端回归。
- 保持真实保存门禁：只有新增或编辑后的规则行确实缺少 `subjectType` / `subjectId` 时才阻止保存。
- 不修改后端接口契约、不放宽业务校验、不通过忽略非法行或吞掉异常掩盖页面状态误判。

## 当前状态

已完成。

## 上一任务检查

- 上一个 frontend 任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-batch-template-preview\task.md`
- 状态：`已完成`
- 处理说明：上一前端任务已完成，不阻塞本次缺陷修复。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 本轮优先执行前端源码、静态回归与只读数据库核对，不做真实写入型 E2E。
  - 若进入真实页面复现，第一条登录相关命令必须先跑 `node scripts/preflight/login-preflight.mjs ...`，并在 `execution-log.md` 先记录 `GREEN: experience-preflight -> PASS`。
  - 页面样式继续遵循 IntPP 紧凑运维台风格，只修交互状态和保存判定，不做无关视觉改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仍保留真实保存前校验，只修复误判。
- `是否从根因和长期维护角度解决`：是。统一目录绑定状态、草稿态和保存校验的判断口径，避免字符串/数字 ID 或草稿态串扰造成回归。
- `是否存在临时补丁或绕过`：否。不会简单删除告警，也不会让非法规则行继续保存。

## BDD 场景

- `BDD: 已绑定目录不应显示未保存目录状态 -> Given 后端已返回当前目录的访问规则且该目录属于已绑定目录列表 / When 页面完成初始化 / Then 标题区不得显示“未保存目录”，左侧对应目录项应保持选中。`
- `BDD: 已有完整规则点击保存不应误报授权对象缺失 -> Given 当前目录所有规则都带真实 subjectType 与 subjectId / When 用户直接点击保存规则 / Then 页面不得提示“请完善授权对象后再保存”。`
- `BDD: 新增空白规则仍阻止保存 -> Given 用户新增一条默认规则且没有选择授权对象 / When 点击保存规则 / Then 页面继续提示“请完善授权对象后再保存”，防止提交空规则。`

## 里程碑

1. M1：创建任务文档、记录请求和只读复现线索。
2. M2：补 RED 静态回归合同，锁定已绑定目录与保存校验口径。
3. M3：最小修复访问规则页的目录/草稿/保存判定逻辑。
4. M4：运行 GREEN 静态验证、类型检查与缺陷证据校验。

## 预期验证

- `node tests/e2e/dcc-access-rule-save-validation-static.spec.js`
- `node tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js`
- `node tests/e2e/dcc-access-rule-header-context-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-dcc-access-rule-save-validation-regression\bug-regression-evidence.md`

## 最终验证结果

- `node tests/e2e/dcc-access-rule-save-validation-static.spec.js` -> PASS
- `node tests/e2e/dcc-access-rule-header-context-static.spec.js` -> PASS
- 真实页面只读复现 -> PASS，`芋道源码/admin` 下点击 `新增规则` 后再点 `保存规则`，页面提示 `第 18 条规则未选择授权对象，请先选择授权对象或删除该规则后再保存`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence ...` -> BLOCKED，当前证据模板仍缺少该校验器要求的 `Expected / RED: / GREEN: / Verification` 固定段落
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> BLOCKED，当前前端仓存在与本任务无关的 Pinia store / `pinia-plugin-persistedstate` 类型错误
