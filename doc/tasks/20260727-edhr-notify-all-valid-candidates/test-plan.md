# Test Plan

## Test Cases

### TC-01 失败清单完整性

- `test_case_id`: TC-01
- `mapped_task_ids`: [T1]
- `mapped_acceptance_ids`: [AC-13, AC-14, AC-16]
- `environment or setup`: 2026-07-27 完整 Surefire 报告和当前 Git 工作区。
- `steps`: 提取所有包含 failure/error 的 `TEST-*.xml`，核对套件数量、测试数和失败簇映射。
- `expected_result`: 41 个失败套件全部映射到唯一任务；无遗漏、无通过跳过隐藏。
- `evidence`: `execution-log.md` 失败清单与报告文件列表。

### TC-02 真实 fixture 权威性与可移植性

- `test_case_id`: TC-02
- `mapped_task_ids`: [T0, T7]
- `mapped_acceptance_ids`: [AC-01, AC-02, AC-03]
- `environment or setup`: 经确认的 Word/Excel 原始文件，记录大小、SHA-256、来源和版本。
- `steps`: 将测试资源定位到项目内稳定路径，运行所有 Word/Excel 解析、导入、结构和探针测试。
- `expected_result`: 相关测试全部通过；代码和测试不包含个人用户名或固定盘符路径。
- `evidence`: fixture 清单、哈希、定向 Maven 结果。

### TC-03 静态契约与当前工程结构

- `test_case_id`: TC-03
- `mapped_task_ids`: [T2]
- `mapped_acceptance_ids`: [AC-04, AC-05, AC-14, AC-17]
- `environment or setup`: 当前 `IntRuoyiFronted` 和 MES 测试源码。
- `steps`: 运行前端路径、菜单、DesignerWrapper、schema/migration 静态契约测试。
- `expected_result`: 不再引用废弃根目录；契约与当前正式结构一致，断言未弱化。
- `evidence`: 目标 JUnit/Node 测试输出。

### TC-04 Schema 与 Spring/DB 测试装配

- `test_case_id`: TC-04
- `mapped_task_ids`: [T3]
- `mapped_acceptance_ids`: [AC-05, AC-06, AC-07, AC-17]
- `environment or setup`: Java 17、MES H2 测试 schema、当前 migration/DO/Mapper。
- `steps`: 运行 schema 契约、BeanCreation 失败类、ApplicationContext 和唯一键用例；唯一键用例连续运行两次。
- `expected_result`: 全部通过，Context 正常启动，重复运行无唯一键污染。
- `evidence`: RED/GREEN 命令、H2 schema 核对与 Surefire 报告。

### TC-05 路线与 eDHR 契约

- `test_case_id`: TC-05
- `mapped_task_ids`: [T4]
- `mapped_acceptance_ids`: [AC-08, AC-09, AC-12, AC-17]
- `environment or setup`: 当前路线/eDHR 服务和测试。
- `steps`: 运行路线版本复制、显示字段、批记录绑定、批次执行、任务门禁、演练、legacy-process 和通知服务测试。
- `expected_result`: 严格 Mockito 下无缺失依赖/空引用，三类配置来源独立，通知 66/66 通过。
- `evidence`: 组合 Maven 测试输出。

### TC-06 批记录解析与路线生成

- `test_case_id`: TC-06
- `mapped_task_ids`: [T5, T7]
- `mapped_acceptance_ids`: [AC-02, AC-10, AC-13, AC-17]
- `environment or setup`: 合成 fixture、经确认真实 Word fixture、当前 parser/layout 实现。
- `steps`: 运行 JSON builder、layout calibrator、shape rules、candidate governance、generation code rule 和真实 Word 识别套件。
- `expected_result`: 类型、坐标、形状、路线治理和旧链字段断言全部通过，无模板名特例。
- `evidence`: 定向组合测试输出。

### TC-07 排产与排产订单

- `test_case_id`: TC-07
- `mapped_task_ids`: [T6]
- `mapped_acceptance_ids`: [AC-08, AC-11, AC-13, AC-17]
- `environment or setup`: 当前排产/排产订单服务与严格 Mockito。
- `steps`: 运行 Gantt 工单编码、auto-schedule、schedule-order admission、risk、no-default-config 和 service 测试。
- `expected_result`: 全部通过，无 `PotentialStubbingProblem`、无 lenient、冻结路线/容量/进度行为一致。
- `evidence`: 组合 Maven 测试输出。

### TC-08 原失败套件组合回归

- `test_case_id`: TC-08
- `mapped_task_ids`: [T8]
- `mapped_acceptance_ids`: [AC-12, AC-13, AC-14, AC-16, AC-17]
- `environment or setup`: T2-T7 全部完成，Git 任务范围已核对。
- `steps`: 运行 41 个原失败套件的组合测试，再运行通知目标 3 用例和服务类 66 用例。
- `expected_result`: 组合测试 0 failures/0 errors，通知行为保持。
- `evidence`: Surefire 汇总、Git diff 审查。

### TC-09 完整 MES 最终验收

- `test_case_id`: TC-09
- `mapped_task_ids`: [T9]
- `mapped_acceptance_ids`: [AC-13, AC-14, AC-15, AC-16, AC-17]
- `environment or setup`: `E:\IntRuoyi\IntRuoyiBackend`，无任务自有未完成修复；允许至少 45 分钟有限超时。
- `steps`: 执行 `mvn -pl yudao-module-mes test`，解析最终 Surefire 汇总和 skipped 清单。
- `expected_result`: 退出码 0、`BUILD SUCCESS`、0 failures、0 errors；无新增/扩大跳过。
- `evidence`: 完整 Maven 输出与 `test-report.md`。

## Test Types

- 单元测试：适用，覆盖 mock 装配、业务计算和解析规则。
- 契约测试：适用，覆盖 schema、工程路径、迁移和 API/服务结构。
- 数据库集成测试：适用，覆盖 Spring Context、H2 schema 和数据隔离。
- 真实 fixture 解析/导入：适用且为硬门禁。
- 前端 E2E：本次完整 MES Maven 回归不要求新增页面 E2E；若修复改变用户可见前端行为，再按 `docs/e2e-rules.md` 增补真实路径验证。
- 性能测试：不适用；仅记录完整套件耗时，不以缩短耗时为业务验收。

## Release Recommendation

在 TC-09 通过前保持 `blocked`，不得提交收尾、推送任务完成状态或宣称 MES 完整回归恢复。
