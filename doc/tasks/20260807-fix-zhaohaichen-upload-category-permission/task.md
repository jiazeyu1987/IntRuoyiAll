# 修复 zhaohaichen 文件上传分类选择权限

## Task Goal

修复 `tenant_id=1/zhaohaichen` 进入 `文控中心 > 文件上传` 后提示“文件分类候选加载失败：没有该操作权限”的问题；将正式修复融合到本地 `int_main`，并使用本机账号从真实前端验证，同时继续禁止 DCC 文件下载。

## Milestones

- [x] M1 记录用户截图、目标环境、账号和预期行为。
- [x] M2 通过真实页面/接口/数据库只读证据复现并定位权限缺口。
- [x] M3 记录 RED，实施最小正式修复。
- [x] M4 将本任务代码和测试融合到本地 `int_main`，更新本地运行态并通过真实用户路径复验。
- [x] M5 完成证据校验、经验沉淀和任务清理。

## Expected Verification

- 本机 `zhaohaichen` 从 `http://localhost:8081` 真实登录并进入文件上传页后，文件分类候选请求成功，不再出现“没有该操作权限”。
- 页面文件分类候选来自正式 taxonomy 数据，并只包含启用节点；文件类别、提交目录和审批阶段权限继续走各自既有正式链路。
- `wenkong_no_download` 不获得目录管理、访问规则管理、类别管理或显式下载权限。
- DCC 下载的用户、角色、岗位、部门和动态授权放行来源继续为 0。
- 缺陷证据 validator、聚焦回归和 cleanup preview/apply 通过。

## Current Status

completed

## Milestone Evidence

- M2: 测试服现有 `zhaohaichen` 会话请求 `GET /dcc/file-type-taxonomies` 返回业务码 `403 / 没有该操作权限`；同会话请求 `GET /dcc/file-categories` 返回业务码 `0`，排除账号失效和整个 DCC 查询链路不可用。
- M2: 源码确认上传页调用分类管理列表，管理列表与 taxonomy CRUD 共用 `dcc:controlled-file:category:manage`。
- M3: 新增 `/dcc/file-type-taxonomies/upload-options`，只接受 `dcc:controlled-file:submit`，仅返回启用 taxonomy 节点；管理列表与 CRUD 继续要求 `category:manage`。
- M3: 上传页改用上传专用候选 API，相关后端、前端聚焦回归和 TypeScript 检查全部通过。
- M4: 用户将范围调整为“融合到本地 `int_main`，然后用本地 `zhaohaichen` 账号测试”；测试服发布已取消。
- M4: 用户已明确授权本任务本地 Git 集成；只允许暂存和提交本任务 6 个代码/测试文件，不得包含其他并发改动，不推送远端。
- M4: 本任务 6 个代码/测试文件已提交到本地 `int_main`，提交 `068d7983e`；提交后这些路径 clean，未推送远端。
- M4: 本机后端运行 Jar 以既有 Jar 为基底，只替换本任务 Controller class；新 Jar SHA-256 为 `D53C6D14EE8DD46D3350842DD176D4F55C62631F59DA8B442EB7FE84C78B6FF0`，`48081` PID `59012`，health `UP`。
- M4: Playwright 使用本机 `tenant_id=1/zhaohaichen` 真实登录文件上传页；`GET /dcc/file-type-taxonomies/upload-options` HTTP 200、业务码 `0`，返回项全部 `active=true`；页面无“文件分类候选加载失败/没有该操作权限”，并成功展开和选择三级文件分类。
- M4 residual: 共享前端的无关并发文件 `TeamLeaderWorkbenchPage.vue` 存在 PostCSS 错误并触发 Vite 开发遮罩；本任务未修改或回退该文件。遮罩证据单独记录，关闭开发遮罩后本任务分类交互通过，不能据此宣称共享前端全局无错误。
- M5: 已将共享 Vite 开发遮罩的目标链路验收边界沉淀到 `docs/e2e-rules.md`；现有经验索引已包含 Vite/E2E 路由，未改动并发任务正在修改的索引文件。
- M5: `task-closeout-cleanup` preview/apply 均无 blocked/warning；已删除本任务临时复现、构建和 evidence 文件，只保留三份正式任务记录、Playwright 截图和当前运行 Jar。
- M5: 清理后 `48081` 仍由 PID `59012` 运行保留 Jar，health `UP`；本地融合提交 `068d7983e` 的 6 个代码/测试文件保持 clean。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；拆分上传运行态只读候选接口与 taxonomy CRUD 管理接口，按后端正式鉴权固定最小权限。
- `是否存在临时补丁或绕过`：否。

## Environment Scope

- 最终验证环境：本机 `http://localhost:8081` / `http://127.0.0.1:48081`。
- 目标租户/账号：`tenant_id=1/zhaohaichen`。
- 不再执行测试服构建、发布、重启或数据写入。
- 本地写入范围仅限本任务 Git 提交、隔离构建产物、本机后端运行态更新和只读页面验证；不修改角色、菜单、业务数据或下载授权。
- 不记录密码、token、私钥或数据库连接密钥。

## Experience Gate Summary

- 命中 `docs/frontend-development.md#DCC-上传类别权限投影门禁`：必须区分文件分类 taxonomy、正式 DCC 文件类别 category 和类别权限阶段，禁止用菜单权限或隐藏错误冒充分类候选可用。
- 命中 `docs/database-rules.md#DCC-菜单恢复与无下载角色隔离门禁`：禁止给 `wenkong_no_download` 增加类别管理、目录管理或访问规则管理权限以绕过候选接口 403。
- 命中 `docs/e2e-rules.md` 真实路径门禁：最终必须由 Playwright 走目标环境真实登录页面验证，API 仅作只读复现和最终辅助核验。
- 现有上传页使用管理型 `/dcc/file-type-taxonomies` GET；正式修复应把上传候选查询与 taxonomy CRUD 管理权限拆分。

## Cleanup Keep

- doc/tasks/20260807-fix-zhaohaichen-upload-category-permission/task.md
- doc/tasks/20260807-fix-zhaohaichen-upload-category-permission/execution-log.md
- doc/tasks/20260807-fix-zhaohaichen-upload-category-permission/verification-report.md
- output/playwright/20260807-fix-zhaohaichen-upload-category-permission/local-zhaohaichen-upload-taxonomy-selected.png
- output/runtime/int_main/backend-runtime-control-20260807-upload-taxonomy-permission.jar
