.load ./extension-functions

CREATE TABLE measurements_local
( library    VARCHAR NOT NULL
, runtime_ms INT     NOT NULL
, mem_bytes  INT     NOT NULL
);

CREATE TABLE measurements_ec2
( library    VARCHAR NOT NULL
, runtime_ms INT     NOT NULL
, mem_bytes  INT     NOT NULL
);

CREATE TABLE measurements
( library    VARCHAR NOT NULL
, runtime_ms INT     NOT NULL
, mem_bytes  INT     NOT NULL
, env        VARCHAR NOT NULL
);

.mode csv
.import ./measurements.local.csv measurements_local
.import ./measurements.ec2.csv measurements_ec2

INSERT INTO measurements
SELECT *, 'local' FROM measurements_local;

INSERT INTO measurements
SELECT *, 'EC2' FROM measurements_ec2;

.mode column
.headers on

.print '+----------+'
.print '| Run time |'
.print '+----------+'
.print
.width 12 5 -11 -11 -11 -9 -24 -16 -9 -14 -19
WITH
stats AS (
  SELECT
    library
  , env
  , min(runtime_ms)   AS min_ms
  , max(runtime_ms)   AS max_ms
  , avg(runtime_ms)   AS avg_ms
  , stdev(runtime_ms) AS stdev_ms
  FROM measurements
  GROUP BY library, env
),

all_min_values AS (
  SELECT min(avg_ms) AS all_min_avg_ms
  FROM stats
),

env_min_values AS (
  SELECT env, min(avg_ms) AS env_min_avg_ms
  FROM stats
  GROUP BY env
),

ref_values AS (
  SELECT avg_ms AS ref_avg_ms
  FROM stats
  WHERE library = 'Java StdLib' AND env = 'EC2'
),

delta_avg_ms AS (
  SELECT s2.library, s2.env, 100.0 * (s2.avg_ms - s1.avg_ms) / s1.avg_ms AS percent_change
  FROM stats s1
  JOIN stats s2 ON s1.library = s2.library AND s1.env = 'local' AND s2.env = 'EC2'
)

SELECT
  library
, env
, printf('%02d:%06.03f', min_ms / 60000.0, min_ms % 60000.0 / 1000.0 ) AS "min (mm:ss)"
, printf('%02d:%06.03f', avg_ms / 60000.0, avg_ms % 60000.0 / 1000.0 ) AS "avg (mm:ss)"
, printf('%02d:%06.03f', max_ms / 60000.0, max_ms % 60000.0 / 1000.0 ) AS "max (mm:ss)"
, printf('%02.03f', stdev_ms / 1000.0 )                                AS "stdev (s)"
, printf('%.02f %%', 100.0 * stdev_ms / avg_ms)                        AS "coefficient of variation"
, printf('%.02f %%', 100.0 * avg_ms / env_min_avg_ms)                  AS "% of best in env"
, printf('%.02f %%', 100.0 * avg_ms / all_min_avg_ms)                  AS "% of best"
, printf('%.02f %%', 100.0 * avg_ms / ref_avg_ms)                      AS "% of reference"
, printf('%.02f %%', percent_change)                                   AS "% change from local"
FROM stats
CROSS JOIN ref_values
CROSS JOIN all_min_values
JOIN env_min_values USING (env)
LEFT JOIN delta_avg_ms USING (library, env)
ORDER BY avg_ms;


.print
.print '+-------------------+'
.print '| Peak memory usage |'
.print '+-------------------+'
.print
.width 12 5 -8 -8 -8 -10 -24 -16 -9 -14
WITH
stats AS (
  select
    library
  , env
  , min(mem_bytes)   AS min_bytes
  , max(mem_bytes)   AS max_bytes
  , avg(mem_bytes)   AS avg_bytes
  , stdev(mem_bytes) AS stdev_bytes
  FROM measurements
  GROUP BY library, env
),

all_min_values AS (
  SELECT min(avg_bytes) AS all_min_avg_bytes
  FROM stats
),

env_min_values AS (
  SELECT env, min(avg_bytes) AS env_min_avg_bytes
  FROM stats
  GROUP BY env
),

ref_values AS (
  SELECT avg_bytes AS ref_avg_bytes
  FROM stats
  WHERE library = 'Java StdLib' AND env = 'EC2'
)

select
  library
, env
, printf('%.02f', min_bytes / 1024.0 / 1024.0 )       AS "min (mb)"
, printf('%.02f', avg_bytes / 1024.0 / 1024.0 )       AS "avg (mb)"
, printf('%.02f', max_bytes / 1024.0 / 1024.0 )       AS "max (mb)"
, printf('%.02f', stdev_bytes  / 1024.0 / 1024.0 )    AS "stdev (mb)"
, printf('%.02f %%', 100.0 * stdev_bytes / avg_bytes)   AS "coefficient of variation"
, printf('%.02f %%', 100.0 * avg_bytes / env_min_avg_bytes) AS "% of best in env"
, printf('%.02f %%', 100.0 * avg_bytes / all_min_avg_bytes) AS "% of best"
, printf('%.02f %%', 100.0 * avg_bytes / ref_avg_bytes) AS "% of reference"
FROM stats
CROSS JOIN ref_values
CROSS JOIN all_min_values
JOIN env_min_values USING (env)
ORDER BY avg_bytes;
