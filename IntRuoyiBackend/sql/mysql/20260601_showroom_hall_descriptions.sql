-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
UPDATE `showroom_hall`
SET
  `description` = CASE
    WHEN `description` IS NULL OR TRIM(`description`) = ''
      THEN '集中展示心内介植入相关产品，覆盖冠脉介入、通路建立及术中辅助器械。'
    ELSE `description`
  END,
  `description_en` = CASE
    WHEN `description_en` IS NULL OR TRIM(`description_en`) = ''
      THEN 'Presents cardiac interventional implant products, covering coronary intervention, access establishment, and procedure-support devices.'
    ELSE `description_en`
  END
WHERE `deleted` = b'0'
  AND `hall_code` = 'hall_01';

UPDATE `showroom_hall`
SET
  `description` = CASE
    WHEN `description` IS NULL OR TRIM(`description`) = ''
      THEN '集中展示心脏植入及相关介入产品，覆盖结构性心脏病、心脏通路及配套器械。'
    ELSE `description`
  END,
  `description_en` = CASE
    WHEN `description_en` IS NULL OR TRIM(`description_en`) = ''
      THEN 'Presents cardiac implant and related interventional products, covering structural heart disease, cardiac access, and supporting devices.'
    ELSE `description_en`
  END
WHERE `deleted` = b'0'
  AND `hall_code` = 'hall_02';

UPDATE `showroom_hall`
SET
  `description` = CASE
    WHEN `description` IS NULL OR TRIM(`description`) = ''
      THEN '集中展示外周介植入相关产品，覆盖主动脉、外周血管及分支血管治疗器械。'
    ELSE `description`
  END,
  `description_en` = CASE
    WHEN `description_en` IS NULL OR TRIM(`description_en`) = ''
      THEN 'Presents peripheral interventional implant products, covering aortic, peripheral vascular, and branch-vessel treatment devices.'
    ELSE `description_en`
  END
WHERE `deleted` = b'0'
  AND `hall_code` = 'hall_03';

UPDATE `showroom_hall`
SET
  `description` = CASE
    WHEN `description` IS NULL OR TRIM(`description`) = ''
      THEN '集中展示神经介植入相关产品，覆盖颅内血管通路、取栓、输送及支撑器械。'
    ELSE `description`
  END,
  `description_en` = CASE
    WHEN `description_en` IS NULL OR TRIM(`description_en`) = ''
      THEN 'Presents neuro interventional implant products, covering intracranial vascular access, thrombectomy, delivery, and support devices.'
    ELSE `description_en`
  END
WHERE `deleted` = b'0'
  AND `hall_code` = 'hall_04';

UPDATE `showroom_hall`
SET
  `description` = CASE
    WHEN `description` IS NULL OR TRIM(`description`) = ''
      THEN '集中展示外泌体应用与聚焦超声相关产品，覆盖无创透皮、能量治疗及配套解决方案。'
    ELSE `description`
  END,
  `description_en` = CASE
    WHEN `description_en` IS NULL OR TRIM(`description_en`) = ''
      THEN 'Presents exosome application and focused ultrasound products, covering non-invasive transdermal delivery, energy therapy, and supporting solutions.'
    ELSE `description_en`
  END
WHERE `deleted` = b'0'
  AND `hall_code` = 'hall_05';

UPDATE `showroom_hall`
SET
  `description` = CASE
    WHEN `description` IS NULL OR TRIM(`description`) = ''
      THEN '集中展示骨科与泌尿方向产品，覆盖关节介入、骨科手术及泌尿治疗相关器械。'
    ELSE `description`
  END,
  `description_en` = CASE
    WHEN `description_en` IS NULL OR TRIM(`description_en`) = ''
      THEN 'Presents orthopedics and urology products, covering joint intervention, orthopedic procedures, and urological treatment devices.'
    ELSE `description_en`
  END
WHERE `deleted` = b'0'
  AND `hall_code` = 'hall_06';

UPDATE `showroom_hall`
SET
  `description` = CASE
    WHEN `description` IS NULL OR TRIM(`description`) = ''
      THEN '集中展示非介入类医疗与健康产品，覆盖材料、消费医疗及配套健康管理方案。'
    ELSE `description`
  END,
  `description_en` = CASE
    WHEN `description_en` IS NULL OR TRIM(`description_en`) = ''
      THEN 'Presents non-interventional medical and health products, covering materials, consumer medical products, and supporting health-management solutions.'
    ELSE `description_en`
  END
WHERE `deleted` = b'0'
  AND `hall_code` = 'hall_07';

UPDATE `showroom_hall`
SET
  `description` = CASE
    WHEN `description` IS NULL OR TRIM(`description`) = ''
      THEN '集中展示医疗器械标准件与基础组件，覆盖导管、连接件、耗材组件及制造配套。'
    ELSE `description`
  END,
  `description_en` = CASE
    WHEN `description_en` IS NULL OR TRIM(`description_en`) = ''
      THEN 'Presents standard medical device components and foundational parts, covering catheters, connectors, consumable components, and manufacturing support.'
    ELSE `description_en`
  END
WHERE `deleted` = b'0'
  AND `hall_code` = 'hall_08';
