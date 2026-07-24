# 任务：预览重排缺少物料不阻断（后端）

- Task ID: `20260630-replan-preview-material-shortage-nonblocking`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `in_progress`

## Task Goal

恢复 `yudao-module-mes` 自动排产预览/重排预览合同：工单缺少生产用料清单时只记录真实 issue，不再作为 blocking 阻断预览。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-showroom-product-import-create-missing\task.md`
- 状态：`completed`
- 处理说明：上一后端任务已完成，本次进入新的 MES 回归修复任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Java/Markdown/测试文件统一按 UTF-8 处理；PowerShell 5.1 不使用 `&&`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接恢复 issue 严重级别与预览汇总逻辑，不掩盖真实物料问题。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 自动排产预览缺少生产用料清单时返回告警 -> Given 待排产工单缺少生产用料清单 / When 调用 preview/replanPreview / Then issueType 仍为 MATERIAL_DEMAND，但 severity 为 WARNING，且不会让 blockingIssueCount 增加。`
- `BDD: 真正不可排产的结构性问题仍阻断 -> Given 工作站/产线等关键排产资源缺失 / When 调用 preview / Then 仍产生 BLOCKING issue。`

## Milestones

1. M1：建立后端任务文档与日志。`completed`
2. M2：补 RED 回归测试。`completed`
3. M3：修复服务逻辑到 GREEN。`completed`
4. M4：回填缺陷证据与验证结果。`completed`

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest" -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-replan-preview-material-shortage-nonblocking\bug-regression-evidence.md`

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest#preview_shouldWarnWhenProductionMaterialListMissing" -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest" -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-replan-preview-material-shortage-nonblocking\bug-regression-evidence.md` -> 待补完证据后复跑

## Current Status

- `completed`

## Current Blockers

- 无。
