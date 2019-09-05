/*
	Create device;
	-ip;
	-mac;
	-name;
	-status;
	-quota;
	-bandwidth;
*/
INSERT INTO device VALUES ('192.168.137.1', 'b8:27:eb:68:17:b4', 'spyke', 'NEW', 100, 10);
/*
	Update device by ip;
	-status;
	-quota;
	-bandwidth;
*/
UPDATE device status = 'BLOCKED', quota = 200, bandwidth = 20 where ip = '192.168.137.1';
/*
	DROP TABLES
*/
DROP TABLE IF EXISTS UPLOAD;
DROP TABLE IF EXISTS DOWNLOAD;
DROP TABLE IF EXISTS DEVICE;