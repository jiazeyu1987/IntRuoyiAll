# 任务：展厅封面服务构造注入导致后端启动失败

- Task ID: `20260629-showroom-cover-service-startup-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

修复 `ShowroomProductCoverImageService` 的 Spring Bean 构造注入问题，确保本机后端可正常启动，不引入 fallback，不改变既有展厅封面生成业务契约。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-dcc-recognized-file-metadata-import-export\task.md`
- 状态：未读取；本次仅为运行态阻塞修复，不与其业务范围混改。
- 处理说明：本次修复只触达 `showroom` 启动阻塞文件与对应台账。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：只命中 PowerShell/中文编码门禁。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：PowerShell 5.1 下日志、台账与命令输出统一显式 UTF-8。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。直接修正 Spring Bean 构造注入入口，避免运行时靠默认构造或隐藏配置兜底。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- `BDD: 展厅封面服务可被 Spring 正常注入 -> Given 后端启动时需要创建 ShowroomProductCoverImageService / When Spring 装配 showroom 相关 bean / Then 不再因缺少默认构造器导致应用启动失败。`
- `BDD: 展厅封面服务既有调用契约保持不变 -> Given 批量封面任务与运行时依赖该服务 / When 后端启动并装配依赖链 / Then 既有业务方法仍可通过定向测试与应用启动路径使用。`

## Milestones

1. M1：建立任务文档并记录 BDD/TDD 范围。`completed`
2. M2：补 RED 证据，锁定当前启动失败根因。`completed`
3. M3：实现最小修复并执行 GREEN。`completed`
4. M4：回填证据与恢复后端启动。`completed`

## Expected Verification

- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi-backend.bat`
- 如需补定向测试，再执行 `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductCoverImageServiceTest" test`

## Current Blockers

- 暂无。

## Final Verification Result

- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi-backend.bat` -> PASS
- `Invoke-WebRequest http://localhost:48081/actuator/health` -> PASS（HTTP 200）
- `Invoke-WebRequest http://localhost:48081/admin-api/system/auth/get-permission-info` -> PASS（HTTP 200，业务返回 `401 账号未登录`）
