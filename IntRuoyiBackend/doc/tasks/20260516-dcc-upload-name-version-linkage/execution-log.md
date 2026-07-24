BDD: 历史文件名称可选联动 -> Given 上传人进入 DCC 上传页并先选中文件类别 When 前端请求该类别下的历史文件名称 Then 后端返回可选文件名称列表，并为每个名称带出当前版本号

BDD: 无历史名称时空列表 -> Given 选中的文件类别下还没有任何受控文件主链 When 前端请求历史文件名称 Then 后端返回空列表，不返回 mock 或默认版本

BDD: 已存在同名当前版本时可联动 -> Given 某文件类别下存在历史文件主链且已记录当前版本 When 前端选择该文件名称 Then 前端能够使用接口返回的当前版本号进行展示和后续编辑

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileUploadNameOptionQueryServiceTest,DccControlledFileUploadNameOptionApiTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, test compile could not find `DccControlledFileUploadNameOptionRespVO`, proving the upload-name option contract did not exist yet

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileUploadNameOptionQueryServiceTest,DccControlledFileUploadNameOptionApiTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 4 tests passed and verified the new upload-name option controller delegation plus query-service sorting, empty-list, and missing-category behavior

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS, rebuilt the runnable `yudao-server.jar` with the new DCC upload-name option endpoint included

GREEN: manual backend runtime switch -> PASS, replaced the live `48081` backend with `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-upload-name-version-*.jar` after `restart-ruoyi.bat` was blocked by the local Docker Desktop daemon being unavailable
