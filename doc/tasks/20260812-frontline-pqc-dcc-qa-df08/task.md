# 20260812 Frontline PQC DCC QA DF08

## Task Goal

实现 DF08：在锁定 QA 发布版本下，按 QA 自有工序聚合检验项目与设备选项，供一线 PQC 工序列表读取。数据链条只允许通过 DCC 项目代码定位 QA 规程，不做产品、物料、路线推算，不做 QA 工序与 MES 路线工序存在性校验。

## Milestones

- [x] 读取工作区规则、后端规则、收尾规则、PowerShell/UTF-8 规则和 backend-api-delivery 技能契约。
- [x] 创建 DF08 任务记录和 BDD 场景。
- [x] 添加最小失败测试，覆盖 QA 工序检验项目聚合规则。
- [x] 实现最小正式 GREEN。
- [x] 运行定向 Maven、静态检查、禁止项扫描和 backend-api evidence validator。
- [x] 标记 ready_for_closeout。

## Expected Verification

- Maven 定向测试：`cd D:\IntRuoyiWorktree\20260812-frontline-pqc-dcc-qa-df08\IntRuoyiBackend` 后运行 `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
- 静态检查：`git diff --check`。
- 禁止项扫描：确认未新增 item-type 表、未做产品/物料/路线推算、未做 QA 工序与 MES 路线工序存在性校验、未引入 fallback/兼容/默认成功。
- 技能证据校验：`python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260812-frontline-pqc-dcc-qa-df08/backend-api-evidence.md`。

## Current Status

completed

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，按 QA 发布版本与 QA 自有工序、检验项目正式关系聚合，不从订单产品、物料或 MES 路线反推 QA 规程。
- 是否存在临时补丁或绕过：否。

## Applicable Experience Gates

- 经验索引已读取。命中本任务的后端门禁：PQC 待检准入与工序选择必须分离、MES PQC 项目级检验快照门禁、QA 多工序正式发布与退役夹具唯一键必须隔离。
- 本任务只在 DF08 范围内实现 QA 工序检验项目聚合，不改 DCC 项目代码关系、活跃订单准入、前端投影或数据库 schema。
