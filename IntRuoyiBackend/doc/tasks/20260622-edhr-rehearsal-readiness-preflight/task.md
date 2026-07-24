# 任务: eDHR 演练预检

## 任务目标

新增 eDHR rehearsal readiness / preflight 后端能力，让系统在真实演练前集中暴露菜单权限、电子签名授权、BPM 发起资格、工艺路线批记录配置、权限范围、模板规则等前置缺口，减少依赖人工查库和临时补数据。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 本任务命中门禁：
  - 缺少前置条件必须显式暴露，不得用 fallback、mock 成功或静默跳过掩盖。
  - 真实 E2E 和租户写入属于高风险动作；本切片只做只读预检接口与单元/契约测试，不操作真实租户数据。
  - 后端行为变更必须先写 BDD 与 RED 测试，再做最小实现。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，使用正式只读预检能力替代人工串查。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 演练角色前置完整时预检通过 -> Given 执行人、审批人、归档员具备菜单权限、签名授权、BPM 发起资格和路线批记录配置 / When 调用 eDHR 演练预检 / Then 返回 overallStatus=PASS 且无 BLOCKER。`
- `BDD: 缺少关键前置时预检阻塞 -> Given 任一角色缺少菜单、签名授权、BPM startUserIds 或路线权限范围 / When 调用 eDHR 演练预检 / Then 返回 overallStatus=BLOCKED 且逐项说明 blocker code、责任角色和修复建议。`
- `BDD: 只读预检不自动修复数据 -> Given 预检发现缺口 / When 服务返回结果 / Then 不写入角色、菜单、BPM、签名或路线配置数据。`
## 里程碑

1. 建立任务包、接口证据和 RED 测试。`DONE`
2. 实现预检请求/响应模型、服务和控制器入口。`DONE`
3. 覆盖菜单、签名、BPM、路线批记录配置、权限范围检查。`DONE`
4. 运行目标测试并回填主演练收口文档。`DONE`

## 预期验证

- `mvn -pl yudao-module-mes -Dtest=MesProEdhrRehearsalReadinessServiceTest,MesProEdhrBatchExecutionControllerTest test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-edhr-rehearsal-readiness-preflight\backend-api-evidence.md`

## 当前状态

`COMPLETED`

已新增只读预检入口 `GET /mes/pro/edhr-batch-execution/rehearsal-readiness`，可返回 `PASS` 或 `BLOCKED`，并用稳定 item code 暴露本轮演练踩过的关键前置缺口。

## Cleanup Keep

- `doc/tasks/20260622-edhr-rehearsal-readiness-preflight/backend-api-evidence.md`
