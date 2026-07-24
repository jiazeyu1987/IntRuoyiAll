# Task: 工艺路线基础信息 Tab 调整

## 任务目标

- 将工艺路线编辑页顶部基础字段移动为独立“基础信息”页签。
- 页签顺序固定为：组成工序、基础信息、流转关系图、关联产品。
- 保留页面顶部标题、返回列表、保存按钮，以及现有保存接口、校验和业务逻辑。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；命令输出显式 UTF-8，中文文件读写使用 UTF-8 aware runtime 或 apply_patch。
- 项目经验索引：已读取 `docs/experience-index.md`；本任务命中 PowerShell、前端页面样式、BDD/TDD 与真实 E2E 登录门禁。
- 前端样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；新增 Tab 沿用现有 Element Plus 表单和操作台风格，不做额外视觉重设计。
- 前端功能交付：已读取 frontend-feature-delivery 与 evidence contract；保留既有 API、路由、状态归属和错误暴露。
- 真实 E2E：执行真实登录/页面验证前必须读取 `docs/login-access.md` 并先跑登录 preflight；未通过则记录 blocker，不绕过。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，通过统一 Tab 结构组织基础信息，不改变接口契约。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 基础信息作为独立页签 -> Given 用户打开工艺路线编辑页 / When 查看页签 / Then 基础信息作为独立 Tab 显示在组成工序和流转关系图之间。
- BDD: 顶部保存保留基础信息保存能力 -> Given 用户修改基础信息 / When 点击页面顶部保存 / Then 仍调用原路线保存接口并保留原校验行为。

## 里程碑

- [completed] M1：创建任务记录并补 RED 静态测试。
- [completed] M2：移动基础字段到新增基础信息 Tab。
- [completed] M3：运行静态测试和 TypeScript 校验。
- [completed] M4：记录验证证据和 closeout preview。
- [completed] M5：完成真实页面复验并纳入前后端独立提交。

## 预期验证

- `node tests/e2e/mes-route-basic-info-tab-static.spec.js`
- `pnpm ts:check`
- 真实路径验证：登录 preflight 后，用测试租户从 `http://localhost:8081` 打开工艺路线编辑页，确认 Tab 顺序、基础字段位置、顶部保存按钮位置与原保存行为。

## 当前状态

COMPLETED：基础信息 Tab 已按计划接入；静态测试、TypeScript 校验、官方登录 preflight 和测试租户真实只读 E2E 均通过。真实页面确认页签顺序为组成工序、基础信息、流转关系图、排产配置、批记录配置、关联产品，基础字段和顶部保存按钮可见，且未产生 MES 写请求。

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260709-route-basic-info-tab/frontend-feature-evidence.md

## 验证结果

- RED: `node tests/e2e/mes-route-basic-info-tab-static.spec.js` -> FAIL，缺少 `basic` Tab 类型和基础信息 Tab。
- GREEN: `node tests/e2e/mes-route-basic-info-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-edit-default-flow-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-edit-page-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-route-basic-info-tab/frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-route-basic-info-tab --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
- BLOCKER: `login-preflight.mjs` with default Playwright headless shell -> FAIL，`Invalid file descriptor to ICU data received`。
- BLOCKER: `login-preflight.mjs` with `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH=C:\Program Files\Google\Chrome\Application\chrome.exe` -> FAIL，等待 `/system/auth/login` 响应超时。
- BLOCKER: commit -> 当前混合工作区存在大量既有后端改动，且前端目标文件包含本轮开始前的重叠未提交改动，无法按规则只提交本任务直接产生的整文件改动。
- GREEN: 官方 `login-preflight.mjs` 使用系统 Chrome重新执行 -> PASS，测试租户 `aoteman` 已进入本机 `/mes/pro/route`。
- GREEN: `node tests/e2e/mes-route-basic-info-tab-real.e2e.js` -> PASS，路线 `RT000017` 的基础信息页签顺序、字段和顶部保存按钮均通过真实页面验证，MES 写请求为 0。
- GREEN: commit-boundary -> PASS，前后端改动已按业务仓库独立暂存验证。
