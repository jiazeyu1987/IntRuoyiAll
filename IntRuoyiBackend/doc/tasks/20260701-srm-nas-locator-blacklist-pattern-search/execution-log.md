BDD: 黑名单配置接口规范化保存 -> Given 用户提交多行黑名单规则且包含空行、重复项 / When 保存黑名单 / Then 服务应 trim、去空、大小写不敏感去重并按原顺序保存 JSON 数组。
BDD: 非法黑名单配置 fail fast -> Given 配置键 srm.nas-locator.blacklist-patterns 的值不是合法 JSON 数组 / When 读取黑名单或执行刷新 / Then 接口抛显式异常。
BDD: refresh 阶段排除黑名单文件 -> Given 黑名单包含 *.pyc / When 刷新快照 / Then pyc 文件既不计数也不入库。
BDD: 通配搜索按文件名 LIKE 生效 -> Given 搜索关键字包含 * / When 查询分页 / Then 服务把 * 安全转换成 SQL LIKE，仅按文件名匹配且不走普通精确/前缀排序。
INFO: previous-task-completed -> PASS，后端上一任务已完成，可直接开始本次 NAS定位 需求。
RED: inherited-blacklist-backend-contract-review -> FAIL, 新增测试在实现前引用了缺失的 `SrmNasLocatorBlacklist*` VO / service / controller 接口，且 refresh 过滤与 `*` 通配搜索行为尚不存在。
GREEN: mvn -pl yudao-module-srm "-Dtest=SrmNasLocatorBlacklistSettingsServiceTest,SrmNasLocatorControllerTest,SrmNasLocatorServiceTest" test -> PASS
GREEN: python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_d7_d10_sql_contract.py -k nas_locator -q -> PASS
INFO: backend-summary -> PASS，新增 `srm.nas-locator.blacklist-patterns` 全局配置键、黑名单接口、refresh 文件排除、`*` -> SQL LIKE 安全转换，以及 `srm:nas-locator:config` 增量菜单权限 SQL。
