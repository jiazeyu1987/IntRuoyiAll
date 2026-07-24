# 任务：电子签名 portal 路径改为子页签

## 任务目标

同步后端电子签名 portal adapter 返回路径，使 DCC 文件签名、用户授权和 eDHR 批记录签名入口返回 `/signature-governance/<tab>` 子页签路径，而不是旧的独立菜单路径或 `?tab=` 主入口。保持现有 API 响应字段结构不变。

## 里程碑

- [x] M1：读取后端交付契约与前端任务范围。
- [x] M2：先改 RED 单测，要求 portal 返回子页签路径。
- [x] M3：实现 DCC/eDHR adapter 路径更新并同步 controller/service 测试。
- [x] M4：运行后端 portal 相关单测。

## 预期验证

- `mvn -pl yudao-module-dcc -Dtest=DccSignatureGovernancePortalAdapterTest,SignatureGovernancePortalServiceTest,SignatureGovernanceControllerTest test`
- `mvn -pl yudao-module-mes -Dtest=MesEdhrSignatureGovernancePortalAdapterTest test`

## 当前状态

已完成。DCC 与 eDHR portal adapter 已返回电子签名子页签路径，controller/service 相关单测通过。

## Current Status

completed

## 经验门禁

- `docs/server-access.md`：本次只改本机源码与单测，不操作测试服、备份服或正式服。
- `docs/login-access.md`：真实 E2E 若执行，默认只用本机测试租户 `测试租户/aoteman/111111`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。portal 返回路径与前端子页签路由统一。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: portal 返回文件签名子页签 -> Given DCC adapter 生成电子签名入口 / When 查询 primary route / Then 返回 /signature-governance/file-signatures。`
- `BDD: portal 返回用户授权子页签 -> Given DCC adapter 生成授权入口 / When 查询 secondary route / Then 返回 /signature-governance/authorizations。`
- `BDD: portal 返回批记录签名子页签 -> Given eDHR adapter 生成签名入口 / When 查询 primary route / Then 返回 /signature-governance/batch-signatures。`

## Cleanup Keep

- `doc/tasks/20260624-signature-governance-route-subtabs/task.md`
- `doc/tasks/20260624-signature-governance-route-subtabs/execution-log.md`
- `doc/tasks/20260624-signature-governance-route-subtabs/backend-api-evidence.md`
