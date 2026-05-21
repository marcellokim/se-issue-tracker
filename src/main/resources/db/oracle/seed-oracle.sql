begin
   merge into users target
   using (
      select 'admin' as login_id,
             'Admin' as name,
             'ADMIN' as role,
             1 as active
        from dual
      union all
      select 'pl1',
             'PL 1',
             'PL',
             1
        from dual
      union all
      select 'pl2',
             'PL 2',
             'PL',
             1
        from dual
      union all
      select 'dev1',
             'Dev 1',
             'DEV',
             1
        from dual
      union all
      select 'dev2',
             'Dev 2',
             'DEV',
             1
        from dual
      union all
      select 'dev3',
             'Dev 3',
             'DEV',
             1
        from dual
      union all
      select 'dev4',
             'Dev 4',
             'DEV',
             1
        from dual
      union all
      select 'dev5',
             'Dev 5',
             'DEV',
             1
        from dual
      union all
      select 'dev6',
             'Dev 6',
             'DEV',
             1
        from dual
      union all
      select 'dev7',
             'Dev 7',
             'DEV',
             1
        from dual
      union all
      select 'dev8',
             'Dev 8',
             'DEV',
             1
        from dual
      union all
      select 'dev9',
             'Dev 9',
             'DEV',
             1
        from dual
      union all
      select 'dev10',
             'Dev 10',
             'DEV',
             1
        from dual
      union all
      select 'tester1',
             'Tester 1',
             'TESTER',
             1
        from dual
      union all
      select 'tester2',
             'Tester 2',
             'TESTER',
             1
        from dual
      union all
      select 'tester3',
             'Tester 3',
             'TESTER',
             1
        from dual
      union all
      select 'tester4',
             'Tester 4',
             'TESTER',
             1
        from dual
      union all
      select 'tester5',
             'Tester 5',
             'TESTER',
             1
        from dual
   ) source on ( target.login_id = source.login_id )
   when matched then update
   set target.name = source.name,
       target.role = source.role,
       target.active = source.active,
       target.updated_at = current_timestamp
   when not matched then
   insert (
      login_id,
      name,
      role,
      active )
   values
      ( source.login_id,
        source.name,
        source.role,
        source.active );
