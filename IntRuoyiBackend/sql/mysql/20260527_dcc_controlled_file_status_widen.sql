-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
ALTER TABLE dcc_controlled_file
    MODIFY COLUMN status varchar(64) NOT NULL COMMENT '受控文件状态';
