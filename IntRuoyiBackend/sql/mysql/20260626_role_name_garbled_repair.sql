-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=data; riskLevel=low
-- 修复角色管理页因错误编码写入导致的乱码角色名称。
-- 范围仅限已确认目标值明确的损坏记录，不修改权限码、菜单绑定和其他业务数据。

UPDATE `system_role`
SET
    `code` = CONVERT(0x454449544F52 USING utf8mb4),
    `name` = CONVERT(0xE5B195E58E85E7BC96E8BE91 USING utf8mb4),
    `remark` = CONVERT(0xE68F90E4BE9BE5B195E58E85E7BC96E8BE91 USING utf8mb4)
WHERE `id` = 910208
  AND `tenant_id` = 1
  AND (
      `code` = '????'
      OR `name` = '????'
      OR `remark` = '??????'
  );

UPDATE `system_role`
SET
    `code` = CONVERT(0x73686F77726F6F6D5F7075626C6963697479 USING utf8mb4),
    `name` = CONVERT(0xE4BC81E5AEA3E8A792E889B2 USING utf8mb4)
WHERE `id` = 910209
  AND `tenant_id` = 122
  AND (
      `name` = 'ä¼å®£è§’è‰²'
      OR HEX(CONVERT(`name` USING binary)) = 'C3A4C2BCC281C3A5C2AEC2A3C3A8C2A7E28099C3A8E280B0C2B2'
  );

UPDATE `system_role`
SET `name` = CONVERT(0x65444852E6BC94E7BB832DE689A7E8A18CE4BABA USING utf8mb4)
WHERE `id` = 910234
  AND `tenant_id` = 1
  AND (
      `name` = 'eDHR??-???'
      OR HEX(CONVERT(`name` USING binary)) = '654448523F3F2D3F3F3F'
  );

UPDATE `system_role`
SET `name` = CONVERT(0x65444852E6BC94E7BB832DE5AEA1E689B9E4BABA USING utf8mb4)
WHERE `id` = 910235
  AND `tenant_id` = 1
  AND (
      `name` = 'eDHR??-???'
      OR HEX(CONVERT(`name` USING binary)) = '654448523F3F2D3F3F3F'
  );

UPDATE `system_role`
SET `name` = CONVERT(0x65444852E6BC94E7BB832DE5BD92E6A1A3E59198 USING utf8mb4)
WHERE `id` = 910236
  AND `tenant_id` = 1
  AND (
      `name` = 'eDHR??-???'
      OR HEX(CONVERT(`name` USING binary)) = '654448523F3F2D3F3F3F'
  );

UPDATE `system_role`
SET `name` = CONVERT(0x65444852E6BC94E7BB832DE58FAAE8AFBB USING utf8mb4)
WHERE `id` = 910237
  AND `tenant_id` = 1
  AND (
      `name` = 'eDHR??-????'
      OR HEX(CONVERT(`name` USING binary)) = '654448523F3F2D3F3F3F3F'
  );
