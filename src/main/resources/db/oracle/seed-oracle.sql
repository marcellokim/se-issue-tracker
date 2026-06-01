begin
   merge into users target
   using (
      select seed.login_id,
             seed.name,
             seed.role,
             seed.active,
             timestamp '2026-03-01 00:00:00' as created_at,
             timestamp '2026-03-01 00:00:00' as updated_at
        from (
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
      ) seed
   ) source on ( target.login_id = source.login_id )
   when matched then update
   set target.name = source.name,
       target.role = source.role,
       target.active = source.active,
       target.created_at = source.created_at,
       target.updated_at = source.updated_at
   when not matched then
   insert (
      login_id,
      name,
      role,
      active,
      created_at,
      updated_at )
   values
      ( source.login_id,
        source.name,
        source.role,
        source.active,
        source.created_at,
        source.updated_at );
end;
/
begin
   merge into user_credentials target
   using (
      select seed.login_id,
             seed.password_salt,
             seed.password_hash,
             timestamp '2026-03-01 00:00:00' as updated_at
        from (
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
      ) seed
   ) source on ( target.login_id = source.login_id )
   when matched then update
   set target.password_salt = source.password_salt,
       target.password_hash = source.password_hash,
       target.updated_at = source.updated_at
   when not matched then
   insert (
      login_id,
      password_salt,
      password_hash,
      updated_at )
   values
      ( source.login_id,
        source.password_salt,
        source.password_hash,
        source.updated_at );
end;
/
begin
   update projects
      set name = 'Project A',
          description = 'Primary demo project for ITS workflow, assignment, statistics, and deletion scenarios'
    where name = 'project1';

   update projects
      set name = 'Project B',
          description = 'Secondary demo project for separated PL, recommendation, and dependency scenarios'
    where name = 'project2';
end;
/
begin
   merge into projects target
   using (
      select seed.name,
             seed.description,
             seed.managed_by_login_id,
             timestamp '2026-03-01 00:00:00' as created_at,
             timestamp '2026-03-01 00:00:00' as updated_at
        from (
         select 'Project A' as name,
                'Primary demo project for ITS workflow, assignment, statistics, and deletion scenarios' as description,
                'admin' as managed_by_login_id
           from dual
         union all
         select 'Project B',
                'Secondary demo project for separated PL, recommendation, and dependency scenarios',
                'admin'
           from dual
      ) seed
   ) source on ( target.name = source.name )
   when matched then update
   set target.description = source.description,
       target.managed_by_login_id = source.managed_by_login_id,
       target.created_at = source.created_at,
       target.updated_at = source.updated_at
   when not matched then
   insert (
      name,
      description,
      managed_by_login_id,
      created_at,
      updated_at )
   values
      ( source.name,
        source.description,
        source.managed_by_login_id,
        source.created_at,
        source.updated_at );
end;
/
begin
   -- Demo seed owns only Project A/Project B membership.
   -- User-created projects and their members are intentionally preserved.
   delete from project_members target
    where target.project_id in (
      select p.id
        from projects p
       where p.name in ( 'Project A',
                         'Project B' )
   );
