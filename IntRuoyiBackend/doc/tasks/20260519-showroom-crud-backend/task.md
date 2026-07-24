# Task: 展厅产品与展厅 CRUD 后端契约

## Goal

补齐展厅产品和展厅管理的一期 CRUD 后端契约，支持新增、删除、查找、修改，并保证列表每页最多返回 20 条。

## Milestones

- [x] 记录 BDD/TDD 目标
- [x] 补充失败测试
- [x] 实现查询参数、删除接口和分页上限
- [x] 运行后端测试
- [x] 提交并合并回主分支

## Expected Verification

- `mvn -pl yudao-module-showroom -Dtest=ShowroomProductContentTest,ShowroomHallContentTest,ShowroomHttpApiIntegrationTest test`

## Status

Completed, verified, and merged into `int_main`.

## Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductContentTest,ShowroomHallContentTest,ShowroomHttpApiIntegrationTest" test` -> PASS, 9 tests.
- 合并后主工作区复验通过：`mvn -pl yudao-module-showroom "-Dtest=ShowroomProductContentTest,ShowroomHallContentTest,ShowroomHttpApiIntegrationTest" test` -> PASS, 9 tests.
