# 执行日志：修复超级管理员看不到展厅产品列表

BDD: super admin should be able to view showroom product page -> Given 当前登录用户具备系统超级管理员能力 / When 打开展厅产品管理页 / Then 即使没有 `showroom_publicity` 角色，也不应被收缩为 0 条产品

BDD: non-publicity scoped users should still remain restricted -> Given 当前用户既不是企宣也不是超级管理员 / When 查询展厅产品页 / Then 仍应只看到被指派或审批可见的产品

RED: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\pom.xml "-Dtest=ShowroomHttpApiIntegrationTest#superAdminShouldBypassScopedVisibilityForProductPage" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，断言 `expected: <1> but was: <0>`，证明当前 `super_admin` 用户仍被错误过滤成空产品列表。
GREEN: 最小修复 -> PASS，`ShowroomAdminController` 现在把 `showroom_publicity` 或 `super_admin` 都视为展厅高权限用户。
GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\pom.xml "-Dtest=ShowroomHttpApiIntegrationTest#superAdminShouldBypassScopedVisibilityForProductPage" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\pom.xml "-Dtest=ShowroomHttpApiIntegrationTest#wholeProductAssignmentShouldExposeFillingStatusAndAssignedEditorAccess" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，普通受限编辑仍保持原有可见性逻辑。
GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS，后端主 jar 已重打。
GREEN: `cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS，本地运行时已重启到新 jar。
GREEN: `Get-CimInstance Win32_Process | Where-Object { $_.ProcessId -eq 49380 }` -> PASS，当前后端进程命令行指向 `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-20260521-111450.jar`。
GREEN: `GET http://127.0.0.1:48081/actuator/health` -> PASS，返回 `200` 与 `{"status":"UP"}`。
GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-super-admin-product-visibility --mode preview` -> PASS，preview 结果 `ready`。