end;
/
begin
   merge into user_credentials target
   using (
      select 'admin' as login_id,
             '4eefdf0a692b0a9f55b0a25aa92ddd3c' as password_salt,
             'e0029239253cae8b9f8851e1e6a59a0c6b2d8692af7d7a3843da2ca4665da673' as password_hash
        from dual
      union all
      select 'pl1',
             '5158aa2245b0cd6d35eb50acb51f90f0',
             'bfc0e50ac607c99ea9e0c30f931c059832046f2190b975c51f715d5b4e08eb48'
        from dual
      union all
      select 'pl2',
             'b6cff8e992a228c0223c6e19c4513254',
             'e221f253a29d9201e759a693910f90f22fb66b2c48d922549c0c10c159efb44d'
        from dual
      union all
      select 'dev1',
             '780f2e9102a5710b4ab9d439b04fbbb2',
             '6a809c6135282018f27e9ab49454553995b198b5d37c8df591a59f36c1e9aa27'
        from dual
      union all
      select 'dev2',
             'e24fd4c7135f6697853d273c58b6012a',
             '6b15ca53ee39f47d37342723a49d2f858e8cce1c0edb6e671bbd79d90feea4ac'
        from dual
      union all
      select 'dev3',
             '51f481f3ca01e49494ac97c8433cbadc',
             'ebf5e8bc15550305b63ebd622ab586f987608c444ed5c5984b351d161f41c1dd'
        from dual
      union all
      select 'dev4',
             '91d41e978ac3797be9276d041d1198e8',
             '068c973da29e3b607fc630b0213f5b5a62db55f249567d88d955bc8bc052aee0'
        from dual
      union all
      select 'dev5',
             '991d64bb66d214708d91a3a956eb1940',
             '6424c8701be088e670a51c1ea227af4806484e2099e70765ba94c71310688421'
        from dual
      union all
      select 'dev6',
             'fbcea9ca91363148673e2967942ff76d',
             'b69dc0d1eaa1bb08e849144a49562e9ffa5b5e6564a8d96dec57bf3cd673b1d7'
        from dual
      union all
      select 'dev7',
             '02d38b80e8c06c5b02d9f4a8b512730b',
             '529f4ff49d6cb0fdb2825f38efda20573235e4f971bc6e15d47ced71a590e2b6'
        from dual
      union all
      select 'dev8',
             'a72916048583855e543c75fdd70f1317',
             '78ef14d74c09a5f61f8989902f3182c7f7970f32c3f69afe4802c662f9fd177c'
        from dual
      union all
      select 'dev9',
             'c43ecfac55a533aa4e5ca7233492d793',
             'a1ce90140e1ee9931a8ab64346e40b26748305fa559eee7f70ad2d647f43c9fd'
        from dual
      union all
      select 'dev10',
             '41ffe9aa08cf84ab48c1758bf5e64a63',
             'ff9489f3b4b0522a82b96229d69fbdd0f123f98d6d695868cce0dcebf2bec0d5'
        from dual
      union all
      select 'tester1',
             '7fe69942c0dd8b9a8c7d0109e691c13e',
             'c82a56579bdb85cc0eb948ddf2bbeba6b2c106ac59f8a6a178128134debfb6c4'
        from dual
      union all
      select 'tester2',
             '92b65790d122706e584563b840c92de0',
             'da02203ce16bb24ecbbdf77e752855108a3a0d554175c497f3b464071109bc79'
        from dual
      union all
      select 'tester3',
             'c33fa6ae64c6397d5ae55b8e8fd826bf',
             '4c8ad45e23c4e89400a0677ca35d29c22520016df774e57287cab55effacf929'
        from dual
      union all
      select 'tester4',
             'd92a5ba7508931ff9d919cdcd9b42ae6',
             '50ef0e5db933166ffbb158dcaa8fcb9116b60dfa5a7aeb7272d8f5f959ff44d5'
        from dual
      union all
      select 'tester5',
             '410692becb9135a848939b8419359965',
             '3fbd6f0569669a2b6a072648cd26a8a5d5f18082b7146392e2b82866c07b9e96'
        from dual
   ) source on ( target.login_id = source.login_id )
   when matched then update
   set target.password_salt = source.password_salt,
       target.password_hash = source.password_hash,
       target.updated_at = current_timestamp
   when not matched then
   insert (
      login_id,
      password_salt,
      password_hash )
   values
      ( source.login_id,
        source.password_salt,
        source.password_hash );
end;
/
begin
   merge into projects target
   using (
      select 'project1' as name,
             'Demo project for ITS persistence and query flows' as description,
             'admin' as managed_by_login_id
        from dual
      union all
      select 'project2',
             'Second demo project for PL assignment separation',
             'admin'
        from dual
   ) source on ( target.name = source.name )
   when matched then update
   set target.description = source.description,
       target.managed_by_login_id = source.managed_by_login_id,
       target.updated_at = current_timestamp
   when not matched then
   insert (
      name,
      description,
      managed_by_login_id )
   values
      ( source.name,
        source.description,
        source.managed_by_login_id );
end;
/
begin
   update issues target
      set target.project_id = (
      select id
        from projects
       where name = 'project2'
   ),
          target.updated_at = current_timestamp
    where target.title in ( 'Dashboard statistics misses closed issues',
                            'Reopened issue keeps old assignee' )
      and target.project_id = (
      select id
        from projects
       where name = 'project1'
   )
      and exists (
      select 1
        from projects
       where name = 'project2'
   );
end;
/
begin
   -- Demo seed owns only project1/project2 membership.
   -- User-created projects and their members are intentionally preserved.
   delete from project_members target
    where target.project_id in (
      select p.id
        from projects p
       where p.name in ( 'project1',
                         'project2' )
   );
