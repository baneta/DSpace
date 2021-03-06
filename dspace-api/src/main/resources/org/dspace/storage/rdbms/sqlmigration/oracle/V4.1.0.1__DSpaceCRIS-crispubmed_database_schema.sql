--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

create table cris_pmc_citation (pubmedID number(10,0) not null, numCitations number(10,0) not null, timestampCreated timestamp, timestampLastModified timestamp, primary key (pubmedID));
create table cris_pmc_citation2record (cris_pmc_citation_pubmedID number(10,0) not null, pmcRecords_pmcID number(10,0) not null);
create table cris_pmc_citation_itemIDs (cris_pmc_citation_pubmedID number(10,0) not null, element number(10,0));
create table cris_pmc_record (pmcID number(10,0) not null, authors VARCHAR2(2000), publicationNote clob, title VARCHAR2(2000), primary key (pmcID));
create table cris_pmc_record_handles (cris_pmc_record_pmcID number(10,0) not null, element varchar2(255 char));
create table cris_pmc_record_pubmedIDs (cris_pmc_record_pmcID number(10,0) not null, element number(10,0));
alter table cris_pmc_citation2record add constraint FKF1B54E9DE4C0A379 foreign key (pmcRecords_pmcID) references cris_pmc_record;
alter table cris_pmc_citation2record add constraint FKF1B54E9D6A8E17F4 foreign key (cris_pmc_citation_pubmedID) references cris_pmc_citation;
alter table cris_pmc_citation_itemIDs add constraint FKC524AD2C6A8E17F4 foreign key (cris_pmc_citation_pubmedID) references cris_pmc_citation;
alter table cris_pmc_record_handles add constraint FKAFFDDCDCC016C9CD foreign key (cris_pmc_record_pmcID) references cris_pmc_record;
alter table cris_pmc_record_pubmedIDs add constraint FKFBE237FAC016C9CD foreign key (cris_pmc_record_pmcID) references cris_pmc_record;