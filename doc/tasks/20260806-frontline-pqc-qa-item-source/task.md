# 20260806 Frontline PQC QA Item Source

## Task Goal

优化一线 PQC 黄框字段来源，确保接收标准、检验方法、检验设备和设备编号均来自生产订单对应产品的 QA 检验项目中匹配当前工序的正式配置，不从表单槽位、工序开始、前端默认文案或 raw payload 推断。

## Milestones

- [x] Milestone 1: 核对现有 QA 规程、PQC 填写页和后端任务快照链路
- [x] Milestone 2: 先补 RED 回归，锁定 QA 项目设备/标准/方法来源约束
- [x] Milestone 3: 实现最小正式链路修正，不引入 fallback
- [ ] Milestone 4: 运行定向验证并记录 GREEN / REGRESSION
- [ ] Milestone 5: 收尾记录与清理状态确认

## Expected Verification

- 后端定向测试：`mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" test`
- 前端静态契约：覆盖 QA 规程保存时项目设备配置不能只降级为 `equipmentRequired`
- 结构校验：`git diff --check`

## Applicable Gates

- MES PQC 项目级检验快照门禁：PQC 填写、接收标准、检验方法、检验设备、设备编号必须来自发布 QA 规程项目和结构化 `itemResults[]`，禁止用整单设备、固定字段、前端文案、默认上下限、空标准、raw payload 或 API-only 展示替代正式项目级快照。
- 严格 no-fallback：缺正式 QA 项目、标准、方法或项目设备配置时必须 fail fast，不能用默认值、表单槽位、工序开始或前端拼接文案兜底。

## Current Status

blocked

## Blocker

- 后端目标验证被本任务外既有合并冲突阻断：`MesProcessPoolTeamLeaderController.java` 与 `MesTeamLeaderActiveOrderServiceImpl.java` 当前为 `UU`，源码含 `<<<<<<< HEAD` / `>>>>>>> origin/int_main` 冲突标记，导致 `yudao-module-mes` 重新编译失败。
- 按 no-fallback 与任务所有权规则，本任务不擅自解析这两个生产组长流程文件的冲突；需先完成对应冲突解决后，才能重新运行后端定向单测并收尾。

## Verification Status

- PASS：`node tests\e2e\pqc-item-equipment-standard-method-static.spec.js`。
- PASS：task-owned `git diff --check`，仅输出 CRLF 工作区提示，无空白错误。
- BLOCKED：`mvn -pl yudao-module-mes -am "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，在 MES 主模块编译阶段被上述无关 `UU` 冲突阻断。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；以 QA 规程项目和项目设备表作为唯一来源。
- `是否存在临时补丁或绕过`：否。
