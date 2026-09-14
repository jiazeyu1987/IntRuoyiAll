# DF03 路线-DCC项目代码正式绑定

## 任务目标

实现工艺路线到 DCC 项目代码的正式绑定能力，供后续一线 PQC 从“订单确定路线 -> 路线正式DCC项目代码 -> QA规程”读取唯一链路。DF03 只负责路线-DCC绑定 API、版本并发控制、权限合同和路线编辑页配置入口，不引入 QA/产品推算，也不修改 DCC 后端或一线 PQC 聚合。

## 里程碑

- [x] M1：读取 DF03 指令、项目规则、主管 dev-plan/test-plan、后端/前端/数据库/E2E规则与适用技能。
- [x] M2：补齐 BDD，并先写/运行真实 RED。
- [x] M3：实现最小正式后端 API、Service、Mapper、VO 与错误码。
- [x] M4：实现路线编辑页的绑定读取、保存、解绑和静态合同。
- [x] M5：运行 GREEN、静态回归和技能证据校验。
- [x] M6：记录验证结果、风险和交接信息。

## 预期验证

- mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesRouteDccProjectBindingServiceTest,MesRouteDccProjectBindingControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- node tests/e2e/mes-route-dcc-project-binding-static.spec.cjs
- 若本地登录入口和账号可用，再执行真实 Playwright 路线编辑路径；若缺入口或账号，只记录阻塞，不用 API-only 冒充。
- 技能证据校验：backend-api-delivery 与 frontend-feature-delivery evidence validator。

## 适用经验门禁

- 一线 PQC DCC-QA 正式关系目标态：QA 只对应 DCC 项目代码，路线-DCC关系是 PQC 上游正式来源。
- 无 fallback：缺 schema、权限、同租户路线或 DCC 项目代码时 fail fast，不返回默认成功。
- 版本并发：绑定、改绑、解绑必须使用 expectedVersion；解绑生成 tombstone 且版本单调递增，避免 ABA。
- 权限边界：GET/PUT 使用路线查询/更新和必要 DCC 查询；DELETE 只需要 route:update，不要求 DCC query。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；建立正式路线-DCC绑定，不再靠产品、路线名称或 QA 推算。
- 是否存在临时补丁或绕过：否。

## Current Status

ready_for_supervisor_review：DF03 已由主管接管完成 RED -> GREEN、前端静态合同、diff check 和技能 evidence 校验；等待独立测试与主管合入门禁。

## 当前已知事实

- worktree 中缺少旧 20260811-frontline-pqc-dcc-qa-agent-design 目录，DF03 以主管 dev-plan.md / test-plan.md 和当前代码为权威。
- 当前代码未发现 MesRouteDccProjectBinding* 生产类或测试类。
- C00 已合入 schema 基线，DF03 不新增迁移，只消费已存在的路线-DCC绑定表。

## 实现结果

- 后端新增路线-DCC绑定 Controller / Service / Mapper / VO，GET 只读返回当前绑定与最新版本，PUT 使用 expectedVersion 保存/改绑，DELETE 使用 expectedVersion 解绑并写入 tombstone 版本。
- 后端保存前校验路线存在、DCC项目代码存在且启用；缺失或停用直接失败，不从产品、QA、formBindings、路线名称或工序推算。
- 绑定表 DO 对齐 TenantBaseDO，使用租户隔离字段匹配 C00 schema。
- 前端路线编辑新增独立 DCC 项目代码页签，读取、保存、解绑均走专用 route-DCC API，不把路线保存成功冒充为 DCC 绑定成功。

## 验证摘要

- RED: mvn DF03 目标测试 -> FAIL，PRO_ROUTE_DCC_PROJECT_INVALID 缺失导致编译失败，证明禁用 DCC 项目代码拒绝合同未实现。
- GREEN: mvn DF03 目标测试 -> PASS，10 tests / 0 failures / 0 errors。
- GREEN: node tests/e2e/mes-route-dcc-project-binding-static.spec.cjs -> PASS。
- GREEN: git diff --check -> PASS，仅报告 LF/CRLF 工作区提示。
- GREEN: backend-api-delivery 与 frontend-feature-delivery evidence validator -> PASS。
