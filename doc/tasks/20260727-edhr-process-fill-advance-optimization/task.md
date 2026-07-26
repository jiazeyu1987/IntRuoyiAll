# eDHR 工序多填写人推进逻辑优化

## Task Goal

按照用户确认的业务规则优化当前 eDHR 批次执行流程：一个工序允许多个人分别填写不同表单；系统不引入负责人概念；当前工序的推进资格优先由过程检验记录填写人集合决定，若当前工序没有过程检验记录填写人，则由该工序解析出的全部表单填写人并集决定。个人工作台必须让当前用户看到并进入自己可填写的表单任务；提交和推进必须经过后端正式权限校验；最终通过完整真实数据 E2E 验证。

## Milestones

- [x] M1: 审计现有工作任务、批次任务、动态表单和工序推进链路
- [x] M2: 记录 BDD 场景并新增 RED 回归测试覆盖多填写人工作台和推进资格
- [x] M3: 实施后端数据/权限/推进逻辑优化，不引入负责人或 fallback
- [x] M4: 实施前端个人工作台和统一进入填写工作区优化
- [x] M5: 运行后端、前端静态合同和完整真实数据 E2E 验证
- [x] M6: 完成证据记录、经验沉淀和收尾门禁

## Expected Verification

- RED: 后端目标测试证明当前仅 `assigneeUserId` 工作台查询和默认第一候选人推进规则不符合业务规则。
- RED: 前端目标静态合同证明个人工作台进入 FormCenter 动态表单任务不能强制要求 `executionId`。
- GREEN: 后端目标测试覆盖：
  - 当前用户属于表单填写人集合时可在个人工作台看到待办。
  - 过程检验记录存在填写人时，仅过程检验记录填写人具备工序推进资格。
  - 无过程检验记录填写人时，当前工序全部表单填写人并集具备推进资格。
  - 非填写人或无推进资格用户 fail-fast。
- GREEN: 前端目标测试覆盖个人工作台打开普通批记录和 FormCenter 动态表单。
- GREEN: 完整真实数据 E2E 使用本机真实前端路径创建或选择任务自有批次数据，验证多用户/多表单填写、推进下一工序、无 API-only 替代。
- GREEN: 相关 evidence 脚本、UTF-8 校验、`git diff --check` 通过。

## Current Status

completed

## 经验门禁

- `eDHR 详情回填门禁`：填写人必须来自正式任务、表单权限规则或路线绑定快照，不得从当前登录人、创建人或前端文案推断。
- `eDHR 批次任务配置来源门禁`：批次执行任务必须尊重已冻结发布路线快照和当前正式规则，不得通过发布快照通用 fallback 掩盖当前配置损坏。
- `eDHR 终态批次个人待办门禁`：个人工作台源头不得展示已关闭、归档、驳回或作废批次残留待办。
- `eDHR 路线表单跳过口径门禁`：动态表单进入路径不得误用跳过或 legacy 预览；必填表单不允许前端绕过后端规则。
- `eDHR 单据填写人显示值门禁`：真实 E2E 页面填写人断言必须以详情接口 `fillableUsers` 当前返回为准。
- `eDHR 批次执行数据库夹具与证据文件门禁`：真实 E2E 必须走真实前端用户路径，API 仅用于最终只读核验。
- `官方登录前置与 admin-only 全量验证门禁`：若只有 admin 基线账号，写入型 E2E 必须获得明确授权并记录数据范围；不得记录密码。
- `Schema-backed E2E 迁移与字段可选态门禁`：若出现 schema 字段缺失或系统异常，必须先核对真实库/迁移，不得前端隐藏。
- `eDHR 工作任务 FormCenter 动态表单夹具门禁`：动态表单真实 E2E 夹具必须创建真实工单、保持 `batch_record_report_id` 为空、补齐 FormCenter 上下文和 route root/predecessor 快照，并按目标表格行点击“处理”。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；目标是将填写人集合、个人工作台可见性和工序推进资格统一到正式后端模型。
- `是否存在临时补丁或绕过`：否。

## Final Verification

- Backend target tests PASS: 5 tests, 0 failures.
- Frontend static contracts PASS: 3 scripts.
- Real data E2E PASS: `EDHR-ADV-6T182008199Z`, next fill counts `1/0/1`.
- Evidence validators PASS: backend API, frontend feature, bug regression, QA.
- Cleanup PASS: closeout preview/apply无 blocked/warnings，`EDHR-ADV-%` 活跃数据库残留为 0，临时 runtime patch 目录和空补丁文件已清理。
- Git status: `int_main` 与 `origin/int_main` 对齐，无未提交差异。

## Cleanup Keep

- doc/tasks/20260727-edhr-process-fill-advance-optimization/task.md
- doc/tasks/20260727-edhr-process-fill-advance-optimization/execution-log.md
- doc/tasks/20260727-edhr-process-fill-advance-optimization/verification-report.md
- doc/tasks/20260727-edhr-process-fill-advance-optimization/backend-api-evidence.md
- doc/tasks/20260727-edhr-process-fill-advance-optimization/frontend-feature-evidence.md
- doc/tasks/20260727-edhr-process-fill-advance-optimization/bug-regression-evidence.md
- doc/tasks/20260727-edhr-process-fill-advance-optimization/qa-evidence.md
