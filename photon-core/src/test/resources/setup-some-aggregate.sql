DROP TABLE IF EXISTS `someaggregate`;
DROP TABLE IF EXISTS `someclass`;

CREATE TABLE `someaggregate` (
`id` int(11) NOT NULL AUTO_INCREMENT,
PRIMARY KEY (`id`)
);

CREATE TABLE `someclass` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`someAggregateId` int(11) NOT NULL,
PRIMARY KEY (`id`),
CONSTRAINT `someclass_someaggregate` FOREIGN KEY (`someAggregateId`) REFERENCES `someaggregate` (`id`)
);

insert into `someaggregate` (`id`) values (1);
insert into `someclass` (`id`, `someAggregateId`) values (1, 1);