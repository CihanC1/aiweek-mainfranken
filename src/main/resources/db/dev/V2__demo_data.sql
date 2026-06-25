INSERT INTO event_sources(id,name,url,active,parser_key,created_at,updated_at) VALUES
('10000000-0000-0000-0000-000000000001','Mainfranken Demo Events','https://example.org/mainfranken-events',false,'generic',now(),now()),
('10000000-0000-0000-0000-000000000002','Bamberg Tech Demo','https://example.org/bamberg-tech',false,'generic',now(),now());
INSERT INTO app_users(id,display_name,whatsapp_opt_in,preferred_city,preferred_attendance_mode,created_at,updated_at) VALUES
('20000000-0000-0000-0000-000000000001','Demo Java Fan',false,'Würzburg','OFFLINE',now(),now()),
('20000000-0000-0000-0000-000000000002','Demo AI Fan',false,'Nürnberg','HYBRID',now(),now());
INSERT INTO user_interests(id,user_id,tag,created_at,updated_at) VALUES
('21000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001','java',now(),now()),
('21000000-0000-0000-0000-000000000002','20000000-0000-0000-0000-000000000001','cloud',now(),now()),
('21000000-0000-0000-0000-000000000003','20000000-0000-0000-0000-000000000002','ai',now(),now());
INSERT INTO events(id,title,organizer,description,event_type,start_at,end_at,location_name,city,address,attendance_mode,source_url,source_name,external_event_id,fingerprint,last_checked_at,status,created_at,updated_at) VALUES
('30000000-0000-0000-0000-000000000001','Java & Cloud Meetup','Mainfranken JUG','Modern Java and cloud-native systems','MEETUP',now()+interval '14 days',now()+interval '14 days 2 hours','Tech Hub','Würzburg',null,'OFFLINE','https://example.org/events/java-cloud','Demo','java-cloud-1','aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',now(),'ACTIVE',now(),now()),
('30000000-0000-0000-0000-000000000002','Applied AI Workshop','AI Network Franken','Hands-on AI engineering','WORKSHOP',now()+interval '20 days',null,'Innovation Lab','Nürnberg',null,'HYBRID','https://example.org/events/ai','Demo','ai-1','bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',now(),'ACTIVE',now(),now()),
('30000000-0000-0000-0000-000000000003','SQL & Database Night','Data Community','PostgreSQL performance and data modeling','MEETUP',now()+interval '25 days',null,'Digitalzentrum','Bamberg',null,'OFFLINE','https://example.org/events/sql','Demo','sql-1','cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc',now(),'ACTIVE',now(),now()),
('30000000-0000-0000-0000-000000000004','Cybersecurity Conference','Security Franken','Defensive security and startup networking','CONFERENCE',now()+interval '35 days',null,'Konferenzzentrum','Schweinfurt',null,'OFFLINE','https://example.org/events/security','Demo','sec-1','dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd',now(),'ACTIVE',now(),now());
INSERT INTO event_tags(event_id,tag) VALUES
('30000000-0000-0000-0000-000000000001','java'),('30000000-0000-0000-0000-000000000001','cloud'),
('30000000-0000-0000-0000-000000000002','ai'),('30000000-0000-0000-0000-000000000002','startup'),
('30000000-0000-0000-0000-000000000003','sql'),('30000000-0000-0000-0000-000000000003','database'),
('30000000-0000-0000-0000-000000000004','cybersecurity'),('30000000-0000-0000-0000-000000000004','networking');
INSERT INTO user_searches(id,user_id,query,normalized_query,created_at,updated_at) VALUES
('22000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001','Java cloud','java cloud',now(),now()),
('22000000-0000-0000-0000-000000000002','20000000-0000-0000-0000-000000000002','AI','ai',now(),now());
INSERT INTO event_groups(id,event_id,name,created_at,updated_at) VALUES
('40000000-0000-0000-0000-000000000001','30000000-0000-0000-0000-000000000001','Java & Cloud Community',now(),now()),
('40000000-0000-0000-0000-000000000002','30000000-0000-0000-0000-000000000002','Applied AI Community',now(),now());
