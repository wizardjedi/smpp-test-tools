CREATE TABLE `messages` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `abonent` varchar(20) NOT NULL,
  `sender` varchar(20) NOT NULL,
  `text` text NOT NULL,
  `add_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `send_date` datetime DEFAULT NULL,
  `delivery_date` datetime DEFAULT NULL,
  `try_count` tinyint(4) NOT NULL DEFAULT '0',
  `pending_count` int(11) NOT NULL DEFAULT '0',
  `accepted_count` int(11) NOT NULL DEFAULT '0',
  `delivered_count` int(11) NOT NULL DEFAULT '0',
  `rejected_count` int(11) NOT NULL DEFAULT '0',
  `error_codes` varchar(200) DEFAULT NULL,
  `validity_period` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `send_queue` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `send_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `message_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `send_date_idx` (`send_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `message_parts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `message_id` bigint(20) NOT NULL,
  `smsc_id` varchar(64) DEFAULT NULL,
  `link_id` varchar(200) DEFAULT NULL,
  `add_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `text` text NOT NULL,
  `send_date` datetime DEFAULT NULL,
  `delivery_date` datetime DEFAULT NULL,
  `status` enum('ENROUTE','DELIVERED','EXPIRED','DELETED','UNDELIVERABLE','ACCEPTED','UNKNOWN','REJECTED','QUEUED','NOT_SENT','SENDING') DEFAULT NULL,
  `send_error_code` int(11) DEFAULT '0',
  `delivery_error_code` int(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `FK_messages` (`message_id`),
  CONSTRAINT `FK_messages` FOREIGN KEY (`message_id`) REFERENCES `messages` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
