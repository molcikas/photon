DROP TABLE IF EXISTS `shape`;
DROP TABLE IF EXISTS `circle`;
DROP TABLE IF EXISTS `rectangle`;

CREATE TABLE `shape` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`type` varchar(255) NOT NULL,
`color` varchar(255) NOT NULL,
PRIMARY KEY (`id`)
);

CREATE TABLE `circle` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`radius` int(11) NULL,
PRIMARY KEY (`id`),
CONSTRAINT `circle_shape` FOREIGN KEY (`id`) REFERENCES `shape` (`id`)
);

CREATE TABLE `rectangle` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`width` int(11) NULL,
`height` int(11) NULL,
PRIMARY KEY (`id`),
CONSTRAINT `rectangle_shape` FOREIGN KEY (`id`) REFERENCES `shape` (`id`)
);

insert into `shape` (`id`, `type`, `color`) values (1, 'circle', 'red');
insert into `circle` (`id`, `radius`) values (1, 3);

insert into `shape` (`id`, `type`, `color`) values (2, 'rectangle', 'blue');
insert into `rectangle` (`id`, `width`, `height`) values (2, 7, 8);