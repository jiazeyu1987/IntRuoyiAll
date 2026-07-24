# 任务：展柜管理数据包导入导出（后端）

- Task ID: `20260630-showroom-hall-config-package`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

为 `yudao-module-showroom` 提供正式的展柜配置包 zip 导出/导入能力，覆盖展柜基础配置、关键词中英文对照、展项映射、背景图资产、live 预览图版本与 live 中英文语音版本，并在导入时按业务键执行完全替换与 fail-fast 校验。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-dcc-admin-full-config-package\task.md`
- 状态：`blocked`
- 处理说明：已因用户优先级切换到展柜配置包需求而显式阻塞；本次只在 `yudao-module-showroom` 范围推进，不混入 DCC 全量包。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md` 与 `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 任务文档、执行日志、zip manifest、测试脚本输出统一按 UTF-8 处理。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 若进行真实租户验收，必须先跑官方最小登录路径。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；由后端统一维护 zip 配置包合同与完整替换逻辑，不靠前端拼装或局部更新伪装成功。
- `是否存在临时补丁或绕过`：否。缺引用、缺资产、缺 live 文件时直接阻断。

## BDD 场景

- `BDD: 后端可导出展柜配置 zip 包 -> Given 当前租户存在展柜、关键词、背景图、live 预览图和 live 中英文语音 / When 调用展柜配置包导出接口 / Then 后端返回单个 zip 包，其中 manifest 与资产内容完整可追溯。`
- `BDD: 导入按业务键完全替换展柜与关键词 owned scope -> Given 目标租户已有旧展柜和旧关键词 / When 导入新的展柜配置包 / Then hallCode 与关键词集合按包内容重建，包外旧数据在 owned scope 内被移除。`
- `BDD: 导入在缺 productCode 或 awardCode 引用时回滚 -> Given 配置包展柜映射引用目标租户不存在的产品或奖项 / When 导入执行 / Then 系统报出缺失业务键并整包回滚。`
- `BDD: 导入重建背景图、preview live 与 narration live -> Given 配置包中包含背景图、preview 资产和 hall live narration / When 导入成功 / Then 目标租户生成新的文件与新的 live 版本记录，但业务内容与源包一致。`

## Milestones

1. M1：建立后端任务文档并锁定配置包合同与边界。`completed`
2. M2：补 RED 测试锁定 zip manifest、权限与回滚合同。`completed`
3. M3：实现 service、controller 与导入摘要响应。`completed`
4. M4：运行定向测试并回填 evidence。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomHallConfigPackageServiceTest,ShowroomHallConfigPackageControllerPermissionTest" -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Blockers

- 无。此前目标租户缺失源包引用 `productCode` 的前置阻塞已在产品主数据同步后解除，根任务真实回导验收现已通过。

## Completed Work

- 已新增 `GET /admin-api/showroom/hall/config-package/export` 与 `POST /admin-api/showroom/hall/config-package/import`。
- 已实现 zip 包 manifest、背景图、live 预览图、live 中英文语音的导出与导入重建。
- 已实现关键词集合替换、展柜按 `hallCode` 替换、包外旧展柜删除、引用业务键整包校验回滚。
- 已补空关键词/空展柜集合的完全替换回归，确保目标租户可被替换到与源包一致的空集合状态。
- 已把导入期的预期业务失败从原始 `IllegalStateException` 收口为 `ServiceException`，使前端和真实 E2E 能直接拿到缺失 `productCode/awardCode` 或缺资产错误，而不是笼统 `500 系统异常`。

## Verification Evidence

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom -Dtest=ShowroomHallConfigPackageServiceTest,ShowroomHallConfigPackageControllerPermissionTest -Dsurefire.failIfNoSpecifiedTests=false test` -> `BUILD SUCCESS`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package` -> `BUILD SUCCESS`
- 真实运行态：重启本机 backend 后，`node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\showroom-hall-config-package-real.e2e.js` 已完成 `芋道源码/admin` 导出 -> `测试租户/aoteman` 导入 -> `测试租户` 回导 -> `manifest/asset hash` 深比较闭环，结果 `PASS`。
- 真实运行态摘要：`hallCount=10`、`keywordCount=34`、`previewAssetCount=10`、`narrationCount=20`、`backgroundAssetCount=0`、`validatedProductCount=164`、`validatedAwardCount=46`。
