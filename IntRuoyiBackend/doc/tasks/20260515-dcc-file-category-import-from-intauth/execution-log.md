# Execution Log: DCC 文件类别一次性导入 IntAuth

BDD: 管理员一次性导入 IntAuth 文件类别 -> Given IntAuth 内部文件类别接口返回有效类别列表 / When 管理员触发 DCC 文件类别导入 / Then 本地 `dcc_file_category` 写入缺失类别并返回导入结果 / And 后续文件类别列表继续来自本地表。

BDD: 同名本地类别在导入时被复用 -> Given 本地 DCC 已存在与 IntAuth 同名的文件类别并挂有治理数据 / When 管理员触发导入 / Then 系统复用该本地类别行并写入来源标识 / And 既有治理挂载保持不丢失。

BDD: IntAuth 导入配置缺失时失败 -> Given `yudao.dcc.int-auth.base-url` 或 `yudao.dcc.int-auth.internal-service-token` 缺失 / When 管理员触发导入 / Then 后端返回明确的 IntAuth 文件类别导入配置错误 / And 不写入任何本地类别。

BDD: IntAuth 文件类别 payload 异常时失败 -> Given IntAuth 返回的 `file_categories` payload 缺少必需字段或结构非法 / When 管理员触发导入 / Then 后端返回明确的响应非法错误 / And 不写入任何本地类别。

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccFileCategoryAdminServiceImplTest,DccIntAuthFileCategoryClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, test compile stopped at missing symbol `DccIntAuthFileCategoryClient`, which proves the one-time import client and service contract did not exist yet.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccFileCategoryAdminServiceImplTest,DccIntAuthFileCategoryClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 9 focused DCC file-category import tests green after adding the IntAuth import client, explicit import endpoint, and local-only list preservation behavior.
