# R06既有入口行为RED冻结设计

状态：FROZEN_TEST_ONLY_WAITING_ROOT_MAVEN；9个测试已落到既有类，未执行Java/Maven，生产未改。

Root已批准6文件候选范围，但要求先由Root运行有效行为RED，再通知GREEN。当前只补3个既有测试类，不创建新生产类或引用尚不存在的类型/字段。

## Given / When / Then

1. Given真实ImageIO生成的双色PNG和合法CONVERT授权，When调用现有convertToPdf(FileDO)，Then即使OnlyOffice未配置也生成包含实际PNG像素的真实PDF。现实现会因OnlyOffice配置缺失失败。
2. Given中文UTF-8 TXT，When本地文本转换未配置字体，Then明确报告yudao.dcc.conversion.text-font，不能误报OnlyOffice配置；转换权限仍先校验。
3. Given合法SOURCE、类别/项目/独立会话及真实PNG/TXT bytes，When调用现有UploadService，Then建立精确原名/MIME/字节/hash的票据并返回IMAGE/TEXT。现实现因扩展名拒绝失败。
4. Given既有finalizing候选与真实PNG原件，When经既有finalization调用真实当前转换器及真实DccPdfStampService，Then真正盖章PDF物理ID绑定发布/盖章/签名，原件bytes/hash保持。当前会在OnlyOffice配置处失败。
5. GivenTXT缺字体，When最终受控转换，Then准确字体错误且无发布/签名绑定/master生效。

## 测试夹具边界

- 只新增测试方法局部PNG（程序生成）、中文文本常量、已有FileService/BusinessFileAccessService模拟存储边界；不模拟转换PDF或盖章PDF结果。
- PNG成功用例不依赖字体。TXT字体缺失负例不需任何OS字体；新纯转换器正例/中文Renderer/TextStripper测试待实现后按Root明确字体Resource补齐。
- Upload测试复用既有tenant31/user99/category10/合法NEW_UPLOAD项目30分类20、原transaction synchronization/类别授权；只在局部补精确session与返回ticket，保留bytes/hash捕获。
- Finalization复用已有锁定/事务/候选fixture，仅注入真实当前conversion实例和真实DccPdfStampService，模拟存储metadata/文件服务边界。无API/DB业务动作。
- 每个新增方法以r06开头。Root执行指定方法，不整体运行已通过业务。测试结果应是业务断言失败/ServiceException被assertDoesNotThrow转为FAIL，不能有缺类/缺字段/编译错误冒充RED。

## 精确命令（Root串行执行）

工作目录：IntRuoyiBackend。

`mvn -pl yudao-module-dcc -am -Dtest=DccOnlyOfficeDocumentPdfConversionServiceTest#r06*,DccControlledFileUploadApiTest#r06*,DccControlledFileFinalizationServiceImplTest#r06* -Dsurefire.failIfNoSpecifiedTests=false test`

PowerShell应将完整-Dtest值作为一个引用参数；Maven程序使用Root既有安装路径。Root保存进程退出码、Surefire counts/XML、准确错误归因。此线程不执行Maven/编译。

冻结交付前记录三个测试文件LF SHA256、方法列表和最小fixture说明；Root回报后再更新本文件为有效RED。生产6文件在收到GREEN通知前不修改。

## 冻结文件

- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccOnlyOfficeDocumentPdfConversionServiceTest.java：3项；LF SHA256 9236dd27b7a099bdcce49aaf9a243a8e084b5dfa62b0fe84a9e0b7fd0bb29b6b
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileUploadApiTest.java：4项；LF SHA256 3995d604b111972edf1e59f9fccf114353d45a590f2b4112dacb392e5b3b3275
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImplTest.java：2项；LF SHA256 c3c51d68987b48d04c6fe15521ed125a6c66480f88221b629f546e09f5fc6622

方法与精确命令参数见run-20261019-r06-design/direct-red-freeze.json。预计6项因明确行为缺口FAIL、3项拒绝守卫PASS；仅为预期，不是已执行结果。无OS字体依赖，无新类/新字段反射引用；PNG测试调用真实当前转换器/真实盖章服务，存储等外部边界沿既有单元fixture模拟。
