# eDHR 批次执行后端实现任务

- Task ID: `20260608-edhr-batch-execution-full-flow`
- Status: `completed`
- Branch: `codex/edhr_batch`
- Source Spec: `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260608-edhr-batch-execution-full-flow\`

## 任务目标

在后端实现 eDHR 批次级执行编排，新增批次主表、工序任务、批次签名、批次归档能力，复用现有 `mes_pro_batch_record_execution` 单张表单执行能力，并为前端批次执行工作台提供清晰 API。

## 里程碑

1. RED：新增后端 service/controller 测试，验证批次创建、任务阻塞、关闭校验、归档前置。
2. GREEN：新增 DO/Mapper/Service/Controller/VO/ErrorCode/SQL，使后端测试通过。
3. REGRESSION：运行现有 eDHR 单张执行相关测试，确认未破坏旧链路。
4. E2E 支撑：提供真实前端路径需要的 API 和测试数据前置失败语义。

## 预期验证

- `mvn -pl yudao-module-mes '-Dtest=MesProEdhrBatchExecutionServiceTest,MesProEdhrBatchExecutionControllerTest,MesProEdhrBatchExecutionArchiveControllerTest' test`
- `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionArchiveContractTest,MesProBatchRecordExecutionSignatureServiceTest' test`
- `python -X utf8 -m pytest script/tests/test_edhr_batch_execution_schema_sql.py -q`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺工单、路线、默认批记录、签名、审批、审计、归档前置必须 fail fast。
- `是否从根因和长期维护角度解决`：是；新增批次级编排层，不把完整流程塞进生产报工。
- `是否存在临时补丁或绕过`：否；不 mock 成功，不改写旧执行历史。

## 当前状态

- 状态：已完成。
- 已完成：批次级 DO/Mapper/Service/Controller/VO/ErrorCode/SQL；批次执行页面菜单和权限 SQL；批次归档独立控制器；后端 RED/GREEN/REGRESSION 验证；真实测试租户 Playwright E2E 全流程验证。
- 提交门禁验证：`python -X utf8 -m pytest script/tests/test_edhr_batch_execution_schema_sql.py -q` 通过，覆盖迁移表、唯一约束、权限、租户包 JSON fail-fast 和禁止静默覆盖语义。
- 最终 E2E：测试租户批次 `EDHR-BATCH-122-FULL-0609020810`，15 张必填单表全部填写/复核/提交/审批，批次关闭并生成 `SEALED` 最终 PDF `EDHR-BATCH-122-FULL-0609020810-edhr-final.pdf`。
- 融入后 `int_main` E2E：测试租户批次 `EDHR-BATCH-122-MAIN-0609013248`，合并后的后端主目录服务 `48081` 与前端主目录服务 `8081` 完成 15 张必填单表复核签名、主数据追溯、提交、审批、关闭、归档、最终 PDF 下载、打印窗口打开和复盘查看；最终 `status=40`、`task_approved_count=15`、`blocked_count=0`、归档 `SEALED`。
