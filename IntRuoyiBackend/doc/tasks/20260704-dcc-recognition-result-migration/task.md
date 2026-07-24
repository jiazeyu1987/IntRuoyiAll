# DCC 识别结果迁移包后端

## 任务目标

在后端实现 DCC 识别结果迁移包导出、导入预览和确认应用能力。测试服务器导出的识别结果应能在正式服务器按稳定业务键匹配文件，确认后只更新可安全应用的成功识别行。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。跨环境导入不依赖测试服 ID，先预览后确认，复用现有元数据更新校验。
- 是否存在临时补丁或绕过：否。

## 经验门禁

- PowerShell/中文文件读写：已读取根目录 `docs/powershell-memory.md`，本任务中文文档与命令均按 UTF-8 处理。
- BDD/TDD：先写 BDD 与 RED 测试，再实现生产代码。
- 无 fallback：文件、产品、项目无法唯一解析时必须显式失败，不自动创建、不猜测、不静默跳过。
- 服务器/E2E：当前仅本地开发与自动化验证；若进入测试服或正式服真实导入导出，需先执行服务器和登录前置门禁。

## 里程碑

1. 后端 RED：新增迁移包导出、预览、确认的失败测试。
2. 后端 GREEN：实现 Excel VO、接口、服务、匹配校验和确认应用。
3. 后端回归：运行 DCC 迁移相关单测和既有导入导出单测。
4. 后端提交：只提交本任务产生的后端与后端任务文档改动。

## 预期验证

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=*Recognition*Migration*,DccControlledFileMetadataImportExportServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 当前状态

- 状态：已完成。
- 已完成：迁移包 Excel VO、导出接口、导入预览、确认应用、稳定键匹配、产品/项目解析、后端单测。
- 最终结果：后端支持 `/recognition-records/migration-export-excel`、`/recognition-records/migration-import-preview`、`/recognition-records/migration-import-confirm`。
- 追加修复：迁移包产品编码列已改为优先导出 MDM 产品主数据 `dccProductCode`，避免把识别记录中的项目短码写入迁移包导致正式服导入校验失败。
- 本地运行态验证：`recognition-migration-after-fix-20260704-231853.xlsx` 可成功回传导入预览，结果 total=1、applicable=1、blocked=0。
