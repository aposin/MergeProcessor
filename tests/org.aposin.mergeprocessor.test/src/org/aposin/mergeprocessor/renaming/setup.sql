--
-- Copyright 2019 Association for the promotion of open-source insurance software and for the establishment of open interface standards in the insurance industry (Verein zur Förderung quelloffener Versicherungssoftware und Etablierung offener Schnittstellenstandards in der Versicherungsbranche)
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE TABLE RENAME_MAPPING (ID INT AUTO_INCREMENT PRIMARY KEY, OLD_NAME VARCHAR(255), NEW_NAME VARCHAR(255), VERSION VARCHAR(63), REPOSITORY VARCHAR(127));
CREATE TABLE LINK_MAPPING (ID INT AUTO_INCREMENT PRIMARY KEY, NAME1 VARCHAR(255), NAME2 VARCHAR(255), VERSION VARCHAR(63), REPOSITORY VARCHAR(127));

INSERT INTO RENAME_MAPPING VALUES(default, 'platform/java/plugins/org.aposin.framework', 'platform/java/plugins/org.opin.framework', '18.5.105', 'https://svn-testrepository.at');
INSERT INTO RENAME_MAPPING VALUES(default, 'platform/java/plugins/org.aposin.framework/src', 'platform/java/plugins/org.opin.framework/src', '18.5.105', 'https://svn-testrepository.at');
INSERT INTO RENAME_MAPPING VALUES(default, 'platform/java/plugins/org.aposin.framework/src/org', 'platform/java/plugins/org.opin.framework/src/org', '18.5.105', 'https://svn-testrepository.at');
INSERT INTO RENAME_MAPPING VALUES(default, 'platform/java/plugins/org.aposin.framework/src/org/aposin', 'platform/java/plugins/org.opin.framework/src/org/opin', '18.5.105', 'https://svn-testrepository.at');
INSERT INTO RENAME_MAPPING VALUES(default, 'platform/java/plugins/org.aposin.framework/src/org/aposin/framework', 'platform/java/plugins/org.opin.framework/src/org/opin/framework', '18.5.105', 'https://svn-testrepository.at');
INSERT INTO RENAME_MAPPING VALUES(default, 'platform/java/plugins/org.aposin.framework/src/org/aposin/framework/logic', 'platform/java/plugins/org.opin.framework/src/org/opin/framework/logic', '18.5.105', 'https://svn-testrepository.at');
INSERT INTO RENAME_MAPPING VALUES(default, 'platform/java/plugins/org.aposin.framework/src/org/aposin/framework/logic/OpinSession.java', 'platform/java/plugins/org.opin.framework/src/org/opin/framework/logic/OpinSession.java', '18.5.105', 'https://svn-testrepository.at');
INSERT INTO RENAME_MAPPING VALUES(default, 'platform/java/plugins/com.aposin.abc.core.logik.journal/src/com/aposin/abc/core/logik/journal/logic/produktionssteuerung/schadenvertrag', 'platform/java/plugins/com.aposin.sba.core.productioncontrol/src/com/aposin/sba/core/productioncontrol/schadenvertrag', '18.5.104', 'https://svn-testrepository.at');
INSERT INTO RENAME_MAPPING VALUES(default, 'platform/java/plugins/com.aposin.sba.core.productioncontrol/src/com/aposin/sba/core/productioncontrol/schadenvertrag/altersstruktur/BoClaimContract.java', 'platform/java/plugins/org.opin.productioncontrol/src/org/opin/productioncontrol/claimcontract/agestructure/BoClaimContract.java', '18.5.105', 'https://svn-testrepository.at');

INSERT INTO RENAME_MAPPING VALUES(default, 'platform/java/plugins/org.aposin.framework/src/org/aposin/framework/messages/messages.properties', 'platform/java/plugins/org.opin.framework/src/org/opin/framework/messages/messages.properties', '18.5.105', 'https://svn-testrepository.at');
INSERT INTO RENAME_MAPPING VALUES(default, 'platform/java/plugins/org.aposin.framework/src/org/aposin/framework/messages/messages_en.properties', 'platform/java/plugins/org.opin.framework/src/org/opin/framework/messages/messages_en.properties', '18.5.105', 'https://svn-testrepository.at');
INSERT INTO RENAME_MAPPING VALUES(default, 'platform/java/plugins/org.opin.framework/src/org/opin/framework/messages/messages.properties', 'platform/java/plugins/org.opin.framework/src/org/opin/framework/messages/messages_de.properties', '18.5.200', 'https://svn-testrepository.at');
INSERT INTO RENAME_MAPPING VALUES(default, 'platform/java/plugins/org.opin.framework/src/org/opin/framework/messages/messages_en.properties', 'platform/java/plugins/org.opin.framework/src/org/opin/framework/messages/messages.properties', '18.5.200', 'https://svn-testrepository.at');

INSERT INTO LINK_MAPPING VALUES(default, 'www/direct/java/plugins/org.opin.framework.direct', 'www/adapter_v2/java/plugins/org.opin.framework.adapter.v2', '18.0.102', 'https://svn-testrepository.at');
INSERT INTO LINK_MAPPING VALUES(default, 'www/direct/java/plugins/org.opin.framework.direct/src/org/opin/framework/direct/framework/assignment/ext/BoAssignmentValidationServiceExt.java', 'www/adapter_v2/java/plugins/org.opin.framework.adapter.v2/src/org/opin/framework/adapter/v2/framework/assignment/ext/BoAssignmentValidationServiceExt.java', '18.0.102', 'https://svn-testrepository.at');