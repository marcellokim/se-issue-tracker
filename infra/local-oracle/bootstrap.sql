set define on
set verify off
set feedback on
whenever sqlerror exit sql.sqlcode

define app_user = '&1'
define app_password = '&2'
define test_user = '&3'
define test_password = '&4'

prompt [bootstrap] validating local Oracle arguments
declare
    app_user_input constant varchar2(128) := q'[&&app_user]';
    app_password_input constant varchar2(128) := q'[&&app_password]';
    test_user_input constant varchar2(128) := q'[&&test_user]';
    test_password_input constant varchar2(128) := q'[&&test_password]';

    procedure assert_user(value in varchar2, label in varchar2) is
    begin
        if not regexp_like(value, '^[A-Za-z][A-Za-z0-9_$#]{0,127}$') then
            raise_application_error(
                -20001,
                '[bootstrap] unsupported ' || label || ': use 1-128 chars, start with a letter, then letters/digits/_/$/# only'
            );
        end if;
    end;

    procedure assert_password(value in varchar2, label in varchar2) is
    begin
        if not regexp_like(value, '^[A-Za-z0-9_$#!.\-]{8,128}$') then
            raise_application_error(
                -20002,
                '[bootstrap] unsupported ' || label || ': use 8-128 chars from letters/digits/_/$/#/!/./- only'
            );
        end if;
    end;
begin
    assert_user(app_user_input, 'app_user');
    assert_user(test_user_input, 'test_user');
    assert_password(app_password_input, 'app_password');
    assert_password(test_password_input, 'test_password');
    if upper(app_user_input) = upper(test_user_input) then
        raise_application_error(
            -20003,
            '[bootstrap] app_user and test_user must be different schemas'
        );
    end if;
    if instr(upper(test_user_input), 'TEST') = 0 then
        raise_application_error(
            -20004,
            '[bootstrap] test_user must include TEST because OracleRepositoryIntegrationTest enforces a TEST schema'
        );
    end if;
end;
/

column app_user_upper new_value app_user_upper noprint
column test_user_upper new_value test_user_upper noprint
select upper(q'[&&app_user]') app_user_upper,
       upper(q'[&&test_user]') test_user_upper
  from dual;

prompt [bootstrap] ensuring USERS tablespace
declare
    tablespace_count number;
    users_datafile varchar2(512);
begin
    select count(*)
      into tablespace_count
      from dba_tablespaces
     where tablespace_name = 'USERS';

    if tablespace_count = 0 then
        -- Oracle Free arm64 이미지의 PDB에는 USERS tablespace가 없을 수 있어 로컬용으로 보장함.
        select substr(file_name, 1, instr(file_name, '/', -1)) || 'users01.dbf'
          into users_datafile
          from dba_data_files
         where tablespace_name = 'SYSTEM'
           and rownum = 1;

        execute immediate 'create tablespace users datafile '''
                || replace(users_datafile, '''', '''''')
                || ''' size 100m autoextend on next 50m maxsize unlimited';
    end if;
end;
/

prompt [bootstrap] preparing &&app_user_upper
declare
    user_count number;
begin
    select count(*)
      into user_count
      from dba_users
     where username = '&&app_user_upper';

    if user_count = 0 then
        execute immediate 'create user "&&app_user_upper" identified by "&&app_password" default tablespace users quota unlimited on users';
    else
        execute immediate 'alter user "&&app_user_upper" identified by "&&app_password" account unlock';
        execute immediate 'alter user "&&app_user_upper" default tablespace users';
        execute immediate 'alter user "&&app_user_upper" quota unlimited on users';
    end if;
end;
/

grant create session to "&&app_user_upper";
grant create table to "&&app_user_upper";
grant create view to "&&app_user_upper";
grant create sequence to "&&app_user_upper";
grant create trigger to "&&app_user_upper";
grant create procedure to "&&app_user_upper";

prompt [bootstrap] preparing &&test_user_upper
declare
    user_count number;
begin
    select count(*)
      into user_count
      from dba_users
     where username = '&&test_user_upper';

    if user_count = 0 then
        execute immediate 'create user "&&test_user_upper" identified by "&&test_password" default tablespace users quota unlimited on users';
    else
        execute immediate 'alter user "&&test_user_upper" identified by "&&test_password" account unlock';
        execute immediate 'alter user "&&test_user_upper" default tablespace users';
        execute immediate 'alter user "&&test_user_upper" quota unlimited on users';
    end if;
end;
/

grant create session to "&&test_user_upper";
grant create table to "&&test_user_upper";
grant create view to "&&test_user_upper";
grant create sequence to "&&test_user_upper";
grant create trigger to "&&test_user_upper";
grant create procedure to "&&test_user_upper";

prompt [bootstrap] local Oracle users are ready
exit
