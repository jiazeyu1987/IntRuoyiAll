# 执行记录

## 2026-09-18

- 已读取根 AGENTS、后端开发、数据库规则、编码规则和任务收尾规则。
- 已核对 M01 现状：`DccProjectCodeServiceImpl.validateProjectCodeUnique` 当前调用 `selectByProjectNameAndProjectCodeExcludingId`，项目编码尚未独立判重。
- 已建立 BDD-M01-project-code-unique，下一步先补 RED 测试。

## RED / GREEN / REGRESSION

- RED：`mvn.cmd -q -pl yudao-module-dcc -am "-Dtest=DccProjectCodeServiceImplTest#createShouldRejectDuplicateProjectCodeAcrossProjectNames" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，1 test 失败；当前实现允许不同项目名称复用 `CODE-SHARED`。
- GREEN：同一命令在增加 `selectByProjectCodeExcludingId` 并切换新建/编辑判重后 -> PASS。
- REGRESSION：`mvn.cmd -q -pl yudao-module-dcc -am "-Dtest=DccProjectCodeServiceImplTest,DccProductOnboardingServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；项目代码服务与产品建档服务均通过。

## Implementation Boundary

- 非空项目编码按租户内独立身份判重。
- 空项目编码继续按旧的项目名称+空编码兼容规则处理。
- 导入流程仍按旧项目名称+项目编码匹配，本切片不扩大导入行为。
