# Task: 电子批记录双页签导入分析

## Goal

将本地 `电子批记录` 页面改为同一路由下的双页签页面，拆分为 `模板列表` 与 `文件解析导入` 两个页签；首期仅支持 `.doc/.docx` 文件，通过本地 `parse + commit` 导入链路完成上传、解析、分析展示、勾选候选并提交模板。

## Scope

- 在后端仓库创建任务文档并记录 BDD/TDD 证据。
- 仅修改电子批记录本地模板导入相关后端接口与权限对齐。
- 保持现有本地模板导入 `parse/commit` 响应结构不变。
- 不扩展图片导入、不改 JimuReport 页面、不新增 fallback。

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260516-dcc-preview-watermark-cors-expose-fix/task.md`
- Status before this task: blocked by user reprioritization.
- Impact: the DCC preview watermark fix is paused independently and does not
  block this MES electronic batch record tab implementation.

## BDD Scenarios

- BDD: 导入接口权限对齐 -> Given 用户具有 `mes:pro-batch-record-template:import` 权限, When 调用本地模板导入 `parse` 或 `commit`, Then 请求通过权限校验并保持原有响应结构。
- BDD: 无导入权限时快速失败 -> Given 用户缺少 `mes:pro-batch-record-template:import` 权限, When 调用本地模板导入 `parse` 或 `commit`, Then 接口被拒绝且不执行导入逻辑。

## Milestones

1. [x] M1: 创建任务包并补后端 BDD 场景与证据文件。
2. [x] M2: 先写失败的后端权限回归测试。
3. [x] M3: 最小修改导入控制器权限注解并保持接口契约不变。
4. [x] M4: 运行后端目标测试并记录 GREEN 证据。
5. [x] M5: 仅提交当前任务相关后端改动。

## Expected Verification

- `parse` 与 `commit` 改为校验 `mes:pro-batch-record-template:import`。
- 现有本地模板导入解析/提交测试通过，响应结构不变。
- 新增或更新的控制器权限测试通过。

## Current Status

Completed. 后端导入控制器已改为校验 `mes:pro-batch-record-template:import`，控制器权限回归与本地模板导入服务回归均已通过，且当前任务相关后端改动已独立提交。

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordTemplateControllerTest,MesProBatchRecordTemplateServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
