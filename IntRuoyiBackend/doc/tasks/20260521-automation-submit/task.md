# 任务：提交展厅公开展示合同后端改动

## Goal

在 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 中整理当前可验证的展厅后端改动，先修复阻塞编译/契约问题，再仅提交本次直接相关文件。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-app-config-company-fields\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-public-display-pure-frontstage\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-automation-submit\**`

## Non-Scope

- 不提交其他未跟踪 task 目录。
- 不为缺失前端代码制造空提交。
- 不引入 fallback、mock 或静默降级。

## Previous Task Check

- Previous same-repo task records reviewed:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-app-config-company-fields\task.md`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-public-display-pure-frontstage\task.md`
- Impact:
  - 当前待提交代码直接对应上述 showroom 合同任务，需先补齐编译与契约一致性，再提交最小集合。

## Milestones

1. 识别当前后端待提交代码与相关任务文档。
2. 记录 RED，修复编译和公开展示合同不一致问题。
3. 跑通定向 GREEN 验证。
4. 仅暂存本次直接相关文件并完成提交。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest,ShowroomHttpApiIntegrationTest#publicFrontstageDisplayEndpointsShouldBeAnnotatedPermitAllExceptHome" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `git diff --cached --stat`
- `git commit -m "任务: 提交展厅公开展示合同修复"`

## Current Status

- Status: Completed
- Completed work:
  - 已定位并修复 `CompanyPayload` 记录定义与运行时构造不一致导致的编译失败。
  - 已补齐 `company` / `hall` / `product` / `narration` 展示接口的 `@PermitAll` 注解，使其与当前 Website 公开展示合同一致。
  - 已将独立 `ShowroomAppConfigCompanyFieldsContractTest` 的权限断言同步到当前公开展示面。
  - 已筛选提交范围，仅保留本次直接相关的 7 个后端文件。
- Remaining blockers:
  - None.

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest,ShowroomHttpApiIntegrationTest#publicFrontstageDisplayEndpointsShouldBeAnnotatedPermitAllExceptHome" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `git diff --cached --stat`
