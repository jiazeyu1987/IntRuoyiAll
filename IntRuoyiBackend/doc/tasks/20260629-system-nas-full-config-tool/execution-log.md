# 执行日志：系统 NAS 配置工具扩展为完整连接参数台（后端）

- BDD: 保存 NAS 配置时持久化完整 SMB 参数 -> Given 用户提交带 domain、port、authType 等字段的 NAS 配置 / When 后端保存 / Then 所有字段都按正式 config key 持久化，并在读取接口中完整返回。
- BDD: 测试连接时使用完整 SMB 参数 -> Given 用户提交完整 NAS 参数 / When 后端执行 testConnection / Then NasConnectionConfig 应包含新增字段，SMBJ 认证使用对应 domain/port。
- BDD: 复用链路重建 NAS 配置对象时不丢字段 -> Given Runtime Control 或 SRM NAS 定位从当前配置派生 share 级连接 / When 系统重建 NasConnectionConfig / Then 新增字段与原配置保持一致。
- RED: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-infra "-Dtest=NasSettingsServiceTest,NasBrowserServiceImplTest,FileControllerTest" test` -> FAIL，`NasConnectionConfig` 扩展后 `RuntimeControlServiceImpl` 与部分测试夹具仍使用旧 4 参构造器。
- GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-infra "-Dtest=NasSettingsServiceTest,NasBrowserServiceImplTest,FileControllerTest" test` -> PASS，29 个测试通过；确认新增 `port/domain` 已贯穿 VO、设置保存、SMBJ 认证、Runtime Control 与 SRM share 派生链路。
- IMPLEMENTATION: `FileNasConfig{SaveReqVO,RespVO}`、`NasSettingsServiceImpl`、`NasConnectionConfig`、`NasBrowserServiceImpl` -> 新增正式字段 `port/domain`；`SMBClient.connect(server, port)` 与 `AuthenticationContext(..., domain)` 已使用新参数。
- IMPLEMENTATION: `RuntimeControlServiceImpl`、`RuntimeReleasePackageNasRepository`、`RuntimeBackupNasRepository`、`SrmNasLocatorServiceImpl` -> 所有从当前配置重建 `NasConnectionConfig` 的链路已带上 `port/domain`，避免中间丢参。
