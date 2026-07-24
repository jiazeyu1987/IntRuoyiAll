# 任务：角色管理导出导入联通（后端）

- Task ID: `20260629-role-export-import-sync`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

补齐权限角色、组织角色、审批角色的后端导入导出合同，确保导出产物包含可回导所需全部字段，且导入对非法结构 fail fast。

## Previous Task Check

- 上一后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-mes-work-order-material-demand-warning-clear\task.md`
- 状态：`completed`
- 处理说明：无未完成阻塞项。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：必须先命中并摘录门禁。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：中文日志、文档和命令输出保持 UTF-8。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。统一修正导出合同与导入入口，避免继续依赖只适合展示的 Excel 列。
- 是否存在临时补丁或绕过：否。

## BDD

- BDD: 权限角色导出包保留回导字段 -> Given 系统存在权限角色及其菜单/数据权限 / When 调用权限角色导出接口 / Then 返回产物包含角色基础字段与回导所需权限结构。
- BDD: 组织角色导出包保留回导字段 -> Given 系统存在组织角色 / When 调用组织角色导出接口 / Then 返回产物包含岗位基础字段并可再次导入。
- BDD: 审批角色导出包保留分配关系 -> Given 系统存在审批角色及分配人员 / When 调用审批角色导出接口 / Then 返回产物同时包含角色基本信息与分配列表。
- BDD: 角色导入包非法时失败 -> Given 导入包缺少必填字段或结构非法 / When 调用任一角色导入接口 / Then 明确报错且不写入任何部分数据。

## Milestones

- M1: 阅读现有 controller/service/vo 与 DCC 审批角色实现。状态：completed。
- M2: 增加 RED 测试锁定导入导出合同。状态：completed。
- M3: 完成后端接口与服务实现。状态：completed。
- M4: 跑定向测试并回填证据。状态：completed。

## Expected Verification

- `mvn -pl yudao-module-system,yudao-module-dcc -Dtest=... test`

## Final Verification Result

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system,yudao-module-dcc "-Dtest=RoleServiceImplTest,PostServiceImplTest,DccApprovalPositionControllerTest,DccApprovalPositionAdminServiceImplTest" test` -> PASS
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system "-Dtest=RoleConfigPackageServiceImplTest" test` -> PASS
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system,yudao-module-dcc "-Dtest=RoleConfigPackageServiceImplTest,PostConfigPackageServiceImplTest,DccApprovalPositionConfigPackageServiceImplTest" test` -> PASS

## Current Blockers

- 无。