end;
/
begin
   merge into project_members target
   using (
      select p.id as project_id,
             u.login_id as user_login_id
        from (
         select 'project1' as project_name,
                'pl1' as login_id
           from dual
         union all
         select 'project1',
                'dev1'
           from dual
         union all
         select 'project1',
                'dev2'
           from dual
         union all
         select 'project1',
                'dev3'
           from dual
         union all
         select 'project1',
                'dev4'
           from dual
         union all
         select 'project1',
                'dev5'
           from dual
         union all
         select 'project1',
                'dev6'
           from dual
         union all
         select 'project1',
                'dev7'
           from dual
         union all
         select 'project1',
                'dev8'
           from dual
         union all
         select 'project1',
                'dev9'
           from dual
         union all
         select 'project1',
                'dev10'
           from dual
         union all
         select 'project1',
                'tester1'
           from dual
         union all
         select 'project1',
                'tester2'
           from dual
         union all
         select 'project1',
                'tester3'
           from dual
         union all
         select 'project1',
                'tester4'
           from dual
         union all
         select 'project1',
                'tester5'
           from dual
         union all
         select 'project2',
                'pl2'
           from dual
         union all
         select 'project2',
                'dev4'
           from dual
         union all
         select 'project2',
                'dev5'
           from dual
         union all
         select 'project2',
                'tester4'
           from dual
         union all
         select 'project2',
                'tester5'
           from dual
      ) membership
        join projects p
      on p.name = membership.project_name
        join users u
      on u.login_id = membership.login_id
   ) source on ( target.project_id = source.project_id
      and target.user_login_id = source.user_login_id )
   when not matched then
   insert (
      project_id,
      user_login_id )
   values
      ( source.project_id,
        source.user_login_id );
end;
/
begin
   delete from issue_history target
    where exists (
      select 1
        from issues i
        join projects p
      on p.id = i.project_id
       where i.id = target.issue_id
         and p.name = 'project2'
         and i.title = 'Dashboard statistics misses closed issues'
         and target.action_type = 'STATUS_CHANGED'
         and target.previous_value = 'FIXED'
         and target.new_value = 'RESOLVED'
         and dbms_lob.substr(
         target.message,
         4000,
         1
      ) = 'Closed issue count verified'
   );
end;
/
begin
   delete from issue_dependencies target
    where exists (
      select 1
        from issues blocking_issue
        join projects blocking_project
      on blocking_project.id = blocking_issue.project_id
        join issues blocked_issue
      on blocked_issue.id = target.blocked_issue_id
        join projects blocked_project
      on blocked_project.id = blocked_issue.project_id
       where target.blocking_issue_id = blocking_issue.id
         and blocking_project.name = 'project1'
         and blocking_issue.title = 'Search result filter returns stale status'
         and blocked_project.name = 'project2'
         and blocked_issue.title = 'Dashboard statistics misses closed issues'
   );
