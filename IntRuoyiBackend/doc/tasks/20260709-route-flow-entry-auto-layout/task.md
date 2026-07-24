# Task: 工艺路线流转关系图进入时自动布局

## 任务目标

- 用户进入工艺路线编辑页“流转关系图”Tab 时，默认触发一次现有“自动布局”动作。
- 保留现有手动“自动布局”按钮、保存关系图、刷新和适配屏幕逻辑。
- 不改后端接口、数据库、权限、菜单或流转关系图保存契约。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；命令输出显式 UTF-8，中文文件读写使用 UTF-8 aware runtime 或 apply_patch。
- 项目经验索引：已读取 `docs/experience-index.md`；本任务命中 PowerShell、前端页面样式与 BDD/TDD 门禁。
- 前端样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本任务只调整行为触发，不新增视觉样式。
- 前端功能交付：已读取 frontend-feature-delivery 与 evidence contract；保留既有 API、状态归属和错误暴露。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，复用现有 `handleAutoLayout` 行为，通过 Tab 进入事件触发，不复制布局算法。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 进入流转关系图自动布局 -> Given 用户打开工艺路线编辑页且默认进入流转关系图 / When 流转关系图加载完成 / Then 系统自动执行一次现有自动布局动作。
- BDD: 切回流转关系图自动布局 -> Given 用户从其它页签切回流转关系图 / When 该页签被激活 / Then 系统再次按本次进入触发一次自动布局。

## 里程碑

- [completed] M1：创建任务记录并补 RED 静态测试。
- [completed] M2：实现进入流转关系图自动触发布局。
- [completed] M3：运行静态测试和 TypeScript 校验。
- [completed] M4：记录验证证据并纳入独立前后端提交。

## 预期验证

- `node tests/e2e/mes-route-flow-entry-auto-layout-static.spec.js`
- `node tests/e2e/mes-route-flow-graph-static.spec.js`
- `node tests/e2e/mes-route-edit-default-flow-tab-static.spec.js`
- `pnpm ts:check`

## 当前状态

COMPLETED：进入“流转关系图”Tab 时已触发现有自动布局入口；相关静态回归、全部当前变更静态测试和 TypeScript 校验均通过，重叠文件已在统一提交边界内完成核对。

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260709-route-flow-entry-auto-layout/frontend-feature-evidence.md

## 验证结果

- RED: `node tests/e2e/mes-route-flow-entry-auto-layout-static.spec.js` -> FAIL，缺少 `@tab-change="handleRouteTabChange"`，证明当前进入 flow Tab 不会触发布局。
- GREEN: `node tests/e2e/mes-route-flow-entry-auto-layout-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-graph-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-edit-default-flow-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-basic-info-tab-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- BLOCKER: commit -> 当前混合工作区存在大量既有改动，目标前端文件也存在本轮前未提交重叠改动；本轮不提交，避免夹带其它任务或用户改动。
- GREEN: changed-static-regression -> PASS，当前 14 个变更静态测试全部通过。
- GREEN: commit-boundary -> PASS，重叠的路线编辑与流转图文件已在统一回归后纳入独立前端提交。
