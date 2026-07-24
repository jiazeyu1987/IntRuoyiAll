# 任务：展厅发布 fail-fast 与后端测试租户上下文修复

## 任务目标

- 修复展厅发布组装过程中 hall/product 物料异常被跳过的问题，改为阻断发布并暴露明确原因。
- 修复展厅 narration/release publisher 后端测试缺少租户上下文导致无法验证的问题。
- 保持多租户 fail-fast 语义，不引入默认租户、兼容降级或静默 fallback。

## BDD 场景

- BDD: 映射产品物料缺失阻断发布 -> Given 展厅已发布公司和展厅，且展厅映射了一个缺少当前发布音频物料的产品 / When 发布当前展厅 release / Then 发布失败并返回包含产品标识和缺失物料原因的异常，不跳过该产品或展厅。
- BDD: 展厅发布测试具备租户上下文 -> Given 后端 JUnit 直接调用 narration/release publisher service / When 测试读写租户隔离表 / Then 测试显式设置并清理测试租户上下文，验证失败只来自业务断言而不是缺少租户编号。

## 里程碑

- [x] M1：创建任务文档和 BDD 场景。
- [x] M2：补充 RED 回归测试并复现现有失败。
- [x] M3：最小修复测试租户上下文和发布组装 fail-fast 行为。
- [x] M4：运行目标后端回归并记录 GREEN 证据。
- [x] M5：收尾清理预览并提交本任务直接改动。

## 预期验证

- RED/GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleasePublisherServiceTest#shouldFailFastWhenMappedProductNarrationIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductContentTest,ShowroomCompanyContentTest,ShowroomHallContentTest,ShowroomVersionBundleServiceTest,ShowroomReleasePublisherServiceTest,ShowroomProductCoverImageServiceTest,ShowroomPersistentNarrationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 当前状态

completed - implementation, verification, cleanup preview, and commit-ready evidence are complete.

## 约束

- 仅修改当前任务相关后端源码、测试和任务文档。
- 不修改芋道源码/admin 业务数据。
- 不通过默认租户、mock 成功或跳过断言掩盖失败。

## Cleanup Keep

- `doc/tasks/20260528-showroom-release-failfast-tenant-tests/bug-regression-evidence.md`
