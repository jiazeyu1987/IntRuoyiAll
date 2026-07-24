# 执行日志：预览重排缺少物料不阻断（后端）

## 2026-06-30

### BDD / TDD

- `BDD: 自动排产预览缺少生产用料清单时返回告警 -> Given 待排产工单缺少生产用料清单 / When 调用 preview / Then blockingIssueCount 为 0、shortageCount 增加并保留 MATERIAL_DEMAND issue。`

### 执行命令

- `rg -n "MATERIAL_DEMAND|工单缺少生产用料清单|缺少生产用料清单" ...` -> PASS，定位到 `MesProAutoScheduleServiceImpl` 与 `MesProAutoScheduleServiceImplTest`
- `Get-Content -Encoding utf8 ...MesProAutoScheduleServiceImpl.java` / `...MesProAutoScheduleServiceImplTest.java` -> PASS，确认当前缺少生产用料清单会写入 blocking issue
- `RED: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest#preview_shouldWarnWhenProductionMaterialListMissing" -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL, blockingIssueCount 仍为 1`
- `GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest#preview_shouldWarnWhenProductionMaterialListMissing" -Dsurefire.failIfNoSpecifiedTests=false test -> PASS`
- `GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest" -Dsurefire.failIfNoSpecifiedTests=false test -> PASS`

### 当前状态

- 已完成最小后端修复与定向回归验证。
