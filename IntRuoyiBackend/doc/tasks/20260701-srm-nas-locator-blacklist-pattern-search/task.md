# 任务：SRM NAS定位 黑名单与通配搜索（后端 / SQL）

- Task ID: `20260701-srm-nas-locator-blacklist-pattern-search`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

在 SRM 模块新增 `NAS定位` 黑名单全局配置接口、refresh 阶段文件名过滤和 `*` 通配搜索行为，并补齐对应菜单 SQL / 权限合同。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-srm-admin-smart-scheduling-menu-leak-fix\task.md`
- 状态：`completed`
- 处理说明：上一后端任务已完成，不阻塞本次 NAS定位 新需求。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - SQL、Java、测试和文档统一按 UTF-8 处理。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。使用正式全局配置键、正式接口和 refresh 时过滤，不走页面本地过滤绕过。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 黑名单配置接口规范化保存 -> Given 用户提交多行黑名单规则且包含空行、重复项 / When 保存黑名单 / Then 服务应 trim、去空、大小写不敏感去重并按原顺序保存 JSON 数组。`
- `BDD: 非法黑名单配置 fail fast -> Given 配置键 srm.nas-locator.blacklist-patterns 的值不是合法 JSON 数组 / When 读取黑名单或执行刷新 / Then 接口抛显式异常。`
- `BDD: refresh 阶段排除黑名单文件 -> Given 黑名单包含 *.pyc / When 刷新快照 / Then pyc 文件既不计数也不入库。`
- `BDD: 通配搜索按文件名 LIKE 生效 -> Given 搜索关键字包含 * / When 查询分页 / Then 服务把 * 安全转换成 SQL LIKE，仅按文件名匹配且不走普通精确/前缀排序。`

## Milestones

1. M1：建立后端任务文档并确认当前 NAS定位 合同入口。`completed`
2. M2：补 service/controller/SQL RED 测试。`completed`
3. M3：实现 settings service、查询与 refresh 逻辑。`completed`
4. M4：补菜单 SQL 与权限合同。`completed`
5. M5：GREEN 验证并补 evidence。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am "-Dtest=SrmNasLocatorServiceTest,SrmNasLocatorBlacklistSettingsServiceTest,SrmNasLocatorControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_d7_d10_sql_contract.py -k nas_locator -q`

## Current Blockers

- 暂无。

## Current Status

completed

## Cleanup Candidates

- `doc/tasks/20260701-srm-nas-locator-blacklist-pattern-search/backend-api-evidence.md`
- `doc/tasks/20260701-srm-nas-locator-blacklist-pattern-search/database-schema-evidence.md`

## Final Verification Result

- `mvn -pl yudao-module-srm "-Dtest=SrmNasLocatorBlacklistSettingsServiceTest,SrmNasLocatorControllerTest,SrmNasLocatorServiceTest" test` -> `PASS`
- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_d7_d10_sql_contract.py -k nas_locator -q` -> `PASS`
