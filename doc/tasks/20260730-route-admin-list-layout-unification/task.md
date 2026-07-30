# 工艺路线列表统一 admin 布局

## Task Goal

将工艺路线列表的默认字段布局统一为“芋道源码 / admin”截图样式，通过升级列表配置 key 使现有账号的旧个人配置失效；继续保留“显示字段”和现有角色权限控制，不向普通账号开放管理员操作。

## Milestones

1. `completed`：建立任务文档，核对并行改动、经验门禁和现有列配置契约。
2. `completed`：编写 RED 静态合同，锁定统一字段集合和权限边界。
3. `completed`：实现 admin 默认字段布局并升级列表配置 key，保留后续个人字段配置能力。
4. `completed`：运行目标静态合同、相邻列配置合同、TypeScript 类型检查和 Chrome/Edge 真实只读 E2E。
5. `ready_for_closeout`：完成经验沉淀、cleanup、选择性提交、远端合并和复验；最终推送当前被 GitHub HTTPS 连接重置阻塞。

## Expected Verification

- `node tests/e2e/mes-route-admin-list-layout-static.spec.js`
- 工艺路线相邻静态合同
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence E:\IntRuoyi\doc\tasks\20260730-route-admin-list-layout-unification\frontend-feature-evidence.md`
- `git diff --check -- <task-owned-files>`

## Experience Gate

- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：新增任务专用最小静态合同，稳定证明旧配置 key 失效、默认字段集合统一且显示字段能力仍保留。
- 命中现有权限边界：布局统一不得删除或绕过 `v-hasPermi`，普通用户不能因视觉统一获得管理员操作。
- 命中 `docs/powershell-memory.md#共享分支并发基线提交门禁` 与 `#同文件并行改动选择性暂存门禁`：目标文件存在并行版本弹窗改动，提交前必须核对 hunk 并只暂存本任务改动。

## Shared Workspace Boundary

- `IntRuoyiFronted/src/views/mes/pro/route/index.vue` 当前包含并行“路线版本草稿显示”任务改动；本任务只修改列表列布局区域，不覆盖版本弹窗区域。
- 提交时必须选择性暂存本任务 hunk，保留并行任务改动。
- 脏工作区基线提交：`67282a86`，包含本任务开始前的 36 个既有并行改动文件；本任务文档未进入该提交。

## Current Status

ready_for_closeout

## Closeout Blocker

- `git push origin int_main` 连续失败，错误为 `Recv failure: Connection was reset`；`git ls-remote --heads origin int_main` 也因同一连接重置失败。
- 影响：本地实现、验证、合并和收尾提交已完成，但当前分支仍领先 `origin/int_main`，任务不能标记为最终 completed。
- 后续动作：网络恢复后重新执行 `git push origin int_main`，再用 `git status --short --branch` 确认不再 ahead。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；从该列表的个性化列配置根因取消跨账号差异，而不是用浏览器缓存清理掩盖。
- `是否存在临时补丁或绕过`：否。