end;
/
begin
   merge into issues target
   using (
      select p.id as project_id,
             lower(standard_hash(
                s.project_name
                || ':'
                || s.title,
                'SHA256'
             )) as issue_id,
             s.title,
             s.description,
             s.reported_date,
             s.priority,
             s.status,
             reporter.login_id as reporter_login_id,
             assignee.login_id as assignee_login_id,
             verifier.login_id as verifier_login_id,
             fixer.login_id as fixer_login_id,
             resolver.login_id as resolver_login_id
        from (
         select 'project1' as project_name,
                'Login fails on invalid credential' as title,
                'Login failure message should be stable for invalid credentials.' as description,
                current_timestamp - interval '15' day as reported_date,
                'MAJOR' as priority,
                'CLOSED' as status,
                'tester1' as reporter_login,
                null as assignee_login,
                null as verifier_login,
                'dev1' as fixer_login,
                'tester2' as resolver_login
           from dual
         union all
         select 'project1',
                'Search result filter returns stale status',
                'Search filters should reflect the latest issue status and priority.',
                current_timestamp - interval '10' day,
                'CRITICAL',
                'RESOLVED',
                'tester2',
                'dev2',
                'tester1',
                'dev2',
                'tester1'
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'Statistics should include closed issues in status trend queries.',
                current_timestamp - interval '20' day,
                'MAJOR',
                'CLOSED',
                'tester4',
                null,
                null,
                'dev4',
                'tester4'
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'Reopened issues should preserve assignment history and allow reassignment.',
                current_timestamp - interval '18' day,
                'CRITICAL',
                'RESOLVED',
                'tester5',
                'dev5',
                'tester5',
                'dev5',
                'tester5'
           from dual
         union all
         select 'project1',
                'Verification rejection returns to assignee',
                'Rejected verification should send the issue back to assigned developer with comment history.',
                current_timestamp - interval '6' day,
                'MAJOR',
                'RESOLVED',
                'tester5',
                'dev6',
                'tester5',
                'dev6',
                'tester5'
           from dual
         union all
         select 'project1',
                'Assignment notification not shown',
                'New issues should be visible to PL assignment workflow.',
                current_timestamp - interval '3' day,
                'MINOR',
                'NEW',
                'tester1',
                null,
                null,
                null,
                null
           from dual
         union all
         select 'project1',
                'Dependency resolution flow blocked',
                'Blocked issue should wait until its blocking issue is resolved or closed.',
                current_timestamp - interval '2' day,
                'BLOCKER',
                'ASSIGNED',
                'tester3',
                'dev3',
                'tester3',
                null,
                null
           from dual
      ) s
        join projects p
      on p.name = s.project_name
        join users reporter
      on reporter.login_id = s.reporter_login
        left join users assignee
      on assignee.login_id = s.assignee_login
        left join users verifier
      on verifier.login_id = s.verifier_login
        left join users fixer
      on fixer.login_id = s.fixer_login
        left join users resolver
      on resolver.login_id = s.resolver_login
   ) source on ( target.project_id = source.project_id
      and target.title = source.title )
   when matched then update
   set target.issue_id = source.issue_id,
       target.description = source.description,
       target.reported_date = source.reported_date,
       target.priority = source.priority,
       target.status = source.status,
       target.reporter_login_id = source.reporter_login_id,
       target.assignee_login_id = source.assignee_login_id,
       target.verifier_login_id = source.verifier_login_id,
       target.fixer_login_id = source.fixer_login_id,
       target.resolver_login_id = source.resolver_login_id,
       target.updated_at = current_timestamp
   when not matched then
   insert (
      project_id,
      issue_id,
      title,
      description,
      reported_date,
      priority,
      status,
      reporter_login_id,
      assignee_login_id,
      verifier_login_id,
      fixer_login_id,
      resolver_login_id )
   values
      ( source.project_id,
        source.issue_id,
        source.title,
        source.description,
        source.reported_date,
        source.priority,
        source.status,
        source.reporter_login_id,
        source.assignee_login_id,
        source.verifier_login_id,
        source.fixer_login_id,
        source.resolver_login_id );
