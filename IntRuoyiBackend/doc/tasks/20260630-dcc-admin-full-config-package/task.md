# 任务：文控管理员全量数据包（后端）

- Task ID: `20260630-dcc-admin-full-config-package`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `in_progress`

## Task Goal

为 DCC 文控中心提供正式的“导出全量数据包 / 导入全量数据包”能力，覆盖文控权限四个页签、分发规则、培训规则以及为这些规则提供闭环引用所必需的目录/类别/审批岗位配置，并将全量包 owned scope 正式收口为 tenant-scoped managed scope。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-mes-schedule-order-manual-finish-filter\task.md`
- 状态：`blocked`
- 处理说明：已因用户切换到更高优先级的 DCC 文控中心配置迁移需求而显式阻塞；本次只在 `yudao-module-dcc` 范围内推进，不混入 MES 任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文任务文档、执行日志、JSON/SQL 与命令输出统一按 UTF-8 处理。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；由后端聚合并维护正式全量包合同，并用 tenant-scoped managed scope 显式隔离“配置迁移范围”和“历史业务文件数据”，不在前端串调多个导入导出接口伪装“全量迁移”。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 后端可导出文控中心单个全量包 -> Given 当前租户存在文控目录、文控权限、分发规则、培训规则与审批岗位配置 / When 调用文控中心全量包导出接口 / Then 返回一个包含所有受管配置与引用键的 JSON 数据包。`
- `BDD: 后端可导入文控中心单个全量包 -> Given 用户持有源租户导出的全量包 / When 在目标租户调用导入接口 / Then 系统按依赖顺序覆盖目录、类别、矩阵、规则与审批岗位配置。`
- `BDD: 导入按业务键覆盖并刷新 managed scope -> Given 目标租户已存在同 code 的目录、类别或审批岗位，以及历史包外旧配置 / When 导入新全量包 / Then 同业务键配置被覆盖，仅上一次由文控管理员全量包接管的 scope 会按本次包内容刷新，包外历史配置不会拖垮整次导入。`
- `BDD: managed scope 导出只返回包管理范围 -> Given 目标租户存在历史测试类别或目录，但当前 managed scope 已被文控管理员全量包刷新 / When 再次导出全量包 / Then 导出结果只包含 managed scope 内的目录、类别与审批岗位，业务内容与源包一致。`
- `BDD: 导入失败时显式阻塞缺失引用 -> Given 数据包中的规则引用了目标包内不存在的目录、类别或审批岗位 / When 导入执行 / Then 后端 fail fast 报错并回滚，不写入部分成功数据。`

## Milestones

1. M1：建立后端任务文档并锁定全量包 owned scope。`completed`
2. M2：补 RED 测试锁定导入导出合同。`completed`
3. M3：实现后端聚合 service、controller 与导入摘要返回。`completed`
4. M4：将 cleanup 合同重构为 tenant-scoped managed scope，并补 schema/回归测试。`completed`
5. M5：运行定向测试并回填证据。`completed`
6. M6：重打隔离后端并复跑真实租户 round-trip。`pending`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccAdminFullConfigPackageServiceTest,DccFileCategoryControllerConfigPackageContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Current Blockers

- 真实租户导出/导入验证在 managed scope 重构完成前被阻塞；当前 cleanup 仍会把整个租户现存 DCC 配置当作 package owned scope，遇到被历史 `dcc_controlled_file_master` 引用的包外类别时会整体失败。
