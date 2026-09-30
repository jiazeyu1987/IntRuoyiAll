# DCC 最小主流程 M01 验证报告

## Scope

本报告只覆盖 M01 的非空项目编码唯一性，不代表 M02—M30 已实现。

## BDD

- `BDD-M01-project-code-unique`：不同项目名称不能复用同一非空项目编码。

## RED / GREEN / REGRESSION

- RED：新增跨项目名称复用编码测试失败，证明旧实现只按名称+编码判重。
- GREEN：跨项目名称复用同一非空编码被 `PROJECT_CODE_DUPLICATE` 拒绝。
- REGRESSION：`DccProjectCodeServiceImplTest` 与 `DccProductOnboardingServiceImplTest` 通过。

## Implemented Boundary

- `DccProjectCodeMapper.selectByProjectCodeExcludingId` 提供非空编码独立查询。
- `DccProjectCodeServiceImpl.validateProjectCodeUnique` 对非空编码按独立身份判重，对空编码保留兼容规则。
- 未修改项目代码导入的名称+编码匹配语义。

## Evidence Boundary

- 不执行真实数据库写入、E2E、服务启停、远程操作或 Git 提交/推送。
