# 排产测试租户数据底座补齐

## 任务目标

在本机运行库中，按用户授权从 `芋道源码/admin` 租户只读提取排产相关真实数据，受控平移到 `测试租户/aoteman`，补齐后续排产闭环开发和真实 E2E 所需的数据底座。写入目标限定 `tenant_id=122`，`tenant_id=1` 只作为来源查询。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260610-scheduling-edhr-development-plan/task.md`。
- 检查结果：该任务已标记 `completed`，并已明确测试租户主要缺口为生产工单 BOM、报工导入/正式报工样本、部分产品路线关系和人工绑定。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺表、缺字段、缺自然键映射、目标数据冲突或写入校验失败均必须显式失败。
- `是否从根因和长期维护角度解决`：是。本任务只补真实 E2E 前置数据，不改变排产业务设计；后续仍按排产工单、报工归属、eDHR 保护的正式方案开发。
- `是否存在临时补丁或绕过`：否。脚本补数只作为测试租户真实用户路径的前置数据准备，不替代前端 E2E。

## BDD 场景

- BDD: 测试租户具备排产订单物料前置数据 -> Given admin 租户存在生产工单和工单 BOM / When 脚本按自然键平移代表性样本到测试租户 / Then 测试租户存在可用于排产校验的生产工单 BOM，且 admin 数据不被修改。
- BDD: 测试租户具备报工归属前置样本 -> Given admin 租户存在外部 MES 报工导入记录和正式报工样本 / When 脚本平移必要样本到测试租户 / Then 测试租户出现报工导入样本和正式报工样本，后续可改造为待归属流程验证数据。
- BDD: 自然键映射失败时快速失败 -> Given admin 源数据引用的产品、工位、工序、任务或工单在测试租户找不到匹配自然键 / When 执行补数脚本 / Then 脚本停止并输出缺失映射，不写入半成品数据。

## 里程碑

- [x] M1：创建补数任务文档和数据库证据模板。
- [x] M2：只读盘点 admin 与测试租户缺口、表结构和自然键映射。
- [x] M3：执行受控补数脚本，仅写入测试租户。
- [x] M4：执行前后 SQL 校验，记录回滚方式和最终状态。

## 预期验证

- RED：测试租户补数前 `mes_pro_work_order_bom`、`mes_pro_feedback`、`mes_pro_feedback_import_record` 缺少排产验证样本。
- GREEN：补数后测试租户存在可映射的生产工单 BOM、报工导入样本、正式报工样本；admin 租户计数不变。
- GREEN：所有新增测试租户数据带 `creator='codex-scheduling-baseline'` 或可追溯 remark，用于回滚识别。

## 当前状态

completed

## 完成记录

- 已新增补数脚本：`doc/tasks/20260610-scheduling-test-tenant-data-baseline/scripts/scheduling_test_tenant_baseline.py`。
- 已从 admin 租户只读提取真实样本，并写入测试租户：新增或补齐产品路线关系、`WS-B040` 人工绑定、两张样本生产工单的 BOM、三条报工与导入记录样本。
- 已确认 admin 租户关键计数保持不变，测试租户数据底座校验通过。

## 最终验证

- GREEN: `python -X utf8 doc\tasks\20260610-scheduling-test-tenant-data-baseline\scripts\scheduling_test_tenant_baseline.py --mode check` -> PASS。
- GREEN: 只读 SQL 明细校验 -> PASS，测试租户样本工单和报工样本可按真实路线、工序、工位查询。

## Cleanup Keep

- `doc/tasks/20260610-scheduling-test-tenant-data-baseline/task.md`
- `doc/tasks/20260610-scheduling-test-tenant-data-baseline/execution-log.md`
- `doc/tasks/20260610-scheduling-test-tenant-data-baseline/database-schema-evidence.md`
- `doc/tasks/20260610-scheduling-test-tenant-data-baseline/scripts/scheduling_test_tenant_baseline.py`
