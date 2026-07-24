# 工序编码 ER 前缀后端修复

## 任务目标

- 修复 eDHR Word 导入自动补齐工序时仍生成 `EDHR_PROC_...` 的后端硬编码根因。
- 新自动补齐工序编码统一改为 `ER...`。
- 将本机已有 `EDHR_PROC_...` 工序数据迁移为 `ER...`，让页面立即显示短编码。

## 里程碑

- [x] M1：定位后端硬编码根因。
- [x] M2：添加回归测试覆盖 ER 前缀规则。
- [x] M3：修改后端工序编码前缀常量。
- [x] M4：迁移本机已有旧前缀数据并回查。
- [x] M5：运行目标测试并提交后端仓改动。

## 预期验证

- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordRouteGenerationCodeRuleTest test` 通过。
- 数据库 `mes_pro_process.code LIKE 'EDHR_PROC_%'` 回查为 0。
- 数据库现有 eDHR 自动补齐工序编码以 `ER` 开头。

## 数据安全与回滚

- 数据库引擎：MySQL 8.0.39，本机容器 `int-ruoyi-mysql`，库 `ruoyi-vue-pro`。
- 数据变更范围：仅本机 `mes_pro_process.code` 中 `EDHR_PROC_` 前缀替换为 `ER`，未删除数据，未改正式环境。
- 回滚方式：如需恢复旧展示，可执行 `UPDATE mes_pro_process SET code = CONCAT('EDHR_PROC_', SUBSTRING(code, 3)) WHERE code LIKE 'ER%' AND name LIKE '%工序生产记录%';`，仅限本次迁移数据范围内使用。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`，所有中文输出与文件读写显式 UTF-8。
- 数据库变更：已读取 `database-schema-delivery`，本次只做本机可逆字符串前缀替换，不删除数据、不修改正式环境。
- 混合脏工作区：后端仓已有大量既有脏改，本任务只提交后端前缀常量、回归测试和任务文档。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。直接移除后端硬编码长前缀。
- 是否存在临时补丁或绕过：否。

## 当前状态

- 状态：COMPLETED
- 已完成：确认根因是 `MesProBatchRecordRouteGenerationServiceImpl` 中 `PROCESS_CODE_PREFIX = "EDHR_PROC_"`；已迁移本机 28 条旧前缀工序数据为 `ER...`。
- 验证：后端目标测试通过；本机数据库 `EDHR_PROC_` 剩余 0 行。
