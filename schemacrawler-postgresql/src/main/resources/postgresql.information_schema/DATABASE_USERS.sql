SELECT
  USENAME AS USERNAME,
  USESYSID,
  USESUPER,
  PASSWD,
  VALUNTIL
FROM
  PG_SHADOW