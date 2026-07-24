# 任务: eDHR 演练预检补齐模板与 BPM 门禁

## 任务目标

扩展已提交的 eDHR rehearsal readiness / preflight 后端能力，补齐两类本轮真实演练踩过但尚未正式预检的长期缺口：

- BPM 流程定义扩展信息重复或缺失时，预检必须明确阻塞，避免提交阶段才出现流程定义异常。
- 工艺路线绑定的批记录报表存在未确认填写规则单元格时，预检必须明确阻塞，避免继续依赖人工启发式数据库写入。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 本任务命中门禁：
  - 缺少前置条件必须显式暴露，不得用 fallback、mock 成功或静默跳过掩盖。
  - 本切片只扩展只读预检，不写真实租户数据，不自动修复 BPM、模板或路线配置。
  - 后端行为变更继续采用 BDD + 严格 TDD。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，把真实演练中人工发现的 BPM/模板缺口纳入正式预检。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: BPM 定义扩展信息必须唯一 -> Given eDHR BPM 激活定义存在 / When readiness 查询到 0 条或多条 bpm_process_definition_info / Then 返回 BPM_DEFINITION_INFO_MISMATCH blocker，且不继续假定可发起。`
- `BDD: 模板填写规则必须确认 -> Given 工艺路线绑定的批记录报表存在未确认 fillable 单元格 / When 调用 readiness / Then 返回 TEMPLATE_CELL_RULE_UNREVIEWED blocker，说明 reportId 和未确认数量。`
- `BDD: 预检只读不修复模板或 BPM -> Given readiness 发现 BPM 或模板缺口 / When 返回 BLOCKED / Then 不修改报表 JSON、不改 BPM startUserIds、不删除重复定义。`

## 里程碑

1. 建立任务包、修正上一任务文档边界。`DONE`
2. 写 RED 测试覆盖 BPM 重复扩展信息和模板未确认规则。`DONE`
3. 实现只读检测并保持现有 PASS 路径。`DONE`
4. 运行验证、回填证据并提交。`DONE`

## 预期验证

- `mvn -pl yudao-module-mes "-Dtest=MesProEdhrRehearsalReadinessServiceTest,MesProEdhrBatchExecutionControllerTest" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-edhr-readiness-template-bpm-hardening\backend-api-evidence.md`

## 当前状态

`COMPLETED`

已在 eDHR rehearsal readiness 只读预检中补齐 BPM 定义扩展信息唯一性和路线绑定模板填写规则确认检查。预检会返回稳定 blocker，不自动修复 BPM、模板 JSON 或租户数据。

## Cleanup Keep

- `doc/tasks/20260622-edhr-readiness-template-bpm-hardening/backend-api-evidence.md`
