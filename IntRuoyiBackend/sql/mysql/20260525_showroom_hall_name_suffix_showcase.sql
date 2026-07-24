-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
UPDATE `showroom_hall`
SET `name` = CONCAT(LEFT(`name`, CHAR_LENGTH(`name`) - CHAR_LENGTH('展厅')), '展柜')
WHERE `deleted` = b'0'
  AND `name` IS NOT NULL
  AND RIGHT(`name`, CHAR_LENGTH('展厅')) = '展厅';

UPDATE `showroom_hall`
SET `name_en` = CONCAT(LEFT(`name_en`, CHAR_LENGTH(`name_en`) - CHAR_LENGTH('Hall')), 'Showcase')
WHERE `deleted` = b'0'
  AND `name_en` IS NOT NULL
  AND RIGHT(`name_en`, CHAR_LENGTH('Hall')) = 'Hall';
