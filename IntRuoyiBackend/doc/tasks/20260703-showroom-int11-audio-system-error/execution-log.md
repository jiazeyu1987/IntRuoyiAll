# 执行日志：修复产品语音未生成时系统异常

- BDD: 产品语音读取缺失应清晰展示 -> Given 产品修订缺少中英文语音 / When 打开产品语音弹窗 / Then 页面应显示未生成或可生成状态，不应提示系统异常。
- RED: test-server-api -> FAIL，`/admin-api/showroom/narration/get?targetType=PRODUCT&targetId=906&audienceType=PUBLIC&language=ZH` 返回 `code=500`、`msg=系统异常`。
- RED: test-server-api -> FAIL，`/admin-api/showroom/narration/get?targetType=PRODUCT&targetId=906&audienceType=PUBLIC&language=EN` 返回 `code=500`、`msg=系统异常`。
- RED: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductNarrationRegressionTest#getProductNarrationShouldReturnBusinessErrorWhenNarrationMissing" test` -> FAIL，初次失败暴露测试上下文缺少新构造依赖 `ShowroomHallConfigPackageService`，补 `@MockBean` 后进入业务断言。
- GREEN: backend-fix -> PASS，`ShowroomApiRuntime#getNarration` 在缺语音时抛出 `ServiceException` / `BAD_REQUEST`，消息保留 `SHOWROOM_TARGET_NOT_FOUND: narration not found`，供前端现有逻辑识别。
- GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductNarrationRegressionTest#getProductNarrationShouldReturnBusinessErrorWhenNarrationMissing" test` -> PASS。
- BLOCKER: test-server-generate-audio -> 测试服生成语音接口仍返回系统异常，后端日志显示 `Cannot run program "codex" (in directory "/yudao-server"): No such file or directory`，说明生成讲解稿/语音链路缺运行环境前置，不属于本次读取缺记录修复。