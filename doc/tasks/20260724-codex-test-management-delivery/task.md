# Codex 自动测试管理开发任务

## Task Goal

根据已批准的系统设计与 BDD/TDD 验收文档，实现系统管理下的测试管理页面、测试管理员角色和权限、测试项与检查点 CRUD、租户级顺序/并行执行编排、Codex Runner 协议、执行结果和失败截图临时 artifact 展示，并完成可执行的验证。

## Milestones

- [x] 创建任务目录，加载开发规则和设计输入。
- [x] 核对现有 schema、迁移、系统模块测试模式、菜单权限和 Codex CLI 能力。
- [x] 先建立失败测试和迁移合同，新增测试管理表、角色、菜单和 admin 赋权。
- [x] 实现后端测试项、执行编排、Runner、artifact API 与服务测试。
- [x] 实现前端测试管理页面、API wrapper、权限展示和静态前端测试。
- [x] 运行后端、前端、迁移合同和可用验证；真实 E2E 因 Runner/租户凭据前置条件未确认而阻塞。
- [x] 完成任务验证、清理预检和收尾记录。

## Expected Verification

- 角色 `codex_test_admin` 与测试管理菜单/按钮权限通过迁移合同测试。
- 测试项支持自然语言方法、用户手写数据和任意检查点。
- 后端拒绝 Runner 离线、目标租户不可用、并行不安全和非法结果回写。
- Runner 协议支持注册、领取、心跳、结果回写和临时截图上传。
- 前端仅对测试管理员展示菜单和操作，展示绿色通过、红色失败、失败原因和截图。
- Maven 定向测试、前端静态合同测试和 `pnpm ts:check` 通过。
- 真实 Playwright E2E 仅在本机运行环境、测试租户和 Runner 前置条件确认后执行。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；使用持久化执行模型和受控 Runner 协议，不把 Codex 聊天输出或 API-only 调用视为测试成功。
- `是否存在临时补丁或绕过`：否。

## 输入文档

- `docs/system/frontend-design.md`
- `docs/system/backend-api-design.md`
- `docs/system/data-model.md`
- `docs/system/config-security-deployment.md`
- `docs/acceptance/bdd-scenarios.md`
- `docs/acceptance/tdd-plan.md`
- `docs/acceptance/e2e-plan.md`
- `docs/acceptance/test-data.md`

## Experience Gate

### 测试管理菜单、角色和租户套餐迁移

- Trigger: 新增 `system_menu`、`system_role`、`system_role_menu`、`system_user_role` 或合并 `system_tenant_package.menu_ids`。
- Preflight check: 以 `permission`、`tenant_id + code` 等稳定业务键定位已有记录；发布前只读核验目标环境菜单占用、角色 code 和 `menu_ids` 字段容量。
- Blocker: 角色依赖固定 ID、菜单只按偏好 ID 校验、`menu_ids` JSON 非法或容量不足、权限字符串比较没有显式兼容 collation。
- Verification: 本地迁移合同测试覆盖权限、角色、admin 绑定、JSON 合并和 `menu_ids` 扩容顺序；发布前运行 migration policy gate 并在授权环境执行只读 schema 查询。
- Forbidden action: 不得手工删除或改名环境既有角色/菜单，不得手工更新发布锁或迁移状态，不得用顺序执行替代被拒绝的并行执行。
- Evidence: `docs/release-build-preflight-lessons.md` 的 2026-07-06 菜单 ID、2026-07-19 角色 ID、2026-07-19 租户套餐 `menu_ids`、2026-07-22 权限菜单兼容门禁。

### PowerShell 与真实 E2E

- Trigger: PowerShell 编排、中文文本、Playwright、本机运行态和真实租户登录。
- Preflight check: 使用 UTF-8 读取/写入；真实 E2E 前确认本机 `8081/48081` 服务、测试租户、测试管理员身份、Runner token、Codex CLI、Playwright 和浏览器。
- Blocker: 真实 E2E 前必须确认本机数据库、Runner token、Codex CLI、Playwright 浏览器、目标测试租户和 Runner 本地凭据映射；缺任一项不得用 API-only 或 mock 成功替代。
- Verification: 所有文本写入使用 `apply_patch`，真实 E2E 记录服务端口、身份标签、测试数据标识、页面断言和敏感信息脱敏。
- Forbidden action: 不得使用 API-only 冒充 E2E，不得切换到远端环境、账号或租户，不得在日志中写入密码或 Runner token。
- Evidence: `docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`。
