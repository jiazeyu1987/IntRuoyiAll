# 20260630 DCC NAS 连接配置发布构建阻塞修复

## 任务目标

修复后端已提交 HEAD 在真实 `build-release` 中因 `NasConnectionConfig` 构造签名变更导致的 `testCompile` 阻塞，确保发布构建能够继续通过；本次只修复编译失败的测试与直接受影响的同类调用，不改变业务运行逻辑。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\experience-index.md`
  - 已按索引命中“已提交 git 版本发布 / clean worktree 发布输入 / 发布前置门禁”经验。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
  - clean worktree 暴露的依赖、锁文件、测试编译问题属于“已提交 HEAD 还不可发布”，必须先最小修复并正式提交，再回到发布闭环。
- `D:\ProjectPackage\Int\IntRuoyi\AGENTS.md`
  - 业务源码修复必须先建任务文档，按 BDD + 严格 TDD 记录 RED/GREEN 证据；只提交本任务直接相关改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；统一把旧的 4 参 `NasConnectionConfig` 调用更新为当前 6 参契约，消除 clean build 编译阻塞。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: clean release build must compile NAS config tests -> Given 后端已提交 HEAD 使用 6 参 `NasConnectionConfig` 契约，When 运行受影响模块测试编译或真实 `build-release`，Then DCC 与同类受影响模块测试必须使用当前 6 参构造并通过编译，不得因旧 4 参调用阻塞发布构建。

## 里程碑

1. 建立任务台账并记录发布阻塞背景。`COMPLETED`
2. 运行定向 RED，确认受影响旧构造调用范围。`COMPLETED`
3. 以最小修改修复受影响测试/调用并保留业务语义。`COMPLETED`
4. 运行定向 GREEN 验证并记录结果。`COMPLETED`
5. 回写任务结论并准备提交。`COMPLETED`

## 预期验证

- `mvn -pl yudao-module-dcc -Dtest=DccNasPermissionSnapshotCaptureServiceImplTest test` 通过。
- `mvn -pl yudao-module-srm -Dtest=SrmNasLocatorServiceTest test` 通过，或如存在独立前置阻塞则明确记录。
- 如有必要，补充运行与 `NasConnectionConfig` 直接相关的定向编译/测试验证。
- 修复后回到维护仓真实 `build-release` 时，不再因为本次旧构造调用而在 `testCompile` 阶段失败。

## 当前状态

COMPLETED：已将 DCC 测试与 SRM NAS locator 的旧 4 参 `NasConnectionConfig` 调用统一更新为当前 6 参契约，并处理 `SrmSupplierPortalApprovalTaskAdapter` 的编译阻塞；定向验证 `mvn -pl yudao-module-dcc -Dtest=DccNasPermissionSnapshotCaptureServiceImplTest test` 与 `mvn -pl yudao-module-srm "-Dtest=SrmNasLocatorServiceTest,SrmSupplierPortalApprovalTaskAdapterTest" test` 均已通过，可回到维护仓继续真实 `build-release`。
