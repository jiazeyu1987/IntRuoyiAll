# 20260602 DCC 受控预览 viewer token 配置缺失

## Task Goal

按用户明确批准的临时范围，修复访问 DCC 受控文件详情页时显示 `DCC viewer token config is missing: yudao.dcc.viewer-token.hmac-secret` 的问题：在后端 viewer token 服务内写死默认 HMAC secret，当运行时配置缺失时回退到该固定密钥，先恢复受控预览/下载链路。

## Milestones

- [x] M1: 记录任务、处理上一个后端任务未完成状态，并确认报错来自 viewer token HMAC secret 缺失。
- [x] M2: 定位发布配置、运行时 `.env` 与后端 token 服务的配置契约。
- [x] M3: 补充或运行现有回归测试，证明发布包必须显式传入 `DCC_VIEWER_TOKEN_HMAC_SECRET`。
- [x] M4: 以严格 TDD 方式补充“缺配置时使用固定密钥”的回归测试并完成最小代码修复。
- [x] M5: 验证 viewer token 服务在缺配置时仍能签发和校验 token，并记录临时方案风险与回滚路径。

## Expected Verification

- RED: 新增 token 服务回归测试，在 `hmacSecret` 为空时调用 `issue()/verify()` 仍按现状抛出 `CONTROLLED_FILE_VIEWER_TOKEN_CONFIG_MISSING`，证明问题存在。
- GREEN: 新增 token 服务回归测试通过，在 `hmacSecret` 为空时使用固定密钥成功签发并校验 token。
- REGRESSION: 现有 preview token 相关测试继续通过，证明默认密钥不会破坏已有签发/校验上下文。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：是。用户于 2026-06-02 明确要求“密钥帮我写死,现在不需要那么严格”；触发条件为运行时未提供 `yudao.dcc.viewer-token.hmac-secret` 或长度不足 32；风险是不同环境会共享固定 viewer token 密钥，需在恢复正式配置后移除。
- `是否从根因和长期维护角度解决`：否。该方案不替代发布配置治理，只用于当前临时恢复预览链路；正式方案仍应恢复显式运行时配置并移除固定密钥。
- `是否存在临时补丁或绕过`：是。范围仅限 `DccViewerTokenService` 默认密钥；回滚/移除策略为恢复显式配置后删除默认密钥常量并恢复 fail-fast 校验。

## Current Status

completed

## Findings

- 用户截图中的报错来自后端 `DccViewerTokenService.requireConfigured()`：`yudao.dcc.viewer-token.hmac-secret` 为空或长度小于 32 时 fail-fast。
- 本地发布契约已经包含 `DCC_VIEWER_TOKEN_HMAC_SECRET` 到 `--yudao.dcc.viewer-token.hmac-secret=${DCC_VIEWER_TOKEN_HMAC_SECRET}` 的传递。
- 测试服当前 `/opt/intruoyi/runtime/.env` 中 `DCC_VIEWER_TOKEN_HMAC_SECRET` 长度为 50；当前 `intruoyi-backend` Java 进程参数中的 viewer token secret 长度也为 50。
- 当前后端镜像为 `intruoyi-backend:26-06-02_00-12-30`，容器已运行约 39 分钟；近 45 分钟日志未再出现 `DCC viewer token config is missing`。
- 用户刷新后浏览器控制台的新错误为 `Controlled file does not exist`，栈在前端 `getControlledFile()` 调用 `GET /admin-api/dcc/controlled-files/2054545668044048046` 阶段。
- 本地代码确认 `DccControlledFileQueryServiceImpl.getControlledFile(userId, id)` 只有在 `controlledFileMapper.selectById(id)` 返回 null 时抛出 `CONTROLLED_FILE_NOT_EXISTS`；如果是权限问题会抛 `CONTROLLED_FILE_ACCESS_DENIED`。因此当前问题指向目标 ID 在当前数据源/租户过滤下不存在，而不是下载或预览 token 逻辑。
- 用户在当前对话中明确批准临时方案：将 viewer token 密钥写死，暂时放宽缺配置时的严格 fail-fast。
- 已在 `DccViewerTokenService` 内加入固定默认密钥 `dcc-viewer-token-default-secret-20260602`；当运行时密钥为空或长度不足 32 时，改用该固定值签发和校验 token。
- 新增 `DccViewerTokenServiceDefaultSecretTest`，并回归 `DccControlledPreviewAccessServiceTest`，本地测试通过。

## Blocker

- 测试服真实页面、真实数据文件 ID `2054545668044048046` 是否可打开，仍需登录态联调或数据库确认；当前变更只能解决 viewer token 缺配置问题，不能代替数据存在性校验。

## Final Verification Result

- RED: `mvn -pl yudao-module-dcc -Dtest=DccViewerTokenServiceDefaultSecretTest test` -> FAIL，修复前空配置仍抛 `CONTROLLED_FILE_VIEWER_TOKEN_CONFIG_MISSING`。
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccViewerTokenServiceDefaultSecretTest,DccControlledPreviewAccessServiceTest" test` -> PASS。
- 当前未执行测试服登录态页面复测；若页面仍报错，后续需继续排查真实文件 ID 是否存在。
