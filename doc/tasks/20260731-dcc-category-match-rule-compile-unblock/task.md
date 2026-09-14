# 20260731 DCC 类别匹配规则编译解阻

## Task Goal

修复 `origin/int_main` 合入后 DCC 项目代码测试编译失败的问题，补齐正式的文件类别匹配规则表、DO、Mapper 和服务读取链路，使 eDHR 一线填写合并门禁中的 `mvn -pl yudao-module-mes -am ... test` 不再被无关 DCC `testCompile` 阻塞。

## Milestones

- [x] 建立任务记录、复现 DCC 编译阻塞并确认缺失契约。
- [x] 补齐 DCC 文件类别匹配规则正式 schema、DO、Mapper 与测试夹具。
- [x] 让项目代码分类服务读取可维护规则，并保持既有硬编码别名作为存量内置规则。
- [x] 复跑 DCC 目标测试、eDHR 后端门禁和前端/eDHR验证。
- [x] 更新 eDHR 收尾记录，完成推送与后续 closeout。

## Expected Verification

- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_file_category_match_rule_sql.py`
- `mvn -pl yudao-module-dcc -am "-Dtest=DccProjectCodeServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRouteProcessTemplateBindingSourceTest,MesFrontlineWorkstationPostRouteBindingSourceTest,MesFrontlineDeviceAccountContextServiceTest,MesFrontlineEmployeeSwitchServiceTest,MesFrontlineTemplateResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- eDHR 前端静态合同与 `pnpm ts:check` 复验。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺失规则表时运行库应按正式迁移补齐，不在服务中吞掉 Mapper/SQL 异常。
- `是否从根因和长期维护角度解决`：是。补正式 schema + DO/Mapper + 服务读取链路，而不是删除测试或跳过 DCC 依赖模块编译。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- Maven `-am` 依赖模块 `compile/testCompile` 失败属于当前融合门禁失败；不得因为当前任务源码未直接修改依赖模块而跳过。
- DCC schema 变更必须带 release-migration metadata、测试夹具和静态 SQL 契约。
