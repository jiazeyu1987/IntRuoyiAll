# 任务：DCC 文件产品名称识别

## 任务目标

在 DCC 受控文件详情页的“产品名称”位置增加“识别”按钮。用户点击后，后端使用当前运行环境配置的 Codex CLI 读取当前受控文件内容，识别文件对应的产品名称，并把识别结果写入该 DCC 文件的 `product_name` 字段，使数据库和前端详情页显示同步更新。

## Previous Task Check

- 当前后端工作区存在其他未完成发布方案任务文件，但本任务只改 DCC 模块源码、DCC 测试和本任务目录。
- 为避免混入既有未提交改动，本任务提交时只暂存本任务直接产生的文件。

## BDD 场景

- BDD: 识别并保存产品名称 -> Given DCC 详情页打开一份已有受控文件 / When 用户点击产品名称旁的“识别”按钮 / Then 后端使用配置的 Codex CLI 识别源文件内容并把返回的产品名称保存到当前文件 `product_name`，前端刷新后显示识别结果。
- BDD: 超管可执行识别 -> Given 用户具备 `super_admin` 角色 / When 调用 DCC 产品名称识别接口 / Then 后端应允许请求进入同一 Codex CLI 识别与持久化流程。
- BDD: Codex CLI 参数兼容当前版本 -> Given 当前配置的 Codex CLI 只接受 `--ask-for-approval` 作为顶层参数 / When 后端执行产品名称识别 / Then 后端必须把 `--ask-for-approval never` 放在 `exec` 前，避免 CLI 因未知子命令参数退出。
- BDD: 缺少 Codex CLI 配置必须失败 -> Given 运行环境未配置 Codex CLI 命令 / When 用户点击“识别” / Then 后端 fail-fast 返回明确错误，不得写入空值、默认值或模拟成功。
- BDD: 识别结果为空必须失败 -> Given Codex CLI 没有返回有效产品名称 / When 后端处理识别输出 / Then 请求失败且数据库不更新。

## 里程碑

- [x] M1：建立任务文档和验收标准。
- [x] M2：新增 RED 测试覆盖后端接口、服务和前端入口。
- [x] M3：实现后端 Codex CLI 识别、文件读取和产品名称持久化。
- [x] M4：实现前端详情页按钮、loading/error 状态和刷新展示。
- [x] M5：运行验证、记录证据、收尾预览并提交。
- [x] M6：修复当前 Codex CLI 参数顺序回归并重新验证本地运行包。

## 预期验证

- `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFileProductNameRecognitionServiceTest,DccControlledFileProductNameRecognitionControllerTest,DccControlledFileMetadataUpdateServiceTest test`
- `node scripts/dcc-controlled-file-product-name-recognition.test.mjs`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260605-dcc-product-name-recognition/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260605-dcc-product-name-recognition/frontend-feature-evidence.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺 Codex CLI、缺文件、CLI 失败或识别结果为空时直接失败，不写默认值。
- `是否从根因和长期维护角度解决`：是。通过受控文件详情 API 增加明确识别动作，由后端统一读取文件、调用 CLI、校验输出和持久化，不在前端绕过。
- `是否存在临时补丁或绕过`：否。不使用 mock 结果、不从文件名猜测替代 CLI 识别。

## 当前状态

completed

## Current Status

completed

## 当前证据

- RED：后端目标测试 testCompile 失败，原因是产品名称识别接口、VO、服务、Codex CLI 客户端和错误码尚未实现。
- GREEN：新增目标测试 6 项通过；带原元数据更新回归测试共 11 项通过；后端 API evidence 自检通过。
- REGRESSION：真实 Playwright 验证发现 `super_admin` 用户前端不可见按钮，且后端当前角色门禁只认 `doc_control`，进入权限修复闭环。
- GREEN：恢复后完成后端授权修复；后端目标测试 14 项通过，`doc_control` 与 `super_admin` 均走同一正式识别/元数据更新授权路径。
- REGRESSION：用户点击识别后 Codex CLI 返回 `unexpected argument '--ask-for-approval'`，确认当前实现把顶层 approval 参数放在了 `exec` 子命令之后。
- GREEN：Codex CLI 参数顺序修复后，客户端回归测试和识别相关后端测试 15 项通过；本地 48081 已替换新 runtime jar 并健康检查 `UP`。

## 阻塞记录

- BLOCKED：2026-06-05 用户将当前优先级切换为展厅产品附件发布与 Win7 默认软件打开方案，本轮不继续修改 DCC 产品名称识别任务源码。
- Impact：DCC 产品名称识别仍停留在已发现的 `super_admin` 可见性/权限修复闭环，后续恢复该任务时需从该回归点继续。
- RESOLVED：2026-06-05 已恢复并修复 `super_admin` 授权回归，当前任务完成。

## Cleanup Keep

- doc/tasks/20260605-dcc-product-name-recognition/bug-regression-evidence.md
