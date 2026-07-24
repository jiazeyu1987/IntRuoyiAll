# Execution Log: 六路识别页签增加一键清空电子批记录报表按钮

BDD: 清空电子批记录目录返回删除数量 -> Given 六路识别页签需要一键清空 `电子批记录` 文件夹下全部报表 When 前端调用新的批量删除接口 Then 后端应删除该目录下全部 Jimu 报表和对应 `MES` 元数据，并返回报表删除数与元数据删除数。
BDD: 目录缺失时明确失败 -> Given 当前运行环境缺少 `电子批记录` 目录 When 用户点击一键清空 Then 后端必须显式返回目录不存在错误，而不是静默成功或偷偷创建目录。
RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, 缺少批量删除响应 VO、controller/service/gateway 新方法和目录缺失错误码，测试编译直接报缺口。
GREEN: 同一 Maven 定向测试命令 -> PASS, `MesProBatchRecordReportControllerTest` 3/3、`MesProBatchRecordReportServiceImplDbTest` 12/12 全通过。
GREEN: runtime DB verification -> PASS, `DELETE /delete-all` 真正物理删除了电子批记录元数据，最终 `jimu_report`、active `mes_pro_batch_record_report` 与 soft-deleted metadata 均为 `0`。
