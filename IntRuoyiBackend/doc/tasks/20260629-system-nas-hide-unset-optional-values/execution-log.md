# 执行日志：NAS 未设置可选参数不作为当前已配置值返回（后端）

- BDD: 未设置端口时读取接口不返回伪配置值 -> Given infra.nas.port 当前没有真实配置 / When 前端读取 NAS 配置 / Then 响应里的 port 为空，而不是默认返回 445。
- BDD: 运行时读取 NAS 配置时仍使用系统默认端口 -> Given infra.nas.port 当前没有真实配置 / When 后端构建实际 NAS 连接参数 / Then NasConnectionConfig 仍使用默认 SMB 端口，不影响现有连接链路。
- BDD: 清空可选参数时持久层不保留空壳配置 -> Given 用户把 NAS 端口或域清空保存 / When 后端处理保存请求 / Then 对应可选参数配置被删除，不再作为“当前已配置值”返回。
- RED: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-infra "-Dtest=NasSettingsServiceTest,FileControllerTest,NasBrowserServiceImplTest" test` -> FAIL，原实现会把未配置端口默认读取成 `445` 并作为当前配置返回，且空可选参数保存后会保留空壳配置。
- GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-infra "-Dtest=NasSettingsServiceTest,FileControllerTest,NasBrowserServiceImplTest" test` -> PASS，32 个测试通过；确认读取接口不再把默认端口伪装成已配置值，运行时连接仍使用默认 SMB 端口，清空 `port/domain` 时会删除对应配置项。
- IMPLEMENTATION: `NasSettingsServiceImpl`、`FileNasConfig{RespVO,SaveReqVO}` -> `getNasConfig()` 改为对未设置端口返回空值；保存端口/域时空值走删除语义；`getRequiredNasConfig()` 与 `toConnectionConfig()` 继续通过 `NasConnectionConfig` 保留默认端口运行时行为。