end;
/
begin
   merge into project_members target
   using (
      select p.id as project_id,
             u.login_id as user_login_id,
             timestamp '2026-03-01 00:00:00' as joined_at
        from (
         select 'Project A' as project_name,
                'pl1' as login_id
           from dual
         union all
         select 'Project A',
                'dev1'
           from dual
         union all
         select 'Project A',
                'dev2'
           from dual
         union all
         select 'Project A',
                'dev3'
           from dual
         union all
         select 'Project A',
                'dev4'
           from dual
         union all
         select 'Project A',
                'dev5'
           from dual
         union all
         select 'Project A',
                'tester1'
           from dual
         union all
         select 'Project A',
                'tester2'
           from dual
         union all
         select 'Project A',
                'tester3'
           from dual
         union all
         select 'Project A',
                'tester4'
           from dual
         union all
         select 'Project B',
                'pl2'
           from dual
         union all
         select 'Project B',
                'dev5'
           from dual
         union all
         select 'Project B',
                'dev6'
           from dual
         union all
         select 'Project B',
                'dev7'
           from dual
         union all
         select 'Project B',
                'dev8'
           from dual
         union all
         select 'Project B',
                'dev9'
           from dual
         union all
         select 'Project B',
                'tester2'
           from dual
         union all
         select 'Project B',
                'tester3'
           from dual
         union all
         select 'Project B',
                'tester4'
           from dual
         union all
         select 'Project B',
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
      user_login_id,
      joined_at )
   values
      ( source.project_id,
        source.user_login_id,
        source.joined_at );
end;
/
begin
   for seed_issue in (
      select p.id as project_id,
             lower(standard_hash(
                s.project_name
                || ':'
                || s.title,
                'SHA256'
             )) as issue_id,
             s.title,
             s.description,
             s.reported_at,
             s.priority,
             s.status,
             reporter.login_id as reporter_login_id,
             assignee.login_id as assignee_login_id,
             verifier.login_id as verifier_login_id,
             fixer.login_id as fixer_login_id,
             resolver.login_id as resolver_login_id
        from (
         select 'Project A' as project_name,
                'Assignment notification not shown' as title,
                'Assignment notification screen should show missing owner selection, dashboard status, and update keywords.' as description,
                timestamp '2026-03-02 09:00:00' as reported_at,
                'MINOR' as priority,
                'NEW' as status,
                'tester1' as reporter_login,
                null as assignee_login,
                null as verifier_login,
                null as fixer_login,
                null as resolver_login
           from dual
         union all
         select 'Project A',
                'Dependency resolution flow blocked',
                'Blocked issue should wait until its blocking issue is resolved or closed.',
                timestamp '2026-03-03 09:00:00',
                'BLOCKER',
                'ASSIGNED',
                'tester3',
                'dev1',
                'tester1',
                null,
                null
           from dual
         union all
         select 'Project A',
                'API error badge missing',
                'Assigned API errors should show a visible dashboard badge.',
                timestamp '2026-03-03 09:00:00',
                'MAJOR',
                'ASSIGNED',
                'tester2',
                'dev2',
                'tester2',
                null,
                null
           from dual
         union all
         select 'Project A',
                'Login form validation race',
                'Login validation race should be fixed before resolution to avoid auth failure and timeout errors.',
                timestamp '2026-03-03 09:00:00',
                'CRITICAL',
                'FIXED',
                'tester1',
                'dev3',
                'tester3',
                'dev3',
                null
           from dual
         union all
         select 'Project A',
                'Session timeout warning absent',
                'Timeout warning is fixed and waiting for tester verification.',
                timestamp '2026-04-10 09:00:00',
                'MAJOR',
                'FIXED',
                'tester4',
                'dev4',
                'tester1',
                'dev4',
                null
           from dual
         union all
         select 'Project A',
                'Search result filter returns stale status',
                'Search filters should reflect the latest issue status and priority.',
                timestamp '2026-04-01 09:00:00',
                'CRITICAL',
                'RESOLVED',
                'tester2',
                null,
                null,
                'dev1',
                'tester1'
           from dual
         union all
         select 'Project A',
                'Verification rejection returns to assignee',
                'Rejected verification should send the issue back to assigned developer with comment history.',
                timestamp '2026-04-01 09:00:00',
                'MAJOR',
                'RESOLVED',
                'tester4',
                null,
                null,
                'dev1',
                'tester2'
           from dual
         union all
         select 'Project A',
                'Profile page cache invalidation',
                'Profile update cache invalidation was fixed and verified.',
                timestamp '2026-04-07 09:00:00',
                'MINOR',
                'RESOLVED',
                'tester1',
                null,
                null,
                'dev2',
                'tester1'
           from dual
         union all
         select 'Project A',
                'Notification preference save',
                'Notification preference persistence has been verified.',
                timestamp '2026-04-13 09:00:00',
                'TRIVIAL',
                'RESOLVED',
                'tester3',
                null,
                null,
                'dev3',
                'tester3'
           from dual
         union all
         select 'Project A',
                'Login fails on invalid credential',
                'Login failure message should be stable for invalid credentials.',
                timestamp '2026-04-13 09:00:00',
                'MAJOR',
                'CLOSED',
                'tester1',
                null,
                null,
                'dev1',
                'tester2'
           from dual
         union all
         select 'Project A',
                'Comment edit audit trail',
                'Comment edits should leave audit history entries.',
                timestamp '2026-05-02 09:00:00',
                'MAJOR',
                'CLOSED',
                'tester2',
                null,
                null,
                'dev1',
                'tester1'
           from dual
         union all
         select 'Project A',
                'Project member removal guard',
                'Assigned project members should not be removable while work is active.',
                timestamp '2026-05-08 09:00:00',
                'CRITICAL',
                'CLOSED',
                'tester3',
                null,
                null,
                'dev2',
                'tester3'
           from dual
         union all
         select 'Project A',
                'Statistics chart label overflow',
                'Statistics chart labels should not overlap in the project dashboard.',
                timestamp '2026-05-08 09:00:00',
                'MINOR',
                'CLOSED',
                'tester4',
                null,
                null,
                'dev4',
                'tester4'
           from dual
         union all
         select 'Project A',
                'Reopened login regression',
                'Closed login regression was reopened for further investigation.',
                timestamp '2026-05-08 09:00:00',
                'BLOCKER',
                'REOPENED',
                'tester2',
                null,
                null,
                'dev2',
                'tester2'
           from dual
         union all
         select 'Project A',
                'Duplicate mobile login report',
                'Duplicate issue should be deleted from NEW and restored to NEW when needed.',
                timestamp '2026-05-20 09:00:00',
                'TRIVIAL',
                'DELETED',
                'tester2',
                null,
                null,
                null,
                null
           from dual
         union all
         select 'Project B',
                'Dashboard widget missing tooltip',
                'New dashboard tooltip issue is awaiting PL assignment.',
                timestamp '2026-03-01 09:00:00',
                'MINOR',
                'NEW',
                'tester2',
                null,
                null,
                null,
                null
           from dual
         union all
         select 'Project B',
                'Bulk import validation stuck',
                'Bulk import validation is assigned for active investigation.',
                timestamp '2026-03-01 09:00:00',
                'MAJOR',
                'ASSIGNED',
                'tester3',
                'dev5',
                'tester2',
                null,
                null
           from dual
         union all
         select 'Project B',
                'Email digest delivery delay',
                'Email digest delay is assigned to the notification team.',
                timestamp '2026-05-01 09:00:00',
                'CRITICAL',
                'ASSIGNED',
                'tester4',
                'dev6',
                'tester3',
                null,
                null
           from dual
         union all
         select 'Project B',
                'Report export fails after generation',
                'Report export generation failure is waiting for verification after format validation.',
                timestamp '2026-03-12 09:00:00',
                'MAJOR',
                'FIXED',
                'tester4',
                'dev7',
                'tester4',
                'dev7',
                null
           from dual
         union all
         select 'Project B',
                'Export filename timezone mismatch',
                'Export filename timezone mismatch has been fixed after format validation.',
                timestamp '2026-03-12 09:00:00',
                'MINOR',
                'FIXED',
                'tester5',
                'dev8',
                'tester4',
                'dev8',
                null
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'Reopened issues should preserve assignment history and allow reassignment.',
                timestamp '2026-03-15 09:00:00',
                'CRITICAL',
                'RESOLVED',
                'tester5',
                null,
                null,
                'dev6',
                'tester3'
           from dual
         union all
         select 'Project B',
                'SLA report excludes reopened issue',
                'SLA report now includes reopened issue paths.',
                timestamp '2026-04-04 09:00:00',
                'MAJOR',
                'RESOLVED',
                'tester2',
                null,
                null,
                'dev5',
                'tester2'
           from dual
         union all
         select 'Project B',
                'Attachment preview broken',
                'Attachment preview rendering was fixed and verified.',
                timestamp '2026-04-04 09:00:00',
                'MINOR',
                'RESOLVED',
                'tester3',
                null,
                null,
                'dev5',
                'tester3'
           from dual
         union all
         select 'Project B',
                'Mobile dashboard empty state',
                'Mobile dashboard empty state has been verified.',
                timestamp '2026-04-04 09:00:00',
                'TRIVIAL',
                'RESOLVED',
                'tester4',
                null,
                null,
                'dev6',
                'tester4'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'Dashboard statistics should include closed issues in status trend report queries.',
                timestamp '2026-04-16 09:00:00',
                'MAJOR',
                'CLOSED',
                'tester4',
                null,
                null,
                'dev5',
                'tester2'
           from dual
         union all
         select 'Project B',
                'Release note typo cleanup',
                'Release note typo cleanup is closed after verification.',
                timestamp '2026-05-03 09:00:00',
                'TRIVIAL',
                'CLOSED',
                'tester2',
                null,
                null,
                'dev5',
                'tester2'
           from dual
         union all
         select 'Project B',
                'Slow project overview query',
                'Project overview query optimization is closed after slow dashboard performance tuning.',
                timestamp '2026-05-03 09:00:00',
                'BLOCKER',
                'CLOSED',
                'tester3',
                null,
                null,
                'dev7',
                'tester4'
           from dual
         union all
         select 'Project B',
                'Permission label mismatch',
                'Permission label mismatch was closed after UI verification.',
                timestamp '2026-05-10 09:00:00',
                'MINOR',
                'CLOSED',
                'tester5',
                null,
                null,
                'dev8',
                'tester5'
           from dual
         union all
         select 'Project B',
                'Reopened export regression',
                'Closed export regression was reopened for PL review.',
                timestamp '2026-05-18 09:00:00',
                'CRITICAL',
                'REOPENED',
                'tester4',
                null,
                null,
                'dev6',
                'tester3'
           from dual
         union all
         select 'Project B',
                'Retired browser support checklist',
                'Retired browser checklist should be deleted from CLOSED state.',
                timestamp '2026-05-18 09:00:00',
                'MINOR',
                'DELETED',
                'tester5',
                null,
                null,
                'dev8',
                'tester5'
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
       order by s.reported_at,
                s.project_name,
                s.title
   ) loop
      merge into issues target
      using (
         select seed_issue.project_id as project_id,
                seed_issue.issue_id as issue_id,
                seed_issue.title as title,
                seed_issue.description as description,
                seed_issue.reported_at as reported_at,
                seed_issue.priority as priority,
                seed_issue.status as status,
                seed_issue.reporter_login_id as reporter_login_id,
                seed_issue.assignee_login_id as assignee_login_id,
                seed_issue.verifier_login_id as verifier_login_id,
                seed_issue.fixer_login_id as fixer_login_id,
                seed_issue.resolver_login_id as resolver_login_id
           from dual
      ) source on ( target.project_id = source.project_id
         and target.title = source.title )
      when matched then update
      set target.issue_id = source.issue_id,
          target.description = source.description,
          target.reported_at = source.reported_at,
          target.priority = source.priority,
          target.status = source.status,
          target.reporter_login_id = source.reporter_login_id,
          target.assignee_login_id = source.assignee_login_id,
          target.verifier_login_id = source.verifier_login_id,
          target.fixer_login_id = source.fixer_login_id,
          target.resolver_login_id = source.resolver_login_id,
          target.updated_at = source.reported_at
      when not matched then
      insert (
         project_id,
         issue_id,
         title,
         description,
         reported_at,
         priority,
         status,
         reporter_login_id,
         assignee_login_id,
         verifier_login_id,
         fixer_login_id,
         resolver_login_id,
         updated_at )
      values
         ( source.project_id,
           source.issue_id,
           source.title,
           source.description,
           source.reported_at,
           source.priority,
           source.status,
           source.reporter_login_id,
           source.assignee_login_id,
           source.verifier_login_id,
           source.fixer_login_id,
           source.resolver_login_id,
           source.reported_at );
   end loop;
end;
/
begin
   for seed_dependency in (
      select blocking_issue.id as blocking_issue_id,
             blocked_issue.id as blocked_issue_id,
             lower(standard_hash(
                to_char(blocking_issue.id)
                || ':'
                || to_char(blocked_issue.id),
                'SHA256'
             )) as dependency_id,
             s.discovered_at
        from (
         select 'Project A' as blocking_project_name,
                'Assignment notification not shown' as blocking_title,
                'Project A' as blocked_project_name,
                'Dependency resolution flow blocked' as blocked_title,
                timestamp '2026-03-03 12:00:00' as discovered_at
           from dual
         union all
         select 'Project B',
                'Bulk import validation stuck',
                'Project B',
                'Report export fails after generation',
                timestamp '2026-03-12 12:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard widget missing tooltip',
                'Project B',
                'Export filename timezone mismatch',
                timestamp '2026-03-12 12:00:00'
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
       order by s.discovered_at,
                blocking_issue.id,
                blocked_issue.id
   ) loop
      merge into issue_dependencies target
      using (
         select seed_dependency.dependency_id as dependency_id,
                seed_dependency.blocking_issue_id as blocking_issue_id,
                seed_dependency.blocked_issue_id as blocked_issue_id,
                seed_dependency.discovered_at as discovered_at
           from dual
      ) source on ( target.blocking_issue_id = source.blocking_issue_id
         and target.blocked_issue_id = source.blocked_issue_id )
      when matched then update
      set target.dependency_id = source.dependency_id,
          target.discovered_at = source.discovered_at
      when not matched then
      insert (
         dependency_id,
         blocking_issue_id,
         blocked_issue_id,
         discovered_at )
      values
         ( source.dependency_id,
           source.blocking_issue_id,
           source.blocked_issue_id,
           source.discovered_at );
   end loop;
end;
/
begin
   for seed_comment in (
      select i.id as issue_id,
             u.login_id as writer_login_id,
             s.content,
             s.purpose,
             s.created_at,
             s.created_at as updated_at
        from (
         select 'Project A' as project_name,
                'Login form validation race' as issue_title,
                'dev3' as writer_login,
                'Login form validation race fix implemented' as content,
                'STATUS_CHANGE' as purpose,
                timestamp '2026-03-04 10:00:00' as created_at
           from dual
         union all
         select 'Project A',
                'Session timeout warning absent',
                'dev4',
                'Session timeout warning absent fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-05-07 10:00:00'
           from dual
         union all
         select 'Project A',
                'Search result filter returns stale status',
                'dev1',
                'Search result filter returns stale status fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-04-02 10:00:00'
           from dual
         union all
         select 'Project A',
                'Search result filter returns stale status',
                'tester1',
                'Search result filter returns stale status verification complete',
                'STATUS_CHANGE',
                timestamp '2026-04-03 10:00:00'
           from dual
         union all
         select 'Project A',
                'Verification rejection returns to assignee',
                'dev1',
                'Initial fix ready for verification',
                'STATUS_CHANGE',
                timestamp '2026-04-02 10:00:00'
           from dual
         union all
         select 'Project A',
                'Verification rejection returns to assignee',
                'tester2',
                'Verification failed due to stale cache',
                'STATUS_CHANGE',
                timestamp '2026-04-03 10:00:00'
           from dual
         union all
         select 'Project A',
                'Verification rejection returns to assignee',
                'dev1',
                'Rework completed after rejection',
                'STATUS_CHANGE',
                timestamp '2026-04-04 10:00:00'
           from dual
         union all
         select 'Project A',
                'Verification rejection returns to assignee',
                'tester2',
                'Reverification complete',
                'STATUS_CHANGE',
                timestamp '2026-04-30 10:00:00'
           from dual
         union all
         select 'Project A',
                'Profile page cache invalidation',
                'dev2',
                'Profile page cache invalidation fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-04-08 10:00:00'
           from dual
         union all
         select 'Project A',
                'Profile page cache invalidation',
                'tester1',
                'Profile page cache invalidation verification complete',
                'STATUS_CHANGE',
                timestamp '2026-04-09 10:00:00'
           from dual
         union all
         select 'Project A',
                'Notification preference save',
                'dev3',
                'Notification preference save fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-04-14 10:00:00'
           from dual
         union all
         select 'Project A',
                'Notification preference save',
                'tester3',
                'Notification preference save verification complete',
                'STATUS_CHANGE',
                timestamp '2026-05-10 10:00:00'
           from dual
         union all
         select 'Project A',
                'Login fails on invalid credential',
                'dev1',
                'Login fails on invalid credential fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-05-09 10:00:00'
           from dual
         union all
         select 'Project A',
                'Login fails on invalid credential',
                'tester2',
                'Login fails on invalid credential verification complete',
                'STATUS_CHANGE',
                timestamp '2026-05-10 10:00:00'
           from dual
         union all
         select 'Project A',
                'Login fails on invalid credential',
                'pl1',
                'Login fails on invalid credential closed by PL',
                'STATUS_CHANGE',
                timestamp '2026-05-11 10:00:00'
           from dual
         union all
         select 'Project A',
                'Comment edit audit trail',
                'dev1',
                'Comment edit audit trail fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-05-03 10:00:00'
           from dual
         union all
         select 'Project A',
                'Comment edit audit trail',
                'tester1',
                'Comment edit audit trail verification complete',
                'STATUS_CHANGE',
                timestamp '2026-05-04 10:00:00'
           from dual
         union all
         select 'Project A',
                'Comment edit audit trail',
                'pl1',
                'Comment edit audit trail closed by PL',
                'STATUS_CHANGE',
                timestamp '2026-05-05 10:00:00'
           from dual
         union all
         select 'Project A',
                'Project member removal guard',
                'dev2',
                'Project member removal guard fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-05-09 10:00:00'
           from dual
         union all
         select 'Project A',
                'Project member removal guard',
                'tester3',
                'Project member removal guard verification complete',
                'STATUS_CHANGE',
                timestamp '2026-05-10 10:00:00'
           from dual
         union all
         select 'Project A',
                'Project member removal guard',
                'pl1',
                'Project member removal guard closed by PL',
                'STATUS_CHANGE',
                timestamp '2026-05-11 10:00:00'
           from dual
         union all
         select 'Project A',
                'Statistics chart label overflow',
                'dev4',
                'Statistics chart label overflow fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-05-09 10:00:00'
           from dual
         union all
         select 'Project A',
                'Statistics chart label overflow',
                'tester4',
                'Statistics chart label overflow verification complete',
                'STATUS_CHANGE',
                timestamp '2026-05-10 10:00:00'
           from dual
         union all
         select 'Project A',
                'Statistics chart label overflow',
                'pl1',
                'Statistics chart label overflow closed by PL',
                'STATUS_CHANGE',
                timestamp '2026-05-11 10:00:00'
           from dual
         union all
         select 'Project A',
                'Reopened login regression',
                'dev2',
                'Reopened login regression fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-05-09 10:00:00'
           from dual
         union all
         select 'Project A',
                'Reopened login regression',
                'tester2',
                'Reopened login regression verification complete',
                'STATUS_CHANGE',
                timestamp '2026-05-10 10:00:00'
           from dual
         union all
         select 'Project A',
                'Reopened login regression',
                'pl1',
                'Reopened login regression closed by PL',
                'STATUS_CHANGE',
                timestamp '2026-05-11 10:00:00'
           from dual
         union all
         select 'Project A',
                'Reopened login regression',
                'pl1',
                'Reopened login regression reopened after regression',
                'STATUS_CHANGE',
                timestamp '2026-05-12 10:00:00'
           from dual
         union all
         select 'Project B',
                'Report export fails after generation',
                'dev7',
                'Report export fails after generation fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-03-13 10:00:00'
           from dual
         union all
         select 'Project B',
                'Export filename timezone mismatch',
                'dev8',
                'Export filename timezone mismatch fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-04-08 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'dev6',
                'Initial reassignment fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-03-16 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'tester3',
                'Initial verification complete',
                'STATUS_CHANGE',
                timestamp '2026-03-17 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'pl2',
                'Regression reproduced after reopen',
                'STATUS_CHANGE',
                timestamp '2026-03-18 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'dev6',
                'Regression fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-04-14 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'tester3',
                'Regression verification complete',
                'STATUS_CHANGE',
                timestamp '2026-04-15 10:00:00'
           from dual
         union all
         select 'Project B',
                'SLA report excludes reopened issue',
                'dev5',
                'SLA report excludes reopened issue fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-04-05 10:00:00'
           from dual
         union all
         select 'Project B',
                'SLA report excludes reopened issue',
                'tester2',
                'SLA report excludes reopened issue verification complete',
                'STATUS_CHANGE',
                timestamp '2026-04-06 10:00:00'
           from dual
         union all
         select 'Project B',
                'Attachment preview broken',
                'dev5',
                'Attachment preview broken fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-04-05 10:00:00'
           from dual
         union all
         select 'Project B',
                'Attachment preview broken',
                'tester3',
                'Attachment preview broken verification complete',
                'STATUS_CHANGE',
                timestamp '2026-04-06 10:00:00'
           from dual
         union all
         select 'Project B',
                'Mobile dashboard empty state',
                'dev6',
                'Mobile dashboard empty state fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-04-05 10:00:00'
           from dual
         union all
         select 'Project B',
                'Mobile dashboard empty state',
                'tester4',
                'Mobile dashboard empty state verification complete',
                'STATUS_CHANGE',
                timestamp '2026-05-01 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'dev5',
                'Initial dashboard fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-05-12 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'tester2',
                'Initial dashboard verification complete',
                'STATUS_CHANGE',
                timestamp '2026-05-13 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'pl2',
                'Initial dashboard issue closed',
                'STATUS_CHANGE',
                timestamp '2026-05-14 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'pl2',
                'Closed dashboard issue reopened for regression',
                'STATUS_CHANGE',
                timestamp '2026-05-15 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'dev5',
                'Follow-up dashboard fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-05-17 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'tester2',
                'Follow-up dashboard verification complete',
                'STATUS_CHANGE',
                timestamp '2026-05-18 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'pl2',
                'Reclosed after release regression verification',
                'STATUS_CHANGE',
                timestamp '2026-05-19 10:00:00'
           from dual
         union all
         select 'Project B',
                'Release note typo cleanup',
                'dev5',
                'Release note typo cleanup fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-05-04 10:00:00'
           from dual
         union all
         select 'Project B',
                'Release note typo cleanup',
                'tester2',
                'Release note typo cleanup verification complete',
                'STATUS_CHANGE',
                timestamp '2026-05-05 10:00:00'
           from dual
         union all
         select 'Project B',
                'Release note typo cleanup',
                'pl2',
                'Release note typo cleanup closed by PL',
                'STATUS_CHANGE',
                timestamp '2026-05-06 10:00:00'
           from dual
         union all
         select 'Project B',
                'Slow project overview query',
                'dev7',
                'Slow project overview query fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-05-04 10:00:00'
           from dual
         union all
         select 'Project B',
                'Slow project overview query',
                'tester4',
                'Slow project overview query verification complete',
                'STATUS_CHANGE',
                timestamp '2026-05-05 10:00:00'
           from dual
         union all
         select 'Project B',
                'Slow project overview query',
                'pl2',
                'Slow project overview query closed by PL',
                'STATUS_CHANGE',
                timestamp '2026-05-06 10:00:00'
           from dual
         union all
         select 'Project B',
                'Permission label mismatch',
                'dev8',
                'Permission label mismatch fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-05-11 10:00:00'
           from dual
         union all
         select 'Project B',
                'Permission label mismatch',
                'tester5',
                'Permission label mismatch verification complete',
                'STATUS_CHANGE',
                timestamp '2026-05-12 10:00:00'
           from dual
         union all
         select 'Project B',
                'Permission label mismatch',
                'pl2',
                'Permission label mismatch closed by PL',
                'STATUS_CHANGE',
                timestamp '2026-05-13 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened export regression',
                'dev6',
                'Reopened export regression fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-05-19 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened export regression',
                'tester3',
                'Reopened export regression verification complete',
                'STATUS_CHANGE',
                timestamp '2026-05-20 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened export regression',
                'pl2',
                'Reopened export regression closed by PL',
                'STATUS_CHANGE',
                timestamp '2026-05-21 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened export regression',
                'pl2',
                'Reopened export regression reopened after regression',
                'STATUS_CHANGE',
                timestamp '2026-05-22 10:00:00'
           from dual
         union all
         select 'Project B',
                'Retired browser support checklist',
                'dev8',
                'Retired browser support checklist fix implemented',
                'STATUS_CHANGE',
                timestamp '2026-05-19 10:00:00'
           from dual
         union all
         select 'Project B',
                'Retired browser support checklist',
                'tester5',
                'Retired browser support checklist verification complete',
                'STATUS_CHANGE',
                timestamp '2026-05-20 10:00:00'
           from dual
         union all
         select 'Project B',
                'Retired browser support checklist',
                'pl2',
                'Retired browser support checklist closed by PL',
                'STATUS_CHANGE',
                timestamp '2026-05-21 10:00:00'
           from dual
         union all
         select 'Project A',
                'Assignment notification not shown',
                'tester1',
                'General note for Assignment notification not shown.',
                'GENERAL',
                timestamp '2026-03-02 09:30:00'
           from dual
         union all
         select 'Project A',
                'Dependency resolution flow blocked',
                'tester3',
                'General note for Dependency resolution flow blocked.',
                'GENERAL',
                timestamp '2026-03-03 09:30:00'
           from dual
         union all
         select 'Project A',
                'API error badge missing',
                'tester2',
                'General note for API error badge missing.',
                'GENERAL',
                timestamp '2026-03-04 09:30:00'
           from dual
         union all
         select 'Project A',
                'Login form validation race',
                'tester1',
                'General note for Login form validation race.',
                'GENERAL',
                timestamp '2026-03-05 09:30:00'
           from dual
         union all
         select 'Project A',
                'Session timeout warning absent',
                'tester4',
                'General note for Session timeout warning absent.',
                'GENERAL',
                timestamp '2026-04-10 09:30:00'
           from dual
         union all
         select 'Project A',
                'Search result filter returns stale status',
                'tester2',
                'General note for Search result filter returns stale status.',
                'GENERAL',
                timestamp '2026-04-02 09:30:00'
           from dual
         union all
         select 'Project A',
                'Verification rejection returns to assignee',
                'tester4',
                'General note for Verification rejection returns to assignee.',
                'GENERAL',
                timestamp '2026-04-03 09:30:00'
           from dual
         union all
         select 'Project A',
                'Profile page cache invalidation',
                'tester1',
                'General note for Profile page cache invalidation.',
                'GENERAL',
                timestamp '2026-04-07 09:30:00'
           from dual
         union all
         select 'Project A',
                'Notification preference save',
                'tester3',
                'General note for Notification preference save.',
                'GENERAL',
                timestamp '2026-04-13 09:30:00'
           from dual
         union all
         select 'Project A',
                'Login fails on invalid credential',
                'tester1',
                'General note for Login fails on invalid credential.',
                'GENERAL',
                timestamp '2026-04-13 09:30:00'
           from dual
         union all
         select 'Project A',
                'Comment edit audit trail',
                'tester2',
                'General note for Comment edit audit trail.',
                'GENERAL',
                timestamp '2026-05-02 09:30:00'
           from dual
         union all
         select 'Project A',
                'Project member removal guard',
                'tester3',
                'General note for Project member removal guard.',
                'GENERAL',
                timestamp '2026-05-08 09:30:00'
           from dual
         union all
         select 'Project A',
                'Statistics chart label overflow',
                'tester4',
                'General note for Statistics chart label overflow.',
                'GENERAL',
                timestamp '2026-05-08 09:30:00'
           from dual
         union all
         select 'Project A',
                'Reopened login regression',
                'tester2',
                'General note for Reopened login regression.',
                'GENERAL',
                timestamp '2026-05-08 09:30:00'
           from dual
         union all
         select 'Project A',
                'Duplicate mobile login report',
                'tester2',
                'General note for Duplicate mobile login report.',
                'GENERAL',
                timestamp '2026-05-20 09:30:00'
           from dual
         union all
         select 'Project B',
                'Dashboard widget missing tooltip',
                'tester2',
                'General note for Dashboard widget missing tooltip.',
                'GENERAL',
                timestamp '2026-03-02 09:30:00'
           from dual
         union all
         select 'Project B',
                'Bulk import validation stuck',
                'tester3',
                'General note for Bulk import validation stuck.',
                'GENERAL',
                timestamp '2026-03-03 09:30:00'
           from dual
         union all
         select 'Project B',
                'Email digest delivery delay',
                'tester4',
                'General note for Email digest delivery delay.',
                'GENERAL',
                timestamp '2026-05-01 09:30:00'
           from dual
         union all
         select 'Project B',
                'Report export fails after generation',
                'tester4',
                'General note for Report export fails after generation.',
                'GENERAL',
                timestamp '2026-03-12 09:30:00'
           from dual
         union all
         select 'Project B',
                'Export filename timezone mismatch',
                'tester5',
                'General note for Export filename timezone mismatch.',
                'GENERAL',
                timestamp '2026-03-12 09:30:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'tester5',
                'General note for Reopened issue keeps old assignee.',
                'GENERAL',
                timestamp '2026-04-02 09:30:00'
           from dual
         union all
         select 'Project B',
                'SLA report excludes reopened issue',
                'tester2',
                'General note for SLA report excludes reopened issue.',
                'GENERAL',
                timestamp '2026-04-04 09:30:00'
           from dual
         union all
         select 'Project B',
                'Attachment preview broken',
                'tester3',
                'General note for Attachment preview broken.',
                'GENERAL',
                timestamp '2026-04-04 09:30:00'
           from dual
         union all
         select 'Project B',
                'Mobile dashboard empty state',
                'tester4',
                'General note for Mobile dashboard empty state.',
                'GENERAL',
                timestamp '2026-04-05 09:30:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'tester4',
                'General note for Dashboard statistics misses closed issues.',
                'GENERAL',
                timestamp '2026-04-16 09:30:00'
           from dual
         union all
         select 'Project B',
                'Release note typo cleanup',
                'tester2',
                'General note for Release note typo cleanup.',
                'GENERAL',
                timestamp '2026-05-03 09:30:00'
           from dual
         union all
         select 'Project B',
                'Slow project overview query',
                'tester3',
                'General note for Slow project overview query.',
                'GENERAL',
                timestamp '2026-05-03 09:30:00'
           from dual
         union all
         select 'Project B',
                'Permission label mismatch',
                'tester5',
                'General note for Permission label mismatch.',
                'GENERAL',
                timestamp '2026-05-10 09:30:00'
           from dual
         union all
         select 'Project B',
                'Reopened export regression',
                'tester4',
                'General note for Reopened export regression.',
                'GENERAL',
                timestamp '2026-05-18 09:30:00'
           from dual
         union all
         select 'Project B',
                'Retired browser support checklist',
                'tester5',
                'General note for Retired browser support checklist.',
                'GENERAL',
                timestamp '2026-05-18 09:30:00'
           from dual
      ) s
        join projects p
      on p.name = s.project_name
        join issues i
      on i.project_id = p.id
         and i.title = s.issue_title
        join users u
      on u.login_id = s.writer_login
       order by s.created_at,
                i.id,
                s.content
   ) loop
      merge into comments target
      using (
         select seed_comment.issue_id as issue_id,
                seed_comment.writer_login_id as writer_login_id,
                seed_comment.content as content,
                seed_comment.purpose as purpose,
                seed_comment.created_at as created_at,
                seed_comment.updated_at as updated_at
           from dual
      ) source on ( target.issue_id = source.issue_id
         and target.writer_login_id = source.writer_login_id
         and target.created_at = source.created_at )
      when matched then update
      set target.content = source.content,
          target.purpose = source.purpose,
          target.updated_at = source.updated_at
      when not matched then
      insert (
         issue_id,
         writer_login_id,
         content,
         purpose,
         created_at,
         updated_at )
      values
         ( source.issue_id,
           source.writer_login_id,
           source.content,
           source.purpose,
           source.created_at,
           source.updated_at );
   end loop;
end;
/
begin
   for seed_history in (
      select p.name as project_name,
             i.title as issue_title,
             u.login_id as changed_by_login_id,
             s.action_type,
             s.previous_value,
             s.new_value,
             s.message,
             s.changed_at as changed_at,
             i.id as issue_id
        from (
         select 'Project A' as project_name,
                'Assignment notification not shown' as issue_title,
                'tester1' as changed_by_login,
                'CREATED' as action_type,
                null as previous_value,
                'NEW' as new_value,
                'Issue created' as message,
                timestamp '2026-03-02 09:00:00' as changed_at
           from dual
         union all
         select 'Project A',
                'Dependency resolution flow blocked',
                'tester3',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-03-03 09:00:00'
           from dual
         union all
         select 'Project A',
                'Dependency resolution flow blocked',
                'pl1',
                'ASSIGNMENT_CHANGED',
                null,
                'dev1/tester1',
                'Issue assigned from NEW',
                timestamp '2026-03-03 10:00:00'
           from dual
         union all
         select 'Project A',
                'Dependency resolution flow blocked',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-03-03 10:00:00'
           from dual
         union all
         select 'Project A',
                'API error badge missing',
                'tester2',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-03-03 09:00:00'
           from dual
         union all
         select 'Project A',
                'API error badge missing',
                'pl1',
                'ASSIGNMENT_CHANGED',
                null,
                'dev2/tester2',
                'Issue assigned from NEW',
                timestamp '2026-03-03 10:00:00'
           from dual
         union all
         select 'Project A',
                'API error badge missing',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-03-03 10:00:00'
           from dual
         union all
         select 'Project A',
                'Login form validation race',
                'tester1',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-03-03 09:00:00'
           from dual
         union all
         select 'Project A',
                'Login form validation race',
                'pl1',
                'ASSIGNMENT_CHANGED',
                null,
                'dev3/tester3',
                'Issue assigned from NEW',
                timestamp '2026-03-03 10:00:00'
           from dual
         union all
         select 'Project A',
                'Login form validation race',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-03-03 10:00:00'
           from dual
         union all
         select 'Project A',
                'Login form validation race',
                'dev3',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Login form validation race fix implemented',
                timestamp '2026-03-04 10:00:00'
           from dual
         union all
         select 'Project A',
                'Session timeout warning absent',
                'tester4',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-04-10 09:00:00'
           from dual
         union all
         select 'Project A',
                'Session timeout warning absent',
                'pl1',
                'ASSIGNMENT_CHANGED',
                null,
                'dev4/tester1',
                'Issue assigned from NEW',
                timestamp '2026-04-10 10:00:00'
           from dual
         union all
         select 'Project A',
                'Session timeout warning absent',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-04-10 10:00:00'
           from dual
         union all
         select 'Project A',
                'Session timeout warning absent',
                'dev4',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Session timeout warning absent fix implemented',
                timestamp '2026-05-07 10:00:00'
           from dual
         union all
         select 'Project A',
                'Search result filter returns stale status',
                'tester2',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-04-01 09:00:00'
           from dual
         union all
         select 'Project A',
                'Search result filter returns stale status',
                'pl1',
                'ASSIGNMENT_CHANGED',
                null,
                'dev1/tester1',
                'Issue assigned from NEW',
                timestamp '2026-04-01 10:00:00'
           from dual
         union all
         select 'Project A',
                'Search result filter returns stale status',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-04-01 10:00:00'
           from dual
         union all
         select 'Project A',
                'Search result filter returns stale status',
                'dev1',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Search result filter returns stale status fix implemented',
                timestamp '2026-04-02 10:00:00'
           from dual
         union all
         select 'Project A',
                'Search result filter returns stale status',
                'tester1',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Search result filter returns stale status verification complete',
                timestamp '2026-04-03 10:00:00'
           from dual
         union all
         select 'Project A',
                'Verification rejection returns to assignee',
                'tester4',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-04-01 09:00:00'
           from dual
         union all
         select 'Project A',
                'Verification rejection returns to assignee',
                'pl1',
                'ASSIGNMENT_CHANGED',
                null,
                'dev1/tester2',
                'Issue assigned from NEW',
                timestamp '2026-04-01 10:00:00'
           from dual
         union all
         select 'Project A',
                'Verification rejection returns to assignee',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-04-01 10:00:00'
           from dual
         union all
         select 'Project A',
                'Verification rejection returns to assignee',
                'dev1',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Initial fix ready for verification',
                timestamp '2026-04-02 10:00:00'
           from dual
         union all
         select 'Project A',
                'Verification rejection returns to assignee',
                'tester2',
                'STATUS_CHANGED',
                'FIXED',
                'ASSIGNED',
                'Verification failed due to stale cache',
                timestamp '2026-04-03 10:00:00'
           from dual
         union all
         select 'Project A',
                'Verification rejection returns to assignee',
                'dev1',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Rework completed after rejection',
                timestamp '2026-04-04 10:00:00'
           from dual
         union all
         select 'Project A',
                'Verification rejection returns to assignee',
                'tester2',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Reverification complete',
                timestamp '2026-04-30 10:00:00'
           from dual
         union all
         select 'Project A',
                'Profile page cache invalidation',
                'tester1',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-04-07 09:00:00'
           from dual
         union all
         select 'Project A',
                'Profile page cache invalidation',
                'pl1',
                'ASSIGNMENT_CHANGED',
                null,
                'dev2/tester1',
                'Issue assigned from NEW',
                timestamp '2026-04-07 10:00:00'
           from dual
         union all
         select 'Project A',
                'Profile page cache invalidation',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-04-07 10:00:00'
           from dual
         union all
         select 'Project A',
                'Profile page cache invalidation',
                'dev2',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Profile page cache invalidation fix implemented',
                timestamp '2026-04-08 10:00:00'
           from dual
         union all
         select 'Project A',
                'Profile page cache invalidation',
                'tester1',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Profile page cache invalidation verification complete',
                timestamp '2026-04-09 10:00:00'
           from dual
         union all
         select 'Project A',
                'Notification preference save',
                'tester3',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-04-13 09:00:00'
           from dual
         union all
         select 'Project A',
                'Notification preference save',
                'pl1',
                'ASSIGNMENT_CHANGED',
                null,
                'dev3/tester3',
                'Issue assigned from NEW',
                timestamp '2026-04-13 10:00:00'
           from dual
         union all
         select 'Project A',
                'Notification preference save',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-04-13 10:00:00'
           from dual
         union all
         select 'Project A',
                'Notification preference save',
                'dev3',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Notification preference save fix implemented',
                timestamp '2026-04-14 10:00:00'
           from dual
         union all
         select 'Project A',
                'Notification preference save',
                'tester3',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Notification preference save verification complete',
                timestamp '2026-05-10 10:00:00'
           from dual
         union all
         select 'Project A',
                'Login fails on invalid credential',
                'tester1',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-04-13 09:00:00'
           from dual
         union all
         select 'Project A',
                'Login fails on invalid credential',
                'pl1',
                'ASSIGNMENT_CHANGED',
                null,
                'dev1/tester2',
                'Issue assigned from NEW',
                timestamp '2026-04-13 10:00:00'
           from dual
         union all
         select 'Project A',
                'Login fails on invalid credential',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-04-13 10:00:00'
           from dual
         union all
         select 'Project A',
                'Login fails on invalid credential',
                'dev1',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Login fails on invalid credential fix implemented',
                timestamp '2026-05-09 10:00:00'
           from dual
         union all
         select 'Project A',
                'Login fails on invalid credential',
                'tester2',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Login fails on invalid credential verification complete',
                timestamp '2026-05-10 10:00:00'
           from dual
         union all
         select 'Project A',
                'Login fails on invalid credential',
                'pl1',
                'STATUS_CHANGED',
                'RESOLVED',
                'CLOSED',
                'Login fails on invalid credential closed by PL',
                timestamp '2026-05-11 10:00:00'
           from dual
         union all
         select 'Project A',
                'Comment edit audit trail',
                'tester2',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-05-02 09:00:00'
           from dual
         union all
         select 'Project A',
                'Comment edit audit trail',
                'pl1',
                'ASSIGNMENT_CHANGED',
                null,
                'dev1/tester1',
                'Issue assigned from NEW',
                timestamp '2026-05-02 10:00:00'
           from dual
         union all
         select 'Project A',
                'Comment edit audit trail',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-05-02 10:00:00'
           from dual
         union all
         select 'Project A',
                'Comment edit audit trail',
                'dev1',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Comment edit audit trail fix implemented',
                timestamp '2026-05-03 10:00:00'
           from dual
         union all
         select 'Project A',
                'Comment edit audit trail',
                'tester1',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Comment edit audit trail verification complete',
                timestamp '2026-05-04 10:00:00'
           from dual
         union all
         select 'Project A',
                'Comment edit audit trail',
                'pl1',
                'STATUS_CHANGED',
                'RESOLVED',
                'CLOSED',
                'Comment edit audit trail closed by PL',
                timestamp '2026-05-05 10:00:00'
           from dual
         union all
         select 'Project A',
                'Project member removal guard',
                'tester3',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-05-08 09:00:00'
           from dual
         union all
         select 'Project A',
                'Project member removal guard',
                'pl1',
                'ASSIGNMENT_CHANGED',
                null,
                'dev2/tester3',
                'Issue assigned from NEW',
                timestamp '2026-05-08 10:00:00'
           from dual
         union all
         select 'Project A',
                'Project member removal guard',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-05-08 10:00:00'
           from dual
         union all
         select 'Project A',
                'Project member removal guard',
                'dev2',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Project member removal guard fix implemented',
                timestamp '2026-05-09 10:00:00'
           from dual
         union all
         select 'Project A',
                'Project member removal guard',
                'tester3',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Project member removal guard verification complete',
                timestamp '2026-05-10 10:00:00'
           from dual
         union all
         select 'Project A',
                'Project member removal guard',
                'pl1',
                'STATUS_CHANGED',
                'RESOLVED',
                'CLOSED',
                'Project member removal guard closed by PL',
                timestamp '2026-05-11 10:00:00'
           from dual
         union all
         select 'Project A',
                'Statistics chart label overflow',
                'tester4',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-05-08 09:00:00'
           from dual
         union all
         select 'Project A',
                'Statistics chart label overflow',
                'pl1',
                'ASSIGNMENT_CHANGED',
                null,
                'dev4/tester4',
                'Issue assigned from NEW',
                timestamp '2026-05-08 10:00:00'
           from dual
         union all
         select 'Project A',
                'Statistics chart label overflow',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-05-08 10:00:00'
           from dual
         union all
         select 'Project A',
                'Statistics chart label overflow',
                'dev4',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Statistics chart label overflow fix implemented',
                timestamp '2026-05-09 10:00:00'
           from dual
         union all
         select 'Project A',
                'Statistics chart label overflow',
                'tester4',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Statistics chart label overflow verification complete',
                timestamp '2026-05-10 10:00:00'
           from dual
         union all
         select 'Project A',
                'Statistics chart label overflow',
                'pl1',
                'STATUS_CHANGED',
                'RESOLVED',
                'CLOSED',
                'Statistics chart label overflow closed by PL',
                timestamp '2026-05-11 10:00:00'
           from dual
         union all
         select 'Project A',
                'Reopened login regression',
                'tester2',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-05-08 09:00:00'
           from dual
         union all
         select 'Project A',
                'Reopened login regression',
                'pl1',
                'ASSIGNMENT_CHANGED',
                null,
                'dev2/tester2',
                'Issue assigned from NEW',
                timestamp '2026-05-08 10:00:00'
           from dual
         union all
         select 'Project A',
                'Reopened login regression',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-05-08 10:00:00'
           from dual
         union all
         select 'Project A',
                'Reopened login regression',
                'dev2',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Reopened login regression fix implemented',
                timestamp '2026-05-09 10:00:00'
           from dual
         union all
         select 'Project A',
                'Reopened login regression',
                'tester2',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Reopened login regression verification complete',
                timestamp '2026-05-10 10:00:00'
           from dual
         union all
         select 'Project A',
                'Reopened login regression',
                'pl1',
                'STATUS_CHANGED',
                'RESOLVED',
                'CLOSED',
                'Reopened login regression closed by PL',
                timestamp '2026-05-11 10:00:00'
           from dual
         union all
         select 'Project A',
                'Reopened login regression',
                'pl1',
                'STATUS_CHANGED',
                'CLOSED',
                'REOPENED',
                'Reopened login regression reopened after regression',
                timestamp '2026-05-12 10:00:00'
           from dual
         union all
         select 'Project A',
                'Duplicate mobile login report',
                'tester2',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-05-20 09:00:00'
           from dual
         union all
         select 'Project A',
                'Duplicate mobile login report',
                'pl1',
                'STATUS_CHANGED',
                'NEW',
                'DELETED',
                'Deleted duplicate NEW issue before assignment',
                timestamp '2026-05-20 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard widget missing tooltip',
                'tester2',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-03-01 09:00:00'
           from dual
         union all
         select 'Project B',
                'Bulk import validation stuck',
                'tester3',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-03-01 09:00:00'
           from dual
         union all
         select 'Project B',
                'Bulk import validation stuck',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev5/tester2',
                'Issue assigned from NEW',
                timestamp '2026-03-01 10:00:00'
           from dual
         union all
         select 'Project B',
                'Bulk import validation stuck',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-03-01 10:00:00'
           from dual
         union all
         select 'Project B',
                'Email digest delivery delay',
                'tester4',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-05-01 09:00:00'
           from dual
         union all
         select 'Project B',
                'Email digest delivery delay',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev6/tester3',
                'Issue assigned from NEW',
                timestamp '2026-05-01 10:00:00'
           from dual
         union all
         select 'Project B',
                'Email digest delivery delay',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-05-01 10:00:00'
           from dual
         union all
         select 'Project B',
                'Report export fails after generation',
                'tester4',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-03-12 09:00:00'
           from dual
         union all
         select 'Project B',
                'Report export fails after generation',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev7/tester4',
                'Issue assigned from NEW',
                timestamp '2026-03-12 10:00:00'
           from dual
         union all
         select 'Project B',
                'Report export fails after generation',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-03-12 10:00:00'
           from dual
         union all
         select 'Project B',
                'Report export fails after generation',
                'dev7',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Report export fails after generation fix implemented',
                timestamp '2026-03-13 10:00:00'
           from dual
         union all
         select 'Project B',
                'Export filename timezone mismatch',
                'tester5',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-03-12 09:00:00'
           from dual
         union all
         select 'Project B',
                'Export filename timezone mismatch',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev8/tester4',
                'Issue assigned from NEW',
                timestamp '2026-03-12 10:00:00'
           from dual
         union all
         select 'Project B',
                'Export filename timezone mismatch',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-03-12 10:00:00'
           from dual
         union all
         select 'Project B',
                'Export filename timezone mismatch',
                'dev8',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Export filename timezone mismatch fix implemented',
                timestamp '2026-04-08 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'tester5',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-03-15 09:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev6/tester3',
                'Issue assigned from NEW',
                timestamp '2026-03-15 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-03-15 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'dev6',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Initial reassignment fix implemented',
                timestamp '2026-03-16 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'tester3',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Initial verification complete',
                timestamp '2026-03-17 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'pl2',
                'STATUS_CHANGED',
                'RESOLVED',
                'REOPENED',
                'Regression reproduced after reopen',
                timestamp '2026-03-18 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev6/tester3',
                'Issue assigned from REOPENED',
                timestamp '2026-03-19 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'pl2',
                'STATUS_CHANGED',
                'REOPENED',
                'ASSIGNED',
                'Issue assigned from REOPENED',
                timestamp '2026-03-19 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'dev6',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Regression fix implemented',
                timestamp '2026-04-14 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened issue keeps old assignee',
                'tester3',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Regression verification complete',
                timestamp '2026-04-15 10:00:00'
           from dual
         union all
         select 'Project B',
                'SLA report excludes reopened issue',
                'tester2',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-04-04 09:00:00'
           from dual
         union all
         select 'Project B',
                'SLA report excludes reopened issue',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev5/tester2',
                'Issue assigned from NEW',
                timestamp '2026-04-04 10:00:00'
           from dual
         union all
         select 'Project B',
                'SLA report excludes reopened issue',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-04-04 10:00:00'
           from dual
         union all
         select 'Project B',
                'SLA report excludes reopened issue',
                'dev5',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'SLA report excludes reopened issue fix implemented',
                timestamp '2026-04-05 10:00:00'
           from dual
         union all
         select 'Project B',
                'SLA report excludes reopened issue',
                'tester2',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'SLA report excludes reopened issue verification complete',
                timestamp '2026-04-06 10:00:00'
           from dual
         union all
         select 'Project B',
                'Attachment preview broken',
                'tester3',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-04-04 09:00:00'
           from dual
         union all
         select 'Project B',
                'Attachment preview broken',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev5/tester3',
                'Issue assigned from NEW',
                timestamp '2026-04-04 10:00:00'
           from dual
         union all
         select 'Project B',
                'Attachment preview broken',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-04-04 10:00:00'
           from dual
         union all
         select 'Project B',
                'Attachment preview broken',
                'dev5',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Attachment preview broken fix implemented',
                timestamp '2026-04-05 10:00:00'
           from dual
         union all
         select 'Project B',
                'Attachment preview broken',
                'tester3',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Attachment preview broken verification complete',
                timestamp '2026-04-06 10:00:00'
           from dual
         union all
         select 'Project B',
                'Mobile dashboard empty state',
                'tester4',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-04-04 09:00:00'
           from dual
         union all
         select 'Project B',
                'Mobile dashboard empty state',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev6/tester4',
                'Issue assigned from NEW',
                timestamp '2026-04-04 10:00:00'
           from dual
         union all
         select 'Project B',
                'Mobile dashboard empty state',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-04-04 10:00:00'
           from dual
         union all
         select 'Project B',
                'Mobile dashboard empty state',
                'dev6',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Mobile dashboard empty state fix implemented',
                timestamp '2026-04-05 10:00:00'
           from dual
         union all
         select 'Project B',
                'Mobile dashboard empty state',
                'tester4',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Mobile dashboard empty state verification complete',
                timestamp '2026-05-01 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'tester4',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-04-16 09:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev5/tester2',
                'Issue assigned from NEW',
                timestamp '2026-04-16 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-04-16 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'dev5',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Initial dashboard fix implemented',
                timestamp '2026-05-12 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'tester2',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Initial dashboard verification complete',
                timestamp '2026-05-13 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'pl2',
                'STATUS_CHANGED',
                'RESOLVED',
                'CLOSED',
                'Initial dashboard issue closed',
                timestamp '2026-05-14 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'pl2',
                'STATUS_CHANGED',
                'CLOSED',
                'REOPENED',
                'Closed dashboard issue reopened for regression',
                timestamp '2026-05-15 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev5/tester2',
                'Issue assigned from REOPENED',
                timestamp '2026-05-16 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'pl2',
                'STATUS_CHANGED',
                'REOPENED',
                'ASSIGNED',
                'Issue assigned from REOPENED',
                timestamp '2026-05-16 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'dev5',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Follow-up dashboard fix implemented',
                timestamp '2026-05-17 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'tester2',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Follow-up dashboard verification complete',
                timestamp '2026-05-18 10:00:00'
           from dual
         union all
         select 'Project B',
                'Dashboard statistics misses closed issues',
                'pl2',
                'STATUS_CHANGED',
                'RESOLVED',
                'CLOSED',
                'Reclosed after release regression verification',
                timestamp '2026-05-19 10:00:00'
           from dual
         union all
         select 'Project B',
                'Release note typo cleanup',
                'tester2',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-05-03 09:00:00'
           from dual
         union all
         select 'Project B',
                'Release note typo cleanup',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev5/tester2',
                'Issue assigned from NEW',
                timestamp '2026-05-03 10:00:00'
           from dual
         union all
         select 'Project B',
                'Release note typo cleanup',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-05-03 10:00:00'
           from dual
         union all
         select 'Project B',
                'Release note typo cleanup',
                'dev5',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Release note typo cleanup fix implemented',
                timestamp '2026-05-04 10:00:00'
           from dual
         union all
         select 'Project B',
                'Release note typo cleanup',
                'tester2',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Release note typo cleanup verification complete',
                timestamp '2026-05-05 10:00:00'
           from dual
         union all
         select 'Project B',
                'Release note typo cleanup',
                'pl2',
                'STATUS_CHANGED',
                'RESOLVED',
                'CLOSED',
                'Release note typo cleanup closed by PL',
                timestamp '2026-05-06 10:00:00'
           from dual
         union all
         select 'Project B',
                'Slow project overview query',
                'tester3',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-05-03 09:00:00'
           from dual
         union all
         select 'Project B',
                'Slow project overview query',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev7/tester4',
                'Issue assigned from NEW',
                timestamp '2026-05-03 10:00:00'
           from dual
         union all
         select 'Project B',
                'Slow project overview query',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-05-03 10:00:00'
           from dual
         union all
         select 'Project B',
                'Slow project overview query',
                'dev7',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Slow project overview query fix implemented',
                timestamp '2026-05-04 10:00:00'
           from dual
         union all
         select 'Project B',
                'Slow project overview query',
                'tester4',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Slow project overview query verification complete',
                timestamp '2026-05-05 10:00:00'
           from dual
         union all
         select 'Project B',
                'Slow project overview query',
                'pl2',
                'STATUS_CHANGED',
                'RESOLVED',
                'CLOSED',
                'Slow project overview query closed by PL',
                timestamp '2026-05-06 10:00:00'
           from dual
         union all
         select 'Project B',
                'Permission label mismatch',
                'tester5',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-05-10 09:00:00'
           from dual
         union all
         select 'Project B',
                'Permission label mismatch',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev8/tester5',
                'Issue assigned from NEW',
                timestamp '2026-05-10 10:00:00'
           from dual
         union all
         select 'Project B',
                'Permission label mismatch',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-05-10 10:00:00'
           from dual
         union all
         select 'Project B',
                'Permission label mismatch',
                'dev8',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Permission label mismatch fix implemented',
                timestamp '2026-05-11 10:00:00'
           from dual
         union all
         select 'Project B',
                'Permission label mismatch',
                'tester5',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Permission label mismatch verification complete',
                timestamp '2026-05-12 10:00:00'
           from dual
         union all
         select 'Project B',
                'Permission label mismatch',
                'pl2',
                'STATUS_CHANGED',
                'RESOLVED',
                'CLOSED',
                'Permission label mismatch closed by PL',
                timestamp '2026-05-13 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened export regression',
                'tester4',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-05-18 09:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened export regression',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev6/tester3',
                'Issue assigned from NEW',
                timestamp '2026-05-18 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened export regression',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-05-18 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened export regression',
                'dev6',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Reopened export regression fix implemented',
                timestamp '2026-05-19 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened export regression',
                'tester3',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Reopened export regression verification complete',
                timestamp '2026-05-20 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened export regression',
                'pl2',
                'STATUS_CHANGED',
                'RESOLVED',
                'CLOSED',
                'Reopened export regression closed by PL',
                timestamp '2026-05-21 10:00:00'
           from dual
         union all
         select 'Project B',
                'Reopened export regression',
                'pl2',
                'STATUS_CHANGED',
                'CLOSED',
                'REOPENED',
                'Reopened export regression reopened after regression',
                timestamp '2026-05-22 10:00:00'
           from dual
         union all
         select 'Project B',
                'Retired browser support checklist',
                'tester5',
                'CREATED',
                null,
                'NEW',
                'Issue created',
                timestamp '2026-05-18 09:00:00'
           from dual
         union all
         select 'Project B',
                'Retired browser support checklist',
                'pl2',
                'ASSIGNMENT_CHANGED',
                null,
                'dev8/tester5',
                'Issue assigned from NEW',
                timestamp '2026-05-18 10:00:00'
           from dual
         union all
         select 'Project B',
                'Retired browser support checklist',
                'pl2',
                'STATUS_CHANGED',
                'NEW',
                'ASSIGNED',
                'Issue assigned from NEW',
                timestamp '2026-05-18 10:00:00'
           from dual
         union all
         select 'Project B',
                'Retired browser support checklist',
                'dev8',
                'STATUS_CHANGED',
                'ASSIGNED',
                'FIXED',
                'Retired browser support checklist fix implemented',
                timestamp '2026-05-19 10:00:00'
           from dual
         union all
         select 'Project B',
                'Retired browser support checklist',
                'tester5',
                'STATUS_CHANGED',
                'FIXED',
                'RESOLVED',
                'Retired browser support checklist verification complete',
                timestamp '2026-05-20 10:00:00'
           from dual
         union all
         select 'Project B',
                'Retired browser support checklist',
                'pl2',
                'STATUS_CHANGED',
                'RESOLVED',
                'CLOSED',
                'Retired browser support checklist closed by PL',
                timestamp '2026-05-21 10:00:00'
           from dual
         union all
         select 'Project B',
                'Retired browser support checklist',
                'pl2',
                'STATUS_CHANGED',
                'CLOSED',
                'DELETED',
                'Deleted retired CLOSED issue after archive',
                timestamp '2026-05-22 10:00:00'
           from dual
      ) s
        join projects p
      on p.name = s.project_name
        join issues i
      on i.project_id = p.id
         and i.title = s.issue_title
        join users u
      on u.login_id = s.changed_by_login
      union all
      select p.name,
             i.title,
             c.writer_login_id,
             'COMMENTED',
             null,
             c.content,
             case
                when c.purpose = 'GENERAL' then 'comment added'
                else c.content
             end,
             c.created_at as changed_at,
             i.id
        from comments c
        join issues i
      on i.id = c.issue_id
        join projects p
      on p.id = i.project_id
       where p.name in ( 'Project A',
                         'Project B' )
      union all
      select s.blocked_project_name,
             s.blocked_title,
             s.changed_by_login,
             'DEPENDENCY_CHANGED',
             null,
             d.dependency_id,
             'Dependency added',
             s.changed_at as changed_at,
             blocked_issue.id
        from (
         select 'Project A' as blocked_project_name,
                'Dependency resolution flow blocked' as blocked_title,
                'pl1' as changed_by_login,
                timestamp '2026-03-03 12:00:00' as changed_at,
                'Project A' as blocking_project_name,
                'Assignment notification not shown' as blocking_title
           from dual
         union all
         select 'Project B',
                'Report export fails after generation',
                'pl2',
                timestamp '2026-03-05 12:00:00',
                'Project B',
                'Bulk import validation stuck'
           from dual
         union all
         select 'Project B',
                'Export filename timezone mismatch',
                'pl2',
                timestamp '2026-03-06 12:00:00',
                'Project B',
                'Dashboard widget missing tooltip'
           from dual
      ) s
        join projects blocked_project
      on blocked_project.name = s.blocked_project_name
        join issues blocked_issue
      on blocked_issue.project_id = blocked_project.id
         and blocked_issue.title = s.blocked_title
        join projects blocking_project
      on blocking_project.name = s.blocking_project_name
        join issues blocking_issue
      on blocking_issue.project_id = blocking_project.id
         and blocking_issue.title = s.blocking_title
        join issue_dependencies d
      on d.blocking_issue_id = blocking_issue.id
         and d.blocked_issue_id = blocked_issue.id
        join users u
      on u.login_id = s.changed_by_login
       order by 8,
                9,
                4,
                7
   ) loop
      merge into issue_history target
      using (
         select seed_history.issue_id as issue_id,
                seed_history.changed_by_login_id as changed_by_login_id,
                seed_history.action_type as action_type,
                seed_history.previous_value as previous_value,
                seed_history.new_value as new_value,
                seed_history.message as message,
                seed_history.changed_at as changed_at
           from dual
      ) source on ( target.issue_id = source.issue_id
         and target.action_type = source.action_type
         and target.changed_at = source.changed_at )
      when matched then update
      set target.changed_by_login_id = source.changed_by_login_id,
          target.previous_value = source.previous_value,
          target.new_value = source.new_value,
          target.message = source.message
      when not matched then
      insert (
         issue_id,
         changed_by_login_id,
         action_type,
         previous_value,
         new_value,
         message,
         changed_at )
      values
         ( source.issue_id,
           source.changed_by_login_id,
           source.action_type,
           source.previous_value,
           source.new_value,
           source.message,
           source.changed_at );
   end loop;
end;
/
begin
   update issues target
      set
      target.updated_at = (
         select max(history.changed_at)
           from issue_history history
          where history.issue_id = target.id
            and history.action_type <> 'COMMENTED'
      )
    where exists (
      select 1
        from issue_history history
       where history.issue_id = target.id
         and history.action_type <> 'COMMENTED'
   )
      and exists (
      select 1
        from projects project
       where project.id = target.project_id
         and project.name in ( 'Project A',
                               'Project B' )
   );
end;
/
