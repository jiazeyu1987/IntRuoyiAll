# Execution Log: Infra 增加带账号密码的 NAS 浏览接口

BDD: NAS共享根目录浏览 -> Given 后端持有 NAS 服务器、共享名、用户名和密码 / When 调用 NAS 浏览接口且 path 为空 / Then 后端建立 SMB 会话并返回共享根目录下的文件夹与文件列表

BDD: NAS子目录浏览 -> Given path 指向 NAS 共享下的相对子目录 / When 调用 NAS 浏览接口 / Then 后端返回该子目录内容，并给出 currentPath、parentPath 和 rootPath

BDD: NAS认证失败显式失败 -> Given NAS 用户名或密码错误 / When 后端建立 SMB 会话失败 / Then 接口显式返回 NAS 认证失败错误，不返回空列表冒充成功

BDD: NAS路径不存在显式失败 -> Given 请求 path 对应的共享内路径不存在 / When 后端读取目录 / Then 接口显式返回路径不存在错误

RED: mvn -pl yudao-module-infra "-Dtest=NasBrowserServiceImplTest,FileControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 当前仓库尚不存在带账号密码的 NAS 浏览服务、依赖、VO、错误码与控制器接口

GREEN: mvn -pl yudao-module-infra "-Dtest=NasBrowserServiceImplTest,FileControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 6 tests green，已覆盖共享根浏览、子路径标准化、认证失败、路径不存在和控制器委托契约

GREEN: Test-NetConnection 172.30.30.4 -Port 445 -> PASS, 当前机器到目标 NAS 的 SMB 端口可达
