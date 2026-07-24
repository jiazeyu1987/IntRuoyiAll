# DCC 基础信息识别无匹配结果语义调整

## 任务目标

将文控中心“识别基础信息”中“文件可正常识别但未匹配到产品/项目基础信息”的场景，从系统失败调整为业务结果为空：接口正常返回，识别记录写入非失败状态，前端提示“识别完成，未匹配到产品名称，请人工确认”。

## 里程碑

1. [x] 建立任务文档、经验门禁和 BDD/TDD 基线。
2. [x] 后端 RED：补充无匹配结果不抛异常、写入未匹配状态的失败测试。
3. [x] 后端 GREEN：实现无匹配状态、响应字段和批量统计语义。
4. [x] 前端同步：根据响应状态区分成功匹配与未匹配提示。
5. [x] 运行目标测试并记录验证证据。

## 预期验证

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest,DccControlledFileProjectCodeRecognitionControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 后端无匹配结果不得抛 `CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_EMPTY`。
- 后端识别记录状态不得写成 `FAILED`，且不得改写受控文件产品名称/编码。
- 真实系统异常仍写入 `FAILED` 并抛错，不引入 fallback、吞异常或默认成功。

## BDD 场景

BDD: 识别完成但未匹配产品名称 -> Given 文控文件可读取且识别服务正常返回空结果, When 用户点击识别基础信息, Then 接口正常返回 `NO_MATCH`，识别记录写入未匹配状态，受控文件产品名称和编码不被改写。

BDD: 系统异常仍识别失败 -> Given 文件读取、配置、候选集或识别服务发生异常, When 用户点击识别基础信息, Then 后端继续 fail fast，记录 `FAILED`，前端展示失败提示。

BDD: 批量识别统计区分未匹配 -> Given 批量识别中某个文件正常完成但没有匹配基础信息, When 任务汇总进度, Then 该文件不计入系统失败，保留可人工确认的未匹配结果。

## 经验门禁

- PowerShell：已读取 `docs/powershell-memory.md`，命令显式设置 UTF-8，不使用 `&&`，不通过 PowerShell 管道传递中文到子进程。
- 任务经验索引：已读取 `docs/experience-index.md`，本任务命中 PowerShell/Windows shell 门禁；不涉及真实 E2E、服务器写入、数据库写入或发布链路。
- BDD/TDD：先记录业务可观察场景，再修改后端测试形成 RED，最后最小实现并回归。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。无匹配是显式业务结果，不捕获或吞掉异常。
- 是否从根因和长期维护角度解决：是。通过响应契约和识别记录状态表达业务空结果。
- 是否存在临时补丁或绕过：否。

## 当前状态

已完成。

## 完成结果

- 后端新增 `recognitionStatus=NO_MATCH` 响应契约和识别记录状态。
- 空识别结果只写入 `NO_MATCH` 识别记录，不再抛空结果异常，也不改写受控文件产品名称/编码。
- 批量识别把既有或新产生的 `NO_MATCH` 视为已完成非失败结果。
- 系统异常、配置缺失、候选缺失、文件缺失、非法候选等 fail-fast 语义保持不变。

## 最终验证

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest,DccControlledFileProjectCodeRecognitionControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> GREEN PASS，47 tests, 0 failures。
- 前端静态契约与 ESLint 由前端任务证据记录。

## Cleanup Keep

- doc/tasks/20260706-dcc-recognition-no-match-success/backend-api-evidence.md
