CREATE TABLE IF NOT EXISTS CWF_DOMAIN (
	ID VARCHAR(20) PRIMARY KEY,
	NAME VARCHAR(255),
	ATTRIBUTES CLOB DEFAULT '');

MERGE INTO CWF_DOMAIN (ID, NAME, ATTRIBUTES) KEY(ID) VALUES
	('1', 'General Medicine Clinic', ''),
	('2', 'Emergency Room', ''),
	('3', 'Test Hospital', 'default=true'),
	('*', 'All Domains', '');

CREATE TABLE IF NOT EXISTS CWF_USER (
	ID VARCHAR(20) PRIMARY KEY,
	USERNAME VARCHAR(40),
	PASSWORD VARCHAR(40),
	NAME VARCHAR(255),
	DOMAIN VARCHAR(20),
	AUTHORITIES CLOB DEFAULT '');

MERGE INTO CWF_USER (ID, USERNAME, PASSWORD, NAME, DOMAIN, AUTHORITIES) KEY(ID) VALUES
	('1', 'doctor', 'doctor', 'Doctor, Test', '1', 'PRIV_PATIENT_SELECT'),
	('2', 'demo', 'demo', 'User, Test', '*', 'PRIV_DEBUG,PRIV_CAREWEB_DESIGNER,PRIV_PATIENT_SELECT');
	