end;
/
begin
   merge into comments target
   using (
      select i.id as issue_id,
             u.login_id as writer_login_id,
             s.content,
             case
                when s.content in ( 'Fix implemented',
                                    'Fix verified',
                                    'Closed by PL',
                                    'Filter corrected',
                                    'Verification complete',
                                    'Statistics query updated',
                                    'Closed issue count verified after dependency guard passed',
                                    'Closed after dashboard verification',
                                    'Reopened after release dashboard regression',
                                    'Follow-up statistics fix implemented',
                                    'Follow-up dashboard verification complete',
                                    'Reclosed after release regression verification',
                                    'Initial reassignment fix implemented',
                                    'Initial verification complete',
                                    'Regression reproduced after reopen',
                                    'Regression fix implemented',
                                    'Regression verification complete',
                                    'Initial fix ready for verification',
                                    'Verification failed due to stale cache',
                                    'Rework completed after rejection',
                                    'Reverification complete' ) then
                   'STATUS_CHANGE_REASON'
                else
                   'GENERAL'
             end as purpose,
             s.created_date
        from (
         select 'project1' as project_name,
                'Login fails on invalid credential' as issue_title,
                'tester1' as writer_login,
                'Initial login bug report.' as content,
                current_timestamp - interval '15' day as created_date
           from dual
         union all
         select 'project1',
                'Login fails on invalid credential',
                'pl1',
                'Assigned to dev1 and tester2',
                current_timestamp - interval '14' day
           from dual
         union all
         select 'project1',
                'Login fails on invalid credential',
                'dev1',
                'Fix implemented',
                current_timestamp - interval '13' day
           from dual
         union all
         select 'project1',
                'Login fails on invalid credential',
                'tester2',
                'Fix verified',
                current_timestamp - interval '12' day
           from dual
         union all
         select 'project1',
                'Login fails on invalid credential',
                'pl1',
                'Closed by PL',
                current_timestamp - interval '11' day
           from dual
         union all
         select 'project1',
                'Search result filter returns stale status',
                'tester2',
                'Search status filter mismatch found during verification.',
                current_timestamp - interval '10' day
           from dual
         union all
         select 'project1',
                'Search result filter returns stale status',
                'pl1',
                'Assigned to dev2 and tester1',
                current_timestamp - interval '9' day
           from dual
         union all
         select 'project1',
                'Search result filter returns stale status',
                'dev2',
                'Filter corrected',
                current_timestamp - interval '8' day
           from dual
         union all
         select 'project1',
                'Search result filter returns stale status',
                'tester1',
                'Verification complete',
                current_timestamp - interval '7' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'tester4',
                'Closed issue is missing from the dashboard status count.',
                current_timestamp - interval '20' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'pl2',
                'Assigned to dev4 and tester4',
                current_timestamp - interval '19' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'dev4',
                'Status aggregation query updated and ready for verification.',
                current_timestamp - interval '18' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'dev4',
                'Statistics query updated',
                current_timestamp - interval '18' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'tester4',
                'Closed issue count verified after dependency guard passed',
                current_timestamp - interval '10' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'pl2',
                'Closed after dashboard verification',
                current_timestamp - interval '9' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'pl2',
                'Reopened after release dashboard regression',
                current_timestamp - interval '8' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'pl2',
                'Reassigned to dev4 after closed issue reopened',
                current_timestamp - interval '7' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'dev4',
                'Follow-up statistics fix implemented',
                current_timestamp - interval '6' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'tester4',
                'Follow-up dashboard verification complete',
                current_timestamp - interval '5' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'pl2',
                'Reclosed after release regression verification',
                current_timestamp - interval '4' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'tester5',
                'Regression found after reopening an already resolved issue.',
                current_timestamp - interval '18' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'pl2',
                'Assigned to dev5 and tester5',
                current_timestamp - interval '17' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'dev5',
                'Initial reassignment fix implemented',
                current_timestamp - interval '16' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'tester5',
                'Initial verification complete',
                current_timestamp - interval '15' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'pl2',
                'Reassignment history should remain traceable after reopen.',
                current_timestamp - interval '14' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'pl2',
                'Regression reproduced after reopen',
                current_timestamp - interval '14' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'pl2',
                'Reassigned to dev5 for regression fix',
                current_timestamp - interval '13' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'dev5',
                'Regression fix implemented',
                current_timestamp - interval '12' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'tester5',
                'Regression verification complete',
                current_timestamp - interval '11' day
           from dual
         union all
         select 'project1',
                'Verification rejection returns to assignee',
                'pl1',
                'Assigned to dev6 and tester5',
                current_timestamp - interval '5' day
           from dual
         union all
         select 'project1',
                'Verification rejection returns to assignee',
                'dev6',
                'Initial fix ready for verification',
                current_timestamp - interval '4' day
           from dual
         union all
         select 'project1',
                'Verification rejection returns to assignee',
                'tester5',
                'Verification failed due to stale cache',
                current_timestamp - interval '3' day
           from dual
         union all
         select 'project1',
                'Verification rejection returns to assignee',
                'dev6',
                'Rework completed after rejection',
                current_timestamp - interval '2' day
           from dual
         union all
         select 'project1',
                'Verification rejection returns to assignee',
                'tester5',
                'Reverification complete',
                current_timestamp - interval '1' day
           from dual
         union all
         select 'project1',
                'Dependency resolution flow blocked',
                'pl1',
                'Resolve only after the blocking issue is closed.',
                current_timestamp - interval '2' day
           from dual
         union all
         select 'project1',
                'Dependency resolution flow blocked',
                'pl1',
                'Assigned to dev3 and tester3',
                current_timestamp - interval '1' day
           from dual
      ) s
        join projects p
      on p.name = s.project_name
        join issues i
      on i.project_id = p.id
         and i.title = s.issue_title
        join users u
      on u.login_id = s.writer_login
   ) source on ( target.issue_id = source.issue_id
      and target.writer_login_id = source.writer_login_id
      and dbms_lob.substr(
      target.content,
      4000,
      1
   ) = source.content )
   when matched then update
   set target.purpose = source.purpose,
       target.created_date = source.created_date
   when not matched then
   insert (
      issue_id,
      writer_login_id,
      content,
      purpose,
      created_date )
   values
      ( source.issue_id,
        source.writer_login_id,
        source.content,
        source.purpose,
        source.created_date );
