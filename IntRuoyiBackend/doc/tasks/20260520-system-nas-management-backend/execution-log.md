# Execution Log: 系统管理 NAS 管理页签（后端）

BDD: 读取已保存 NAS 参数 -> Given 系统已保存 NAS 服务器、共享名、用户名和密码 / When 页面加载调用读取接口 / Then 后端返回当前参数值供页面初始化

BDD: 保存 NAS 参数 -> Given 管理员在页面填写 NAS 参数 / When 调用保存接口 / Then 后端将参数持久化到配置存储，并供后续浏览接口读取

BDD: 测试 NAS 连接 -> Given 页面表单填写了一组 NAS 参数 / When 点击测试连接按钮 / Then 后端使用这组参数建立 SMB 会话并返回测试成功结果；如果认证失败或共享/路径异常则显式失败

BDD: NAS 浏览接口使用已保存参数 -> Given 已保存 NAS 参数 / When 调用 `/infra/file/nas-files` / Then 后端使用已保存配置建立 SMB 会话，而不是硬编码常量

RED: mvn -pl yudao-module-infra "-Dtest=NasSettingsServiceTest,NasBrowserServiceImplTest,FileControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 当前仓库尚不存在 NAS 参数读取/保存/测试接口、配置持久化服务和系统管理菜单挂接

GREEN: mvn -pl yudao-module-infra "-Dtest=NasSettingsServiceTest,NasBrowserServiceImplTest,FileControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 13 tests green，已覆盖配置读取、配置保存、测试连接、浏览接口读取保存配置和控制器委托契约

GREEN: python -m pytest ruoyi-vue-pro\script\tests\test_system_nas_menu_sql.py -q -> PASS, 菜单 SQL 已声明系统管理下的 `NAS管理` 页面和 `infra:nas:*` 权限
