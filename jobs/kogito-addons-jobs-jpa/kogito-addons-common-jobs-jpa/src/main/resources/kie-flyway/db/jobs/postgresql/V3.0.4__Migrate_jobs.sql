UPDATE job_details
SET execution_timeout = (trigger ->> 'nextFireTime')::bigint
WHERE execution_timeout is null;

UPDATE job_details
SET execution_timeout_unit = (trigger ->> 'periodUnit')::varchar
WHERE execution_timeout_unit is null;