end;
/
begin
   merge into issue_history target
   using (
      select i.id as issue_id,
             u.login_id as changed_by_login_id,
             s.action_type,
             s.previous_value,
             s.new_value,
             s.message,
             s.changed_date
        from (
         select 'project1' as project_name,
                'Login fails on invalid credential' as issue_title,
                'tester1' as changed_by_login,
                'CREATED' as action_type,
                null as previous_value,
                'NEW' as new_value,
                'Issue created' as message,
                current_timestamp - interval '15' day as changed_date
           from dual
         union all
         select 'project1',
                'Login fails on invalid credential',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Assigned to dev1 and tester2',
                current_timestamp - interval '14' day
           from dual
         union all
         select 'project1',
                'Login fails on invalid credential',
                'dev1',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Fix implemented',
                current_timestamp - interval '13' day
           from dual
         union all
         select 'project1',
                'Login fails on invalid credential',
                'tester2',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Fix verified',
                current_timestamp - interval '12' day
           from dual
         union all
         select 'project1',
                'Login fails on invalid credential',
                'pl1',
                'STATUS_CHANGED',
                'RESOLVED',
                'CLOSED',
                'Closed by PL',
                current_timestamp - interval '11' day
           from dual
         union all
         select 'project1',
                'Search result filter returns stale status',
                'tester2',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                current_timestamp - interval '10' day
           from dual
         union all
         select 'project1',
                'Search result filter returns stale status',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Assigned to dev2 and tester1',
                current_timestamp - interval '9' day
           from dual
         union all
         select 'project1',
                'Search result filter returns stale status',
                'dev2',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Filter corrected',
                current_timestamp - interval '8' day
           from dual
         union all
         select 'project1',
                'Search result filter returns stale status',
                'tester1',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Verification complete',
                current_timestamp - interval '7' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'tester4',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                current_timestamp - interval '20' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Assigned to dev4 and tester4',
                current_timestamp - interval '19' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'dev4',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Statistics query updated',
                current_timestamp - interval '18' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'dev4',
                'COMMENTED',
                null,
                null,
                'Implementation note added to comment thread',
                current_timestamp - interval '18' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'tester4',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Closed issue count verified after dependency guard passed',
                current_timestamp - interval '10' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'pl2',
                'STATUS_CHANGED',
                'RESOLVED',
                'CLOSED',
                'Closed after dashboard verification',
                current_timestamp - interval '9' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'pl2',
                'STATUS_CHANGED',
                'CLOSED',
                'REOPENED',
                'Reopened after release dashboard regression',
                current_timestamp - interval '8' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'pl2',
                'STATUS_CHANGED',
                'REOPENED',
                'ASSIGNED',
                'Reassigned to dev4 after closed issue reopened',
                current_timestamp - interval '7' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'dev4',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Follow-up statistics fix implemented',
                current_timestamp - interval '6' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'tester4',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Follow-up dashboard verification complete',
                current_timestamp - interval '5' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'pl2',
                'STATUS_CHANGED',
                'RESOLVED',
                'CLOSED',
                'Reclosed after release regression verification',
                current_timestamp - interval '4' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'tester5',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                current_timestamp - interval '18' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Assigned to dev5 and tester5',
                current_timestamp - interval '17' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'dev5',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Initial reassignment fix implemented',
                current_timestamp - interval '16' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'tester5',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Initial verification complete',
                current_timestamp - interval '15' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'pl2',
                'STATUS_CHANGED',
                'RESOLVED',
                'REOPENED',
                'Regression reproduced after reopen',
                current_timestamp - interval '14' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'pl2',
                'STATUS_CHANGED',
                'REOPENED',
                'ASSIGNED',
                'Reassigned to dev5 for regression fix',
                current_timestamp - interval '13' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'dev5',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Regression fix implemented',
                current_timestamp - interval '12' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'tester5',
                'COMMENTED',
                null,
                null,
                'Regression path documented in comments',
                current_timestamp - interval '12' day
           from dual
         union all
         select 'project2',
                'Reopened issue keeps old assignee',
                'tester5',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Regression verification complete',
                current_timestamp - interval '11' day
           from dual
         union all
         select 'project1',
                'Verification rejection returns to assignee',
                'tester5',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                current_timestamp - interval '6' day
           from dual
         union all
         select 'project1',
                'Verification rejection returns to assignee',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Assigned to dev6 and tester5',
                current_timestamp - interval '5' day
           from dual
         union all
         select 'project1',
                'Verification rejection returns to assignee',
                'dev6',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Initial fix ready for verification',
                current_timestamp - interval '4' day
           from dual
         union all
         select 'project1',
                'Verification rejection returns to assignee',
                'tester5',
                'STATUS_CHANGED',
                'FIXED',
                'ASSIGNED',
                'Verification failed due to stale cache',
                current_timestamp - interval '3' day
           from dual
         union all
         select 'project1',
                'Verification rejection returns to assignee',
                'dev6',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Rework completed after rejection',
                current_timestamp - interval '2' day
           from dual
         union all
         select 'project1',
                'Verification rejection returns to assignee',
                'tester5',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Reverification complete',
                current_timestamp - interval '1' day
           from dual
         union all
         select 'project1',
                'Assignment notification not shown',
                'tester1',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                current_timestamp - interval '3' day
           from dual
         union all
         select 'project1',
                'Dependency resolution flow blocked',
                'tester3',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                current_timestamp - interval '2' day
           from dual
         union all
         select 'project1',
                'Dependency resolution flow blocked',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Assigned to dev3 and tester3',
                current_timestamp - interval '1' day
           from dual
      ) s
        join projects p
      on p.name = s.project_name
        join issues i
      on i.project_id = p.id
         and i.title = s.issue_title
        join users u
      on u.login_id = s.changed_by_login
   ) source on ( target.issue_id = source.issue_id
      and target.action_type = source.action_type
      and nvl(
      target.previous_value,
      '#'
   ) = nvl(
      source.previous_value,
      '#'
   )
      and nvl(
      target.new_value,
      '#'
   ) = nvl(
      source.new_value,
      '#'
   )
      and nvl(
      dbms_lob.substr(
         target.message,
         4000,
         1
      ),
      '#'
   ) = nvl(
      source.message,
      '#'
   ) )
   when matched then update
   set target.changed_by_login_id = source.changed_by_login_id,
       target.changed_date = source.changed_date
   when not matched then
   insert (
      issue_id,
      changed_by_login_id,
      action_type,
      previous_value,
      new_value,
      message,
      changed_date )
   values
      ( source.issue_id,
        source.changed_by_login_id,
        source.action_type,
        source.previous_value,
        source.new_value,
        source.message,
        source.changed_date );
