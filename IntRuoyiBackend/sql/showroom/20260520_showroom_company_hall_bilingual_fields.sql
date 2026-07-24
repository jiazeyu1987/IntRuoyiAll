ALTER TABLE `showroom_company`
    ADD COLUMN IF NOT EXISTS `display_name_en` varchar(255) DEFAULT NULL AFTER `display_name`;

ALTER TABLE `showroom_hall`
    ADD COLUMN IF NOT EXISTS `name_en` varchar(255) DEFAULT NULL AFTER `name`,
    ADD COLUMN IF NOT EXISTS `description_en` text DEFAULT NULL AFTER `description`;
