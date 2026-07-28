# Verification Report

## Scope

- 在本机 `http://127.0.0.1:8081`，使用默认本机身份标签 `芋道源码/admin`，通过真实 `系统管理 > 测试管理` 页面新增并验证批记录模块 Codex Runner 测试项。
- 本任务只维护测试管理测试项与任务证据，不执行远端环境、生产数据、发布、备份或共享运行态操作。

## Added Test Items

| ID | 测试项 | 默认方法 | 并行安全 | 状态 | 检查点 |
| --- | --- | --- | --- | --- | --- |
| 2 | 批记录批次创建与已发布路线快照 | SEQUENTIAL | false | ENABLE | 4 |
| 3 | 批记录打开填写与单元格规则治理 | SEQUENTIAL | false | ENABLE | 4 |
| 4 | 批记录伴随单据填写人与必填跳过口径 | SEQUENTIAL | false | ENABLE | 4 |
| 5 | 批记录提交审核批准闭环 | SEQUENTIAL | false | ENABLE | 4 |
| 6 | 批记录字段审计与操作追溯 | SEQUENTIAL | false | ENABLE | 4 |
| 7 | 批记录归档与电子签名完整性 | SEQUENTIAL | false | ENABLE | 4 |
| 8 | 批记录 Word 导入解析与版式回归 | SEQUENTIAL | false | ENABLE | 4 |

## Evidence

- RED: `BATCH_RECORD_TEST_ITEMS_MODE=assert-existing node doc\tasks\20260726-batch-record-codex-test-items\ensure-batch-record-codex-test-items.e2e.cjs` -> FAIL，证明 7 个批记录测试项在新增前不存在。
- GREEN: `BATCH_RECORD_TEST_ITEMS_MODE=case-only node doc\tasks\20260726-batch-record-codex-test-items\ensure-batch-record-codex-test-items.e2e.cjs` -> PASS，通过真实页面新增/更新 7 个测试项。
- GREEN: `BATCH_RECORD_TEST_ITEMS_MODE=assert-existing node doc\tasks\20260726-batch-record-codex-test-items\ensure-batch-record-codex-test-items.e2e.cjs` -> PASS，精确搜索并回读验证 7 个测试项。
- Artifact: `doc/tasks/20260726-batch-record-codex-test-items/artifacts/batch-record-codex-test-items-summary.json`。

## Notes

- 所有新增项按手动重排样例约定：测试方法项只写操作步骤，测试目标项写可验证检查点。
- 所有新增项均设置为 `SEQUENTIAL` 且 `parallelSafe=false`，避免批记录写入、审批、归档和导入场景并行执行互相污染。
- 未执行这 7 个业务测试项本身；本次完成的是测试管理测试项新增和结构化保存验证。
