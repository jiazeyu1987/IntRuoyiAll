# 任务：统一电子签名页签后端聚合协议

## 任务目标

在 `sign2` 后端 worktree 中为统一电子签名页签提供聚合协议，首批收口 DCC 与 eDHR 的签名授权状态、签名记录摘要、待处理项摘要和正式签名入口；预留 Showroom、IntAuth 与后续模块按同一协议接入。

## 范围

- 新增或扩展 `signature-governance` 后端聚合接口。
- 首批接入 DCC 与 eDHR。
- 仅提供统一页签展示与跳转所需摘要，不替代 DCC、eDHR 模块内正式签名执行页。
- 不合并 DCC、eDHR 业务签名证据表。

## 经验门禁

- `docs/worktree-memory.md`：前后端成对 worktree，分支同名 `codex/sign2`；验证前必须记录 FE/BE 运行态归属，不能默认复用主工作区端口。
- `docs/login-access.md`：真实 E2E 默认本机测试租户 `测试租户/aoteman/111111`；电子签名密码缺失时必须阻塞，不得 mock 或切换账号。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：统一页签应保持 IntPP 生产订单列表式操作台风格，密集、清晰、表格优先。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺登录用户、缺权限、缺授权或模块未接入时显式返回模块状态与阻塞原因。
- `是否从根因和长期维护角度解决`：是。新增统一聚合协议和模块接入模型，后续模块走同一协议。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 统一电子签名页签展示 DCC/eDHR 摘要 -> Given 当前账号具备统一电子签名页签权限 When 打开统一电子签名页签 Then 后端返回 DCC 与 eDHR 的授权状态、签名记录数、待处理数和正式入口路径。`
- `BDD: 未授权签名必须显式暴露 -> Given 当前账号未开通电子签名授权 When 请求统一电子签名页签摘要 Then DCC/eDHR 模块状态显示未授权阻塞，不能把状态降级为正常。`
- `BDD: 正式签名仍回模块执行 -> Given 当前账号在统一页签看到 DCC/eDHR 待处理项 When 点击模块入口 Then 前端跳转到 DCC 或 eDHR 正式签名页，而不是 BPM 通用审批按钮。`
- `BDD: 新模块统一接入 -> Given 后续模块需要电子签名 When 接入统一页签 Then 只需实现统一聚合/展示协议，不再复制独立签名中心。`

## 里程碑

- [x] M1：创建 `sign2` 后端 worktree 并建立任务文档。
- [x] M2：补齐后端 RED 测试，锁定统一电子签名聚合协议。
- [x] M3：实现 DCC/eDHR 后端聚合摘要与模块接入状态。
- [x] M4：运行 targeted 后端验证并记录证据。
- [x] M5：配合前端统一页签完成联调契约。

## 预期验证

- `mvn -pl yudao-module-dcc "-Dtest=SignatureGovernancePortalServiceTest,SignatureGovernanceControllerTest" test`
- `mvn -pl yudao-module-mes -am "-DskipTests" compile`
- `mvn -pl yudao-server -Dtest=SignatureGovernancePolicySourceConfigTest test`
- `SIGNATURE_GOVERNANCE_E2E_* node tests/e2e/signature-governance-policy.e2e.js`

## 当前状态

已完成。后端统一门户服务、DCC/eDHR adapter、`GET /signature-governance/portal/overview` 与前端统一页签静态联调契约均已验证；本次继续补齐 `signature.governance.policy.modules` 权威策略源配置并新增配置回归测试后，sign2 独立前后端真实数据 E2E 已返回 `READY/PASS`，并在干净融合到 `int_main` 后再次复验通过。旧的 `POLICY_SOURCE_MISSING` 阻塞已被正式配置修复，没有引入 fallback、降级或 mock 数据。

## Cleanup Keep

- `doc/tasks/20260623-unified-electronic-signature-tab/backend-api-evidence.md`
- `doc/tasks/20260623-unified-electronic-signature-tab/bug-regression-evidence.md`
