# 任务: eDHR BPM 通知改为站内信

## 任务目标

将 `mes-edhr-approval-v1` 的 BPM 通知从短信发送切换为正式站内信模板，避免审批主链路被用户移动联系方式缺失阻断。

## 用户要求与执行边界

- 用户要求：
  - 继续完善 eDHR，直到可以完成一次完整演练。
  - 发现缺口后按长期机制补齐，而不是继续依赖临时补数据。
- 本任务边界：
  - 只修改 eDHR BPM 通知发送策略与对应站内信模板种子、单元测试、SQL 契约测试。
  - 不引入 fallback，不保留“手机号缺失则换别的路径”的隐式兜底。
  - 不改动 DCC 既有通知行为。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 本任务强制门禁摘录：
  - 缺少前置条件时必须显式暴露，不得用静默失败或 mock 成功掩盖。
  - 长链路问题优先回到正式机制修复，不接受只靠人工补数据维持可用。
  - 新增 SQL/通知契约必须补对应测试。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: eDHR BPM 任务分配通知不应再依赖短信手机号 -> Given 流程定义为 mes-edhr-approval-v1 / When BPM 分配审批任务给审核人 / Then 系统必须发送正式站内信而不是短信。`
- `BDD: eDHR BPM 审批通过通知不应再依赖短信手机号 -> Given 流程定义为 mes-edhr-approval-v1 / When 审批通过后通知发起人 / Then 系统必须发送正式站内信而不是短信。`
- `BDD: 非 eDHR BPM 流程不受影响 -> Given 流程定义不是 mes-edhr-approval-v1 且不是 dcc-controlled-file-approval / When BPM 发送任务或审批通知 / Then 系统仍保持原有短信路径。`

## 里程碑

1. 为 eDHR BPM 通知建立独立任务文档与验收边界。`DONE`
2. 先补 RED/GREEN 单元测试与 SQL 契约测试。`DONE`
3. 修改 BPM 通知实现，使 eDHR 走站内信模板。`DONE`
4. 回填交付证据并交回主演练任务继续验证。`DONE`

## 预期验证

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_edhr_bpm_notify_to_inbox_sql.py -q`
- `mvn -pl yudao-module-bpm -Dtest=BpmMessageServiceImplTest test`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql`

## 当前状态

`COMPLETED`

## Current Status

completed

eDHR BPM 通知已切换为站内信模板：
- `MES_EDHR_BPM_TASK_ASSIGNED`
- `MES_EDHR_BPM_APPROVED`
- `MES_EDHR_BPM_REJECTED`
- `MES_EDHR_BPM_TASK_TIMEOUT`

对应实现位于 `yudao-module-bpm/src/main/java/.../BpmMessageServiceImpl.java`，验证已通过 SQL 契约测试和 `BpmMessageServiceImplTest`，并已在主线修正 `release-migration` 依赖声明。
当前 `release-migration` 头已改为门禁可识别的依赖 ID，且 SQL 契约测试会继续校验它不能带 `.sql` 后缀，避免并入主线后在发布级迁移策略门禁误报缺失依赖。