end;
/
begin
   merge into issue_dependencies target
   using (
      select blocking_issue.id as blocking_issue_id,
             blocked_issue.id as blocked_issue_id,
             lower(standard_hash(
                to_char(blocking_issue.id)
                || ':'
                || to_char(blocked_issue.id),
                'SHA256'
             )) as dependency_id,
             s.discovered_date
        from (
         select 'project1' as blocking_project_name,
                'Login fails on invalid credential' as blocking_title,
                'project1' as blocked_project_name,
                'Dependency resolution flow blocked' as blocked_title,
                current_timestamp - interval '1' day as discovered_date
           from dual
         union all
         select 'project1',
                'Login fails on invalid credential',
                'project2',
                'Dashboard statistics misses closed issues',
                current_timestamp - interval '11' day
           from dual
         union all
         select 'project2',
                'Dashboard statistics misses closed issues',
                'project2',
                'Reopened issue keeps old assignee',
                current_timestamp - interval '13' day
           from dual
      ) s
        join projects blocking_project
      on blocking_project.name = s.blocking_project_name
        join issues blocking_issue
      on blocking_issue.project_id = blocking_project.id
         and blocking_issue.title = s.blocking_title
        join projects blocked_project
      on blocked_project.name = s.blocked_project_name
        join issues blocked_issue
      on blocked_issue.project_id = blocked_project.id
         and blocked_issue.title = s.blocked_title
   ) source on ( target.blocking_issue_id = source.blocking_issue_id
      and target.blocked_issue_id = source.blocked_issue_id )
   when matched then update
   set target.dependency_id = source.dependency_id,
       target.discovered_date = source.discovered_date
   when not matched then
   insert (
      dependency_id,
      blocking_issue_id,
      blocked_issue_id,
      discovered_date )
   values
      ( source.dependency_id,
        source.blocking_issue_id,
        source.blocked_issue_id,
        source.discovered_date );
end;
/