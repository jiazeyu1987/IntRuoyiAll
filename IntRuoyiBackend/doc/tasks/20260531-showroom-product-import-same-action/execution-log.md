# 执行日志：展厅产品导入相同产品选择覆盖或跳过（后端）

BDD: 选择跳过相同产品 -> Given Excel 行与当前产品所有导入字段一致 / When 调用导入接口并指定跳过相同产品 / Then 该产品进入跳过列表，不新增 revision。

BDD: 选择覆盖相同产品 -> Given Excel 行与当前产品所有导入字段一致 / When 调用导入接口并指定覆盖相同产品 / Then 该产品进入成功列表，并发布一个新 revision。

BDD: 非法相同产品处理方式失败 -> Given 导入请求传入未知处理方式 / When 调用导入接口 / Then 请求快速失败并返回明确错误。

RED: mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 新增测试调用 importProductExcel(file, sameProductAction) 时控制器仍只有旧签名，编译错误提示参数列表不匹配。

GREEN: mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 25, Failures: 0, Errors: 0, Skipped: 0。

GREEN: mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductContentTest,ShowroomPersistentContentServiceTest,ShowroomFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 46, Failures: 0, Errors: 0, Skipped: 0。

实现记录：控制器解析必填 sameProductAction，支持 SKIP/OVERWRITE；运行时仅在无变化且 SKIP 时跳过，OVERWRITE 走原有发布链路并复用讲解稿/音频。

验证记录：相同产品覆盖用例补齐当前 revision 的中英文讲解稿夹具，确保覆盖发布经过真实发布链路；非法处理方式用例验证快速失败。

E2E-BLOCKED: 浏览器打开 http://localhost:8081 后停留在启动页；检查本机端口发现 48081/48082 无后端监听。`output/runtime/backend-20260531-201110.out.log` 记录后端启动失败：`DCC download encryption config is missing or invalid: base64-key must be valid Base64`。未使用 mock 或临时配置绕过。
