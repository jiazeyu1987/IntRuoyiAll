# 任务：业务前端显示当前发布版本与变更说明

## 目标

修复测试服、正式服、备份服业务前端发布后不显示版本号和版本变更信息的问题。发布后的业务页面必须能在左下角看到当前运行版本，并能打开本次发布的变更说明。

## 上一任务检查

- 上一相关任务：`doc/tasks/20260624-showroom-menu-title-placeholder/task.md`
- 状态：已完成。
- 处理：本任务只新增全局发布信息展示，不改动展厅菜单任务文件。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 页面 UI 使用蓝色/中性色、紧凑操作台风格。
  - 控件尺寸固定，避免遮挡和布局跳动。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
  - 发布包 manifest 必须携带 releaseTag、sourceRepos、changeSet。
  - 发布后页面证据必须能证明当前运行版本。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；静态发布信息缺失时页面直接显示“版本信息未生成”，不伪造版本。
- `是否从根因和长期维护角度解决`：是；业务前端读取发布脚本生成的静态发布信息文件，随发布包进入镜像。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 业务前端版本可见 -> Given 发布脚本在前端 dist 生成 release-info.json When 用户打开测试服/正式服/备份服业务前端 Then 左下角显示当前 releaseTag。`
- `BDD: 业务前端变更可见 -> Given release-info.json 包含 changeSet When 用户点击查看变更 Then 弹窗显示摘要、变更项和源码提交。`
- `BDD: 发布信息缺失暴露 -> Given release-info.json 未生成或不可读 When 用户打开业务前端 Then 页面显示版本信息未生成，不静默隐藏。`

## 里程碑

1. 写入任务文档与 RED 静态契约测试。`COMPLETED`
2. 实现业务前端发布信息浮层和弹窗。`COMPLETED`
3. 运行静态测试与类型检查。`COMPLETED`
4. 提交本任务业务前端改动。`COMPLETED`

## 预期验证

- `node scripts/release-info-dock-contract.test.mjs` 通过。
- `pnpm ts:check` 通过。

## 当前状态

COMPLETED：业务前端已挂载全局版本浮层，读取 `/release-info.json` 显示当前 releaseTag，并通过“查看变更”弹窗展示 changeSet 与 sourceRepos；静态契约测试、类型检查和 `build:test` 已通过。

## 验证证据

- `node scripts/release-info-dock-contract.test.mjs` -> RED：`App.vue` 未挂载 `ReleaseInfoDock`，组件文件不存在。
- `node scripts/release-info-dock-contract.test.mjs` -> GREEN：2 tests passed。
- `pnpm ts:check` -> FAIL：Node 默认堆内存 OOM，未出现 TypeScript 诊断。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> GREEN。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm build:test` -> GREEN，`dist-test` 构建成功。
- `Select-String dist-test\assets\*.js release-info.json` -> GREEN，构建产物包含发布信息读取入口。
