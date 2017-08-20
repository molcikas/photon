DROP TABLE IF EXISTS `drawing`;
DROP TABLE IF EXISTS `shape`;
DROP TABLE IF EXISTS `shapecolorhistory`;
DROP TABLE IF EXISTS `circle`;
DROP TABLE IF EXISTS `rectangle`;
DROP TABLE IF EXISTS `cornercoordinates`;

CREATE TABLE `drawing` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`name` varchar(255) NULL,
PRIMARY KEY (`id`)
);

CREATE TABLE `shape` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`type` varchar(255) NOT NULL,
`color` varchar(255) NOT NULL,
`drawingId` int(11) DEFAULT NULL,
CONSTRAINT `shape_drawing` FOREIGN KEY (`drawingId`) REFERENCES `drawing` (`id`),
PRIMARY KEY (`id`)
);

CREATE TABLE `shapecolorhistory` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`shapeId` int(11) NOT NULL,
`dateChanged` datetime NOT NULL,
`colorName` varchar(255) NOT NULL,
PRIMARY KEY (`id`),
CONSTRAINT `shapecolorhistory_shape` FOREIGN KEY (`shapeId`) REFERENCES `shape` (`id`)
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

CREATE TABLE `cornercoordinates` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`shapeId` int(11) NOT NULL,
`x` int(11) NULL,
`y` int(11) NULL,
PRIMARY KEY (`id`),
CONSTRAINT `cornerCoordinates_shape` FOREIGN KEY (`shapeId`) REFERENCES `shape` (`id`)
);

insert into `drawing` (`id`) values (1);

insert into `shape` (`id`, `type`, `color`, `drawingId`) values (1, 'circle', 'red', 1);
insert into `circle` (`id`, `radius`) values (1, 3);

insert into `shape` (`id`, `type`, `color`, `drawingId`) values (2, 'rectangle', 'blue', 1);
insert into `rectangle` (`id`, `width`, `height`) values (2, 7, 8);

insert into `shape` (`id`, `type`, `color`, `drawingId`) values (3, 'circle', 'orange', 1);
insert into `circle` (`id`, `radius`) values (3, 4);
insert into `shapecolorhistory` (`id`, `shapeId`, `dateChanged`, `colorName`) values (1, 3, PARSEDATETIME('2017-03-19 09-28-17', 'yyyy-MM-dd HH-mm-ss'), 'creamsicle');
insert into `shapecolorhistory` (`id`, `shapeId`, `dateChanged`, `colorName`) values (2, 3, PARSEDATETIME('2017-04-20 10-29-18', 'yyyy-MM-dd HH-mm-ss'), 'yellow');

insert into `shape` (`id`, `type`, `color`, `drawingId`) values (4, 'rectangle', 'white', 1);
insert into `rectangle` (`id`, `width`, `height`) values (4, 11, 9);
insert into `shapecolorhistory` (`id`, `shapeId`, `dateChanged`, `colorName`) values (3, 4, PARSEDATETIME('2017-04-11 11-28-17', 'yyyy-MM-dd HH-mm-ss'), 'beige');
insert into `shapecolorhistory` (`id`, `shapeId`, `dateChanged`, `colorName`) values (4, 4, PARSEDATETIME('2017-04-22 12-29-18', 'yyyy-MM-dd HH-mm-ss'), 'gray');
insert into `cornercoordinates` (`id`, `shapeId`, `x`, `y`) values (1, 4, 0, 0);
insert into `cornercoordinates` (`id`, `shapeId`, `x`, `y`) values (2, 4, 7, 0);
insert into `cornercoordinates` (`id`, `shapeId`, `x`, `y`) values (3, 4, 0, 8);
insert into `cornercoordinates` (`id`, `shapeId`, `x`, `y`) values (4, 4, 7, 8);