# Codex Runner 本机配置与作废测试执行

## Task Goal

- 使用当前电脑可用的 Codex CLI/运行环境配置当前系统的 Codex Runner。
- 配置完成后，从系统“测试管理”运行目标测试项“作废测试”。
- 记录 Runner 前置条件、执行批次、结果、失败截图/检查点证据和必要清理。

## Milestones

- [x] 记录 BDD/TDD、Runner/E2E/登录/本地运行门禁。
- [x] 检查本机前端、后端、Codex CLI、Playwright 和 Runner 配置。
- [x] 配置当前系统 Codex Runner 使用当前电脑 Codex 环境。
- [ ] 通过真实测试管理页面运行“作废测试”。
- [x] 核验已完成配置结果、记录阻塞证据。

## Expected Verification

- `codex --version` 或等效 Codex CLI 可用性检查。
- `node scripts/codex-test-runner.mjs ...` Runner 可注册/领取/执行/回写。
- 本机前端 `http://127.0.0.1:8081` 可访问。
- 本机后端 `http://127.0.0.1:48081/actuator/health` 为 UP。
- 测试管理“作废测试”执行批次完成并记录检查点结果。

## Current Status

blocked

## Blocker

- 当前本机真实测试管理数据不存在名为“作废测试”的测试项。
- 真实页面按“作废测试”和“作废”搜索均返回 `total=0`；只读 DB 核对 `system_codex_test_case` 也没有任何名称、方法或测试数据包含“作废”的记录。
- 当前 `system_codex_test_case` 只有 1 个启用项：`排产工单手动重排 881MO093613/881MO093615`。按门禁要求不能改跑其它项、不能自动创建占位项，也不能把 Runner 空领取当作执行成功。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，优先使用系统已有 Runner/API/页面入口和当前机器真实 Codex CLI。
- `是否存在临时补丁或绕过`：否；若 Runner 前置缺失则 fail fast 记录 blocker。

## 经验门禁

- Codex Runner 自动测试门禁：执行前确认本机前端/后端入口、目标测试租户、测试管理员账号、Runner token、Codex CLI、Playwright 浏览器、Runner 本地凭据映射和测试数据清理责任。
- Codex Runner 目标测试项存在性门禁：点击执行前必须按可见业务名称定位目标项；目标项不存在、禁用、租户不匹配或只存在历史文档时必须阻塞。
- 本地运行门禁：`int_main` 使用前端 `8081`、后端 `48081`，不得换端口或强杀未知进程。
- 登录访问门禁：默认使用本机 `芋道源码/admin` 身份标签，不记录密码；不得切换到远端环境。
- Element Plus 表格门禁：页面执行时按可见业务文本“作废测试”定位目标项，不用数组下标或 API-only 替代。
- 数据库门禁：如需只读核验 schema/状态，先核对表结构；不得直接 SQL 写入测试结果或伪造通过。
