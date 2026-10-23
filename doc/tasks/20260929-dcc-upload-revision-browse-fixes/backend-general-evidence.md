# URB2-010/011 后端修复证据

## 目标

收口 DCC 图纸检入差异判定与同一 Master 连续修改的检出冲突判定。

## BDD 与 TDD

### URB2-010 图纸配套 PDF 差异

- Given CAD 源件摘要不变、配套 PDF 内容已变化且备注不变
- When 操作者提交图纸检入
- Then 服务端按 PDF 内容差异创建新的 WORKING 版本，不以“正文无变化”拒绝

- Given CAD 源件、配套 PDF 内容和备注均未变化
- When 操作者提交图纸检入
- Then 服务端拒绝 CHECKIN_NO_CHANGE

RED（修复前）：`doCheckinControlledFile` 只比较 `baseSourceSha256` 与新源件摘要以及备注，未读取或比较新旧 `drawingPdfFileId` 的内容摘要；第一条场景会错误抛出 `CONTROLLED_FILE_CHECKIN_NO_CHANGE`。

### URB2-011 同链旧工作稿/返工前驱

- Given 同一 Master 的 ACTIVE A/1 -> WORKING A/2 -> WORKING A/3 前驱链
- When 操作者检出 A/3 继续修改
- Then A/2 作为同链旧 WORKING 不构成其它未完成流程，检出成功

- Given 同一 Master 的返工前驱 PENDING_APPLICANT_REWORK A/1 -> WORKING A/2
- When 操作者检出 A/2 继续修改
- Then 合法返工前驱不阻断检出

- Given 同一 Master 存在不属于当前前驱链的其它未完成审批候选
- When 操作者检出当前版本
- Then 服务端继续拒绝 `CONTROLLED_FILE_WORKFLOW_IN_PROGRESS`

RED（修复前）：`rejectWhenMasterHasOtherUnfinishedWorkflow` 将除当前 ID 外的所有未完成版本视为冲突，没有按 `predecessorControlledFileId` 区分同链旧 WORKING/返工前驱；连续检入和返工继续修改会被错误阻断。

## 里程碑

- [x] 建立 BDD、RED 记录
- [x] 新增聚焦单测并确认 RED
- [x] 实现最小后端修复
- [x] 定向单测 GREEN 与编译回归
- [x] `ready_for_closeout` 后清理验证并标记 `completed`

## 验证记录

- `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest,DccControlledFileFinalizationServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DskipITs" test "-Dcheckstyle.skip=true"`：227 tests PASS。
- `mvn -pl yudao-module-dcc -am "-DskipTests" compile "-Dcheckstyle.skip=true"`：PASS；主 Agent 补齐最终化服务的 PDF 错误码 import 后完成 reactor 编译。
- `git diff --check -- <本任务涉及文件>`：通过。
- 静态复核：URB2-010 的 PDF-only 变化、PDF/CAD/元数据全相同拒绝、URB2-011 的连续 WORKING、返工前驱豁免和分支候选拒绝均有对应测试。

## 约束

- 仅修改 DCC QueryService 及其聚焦单测/证据文档。
- 不运行 E2E、业务 API、数据库写入或服务启停，不执行 Git 提交/推送。
- 保留工作区其它既有改动。

## 状态

completed
