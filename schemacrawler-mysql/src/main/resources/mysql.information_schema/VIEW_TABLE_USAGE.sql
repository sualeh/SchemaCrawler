SELECT
  VIEW_SCHEMA AS VIEW_CATALOG,
  NULL AS VIEW_SCHEMA,
  VIEW_NAME,
  TABLE_SCHEMA AS TABLE_CATALOG,
  NULL AS TABLE_SCHEMA,
  TABLE_NAME
FROM
  INFORMATION_SCHEMA.VIEW_TABLE_USAGE
ORDER BY
  VIEW_SCHEMA,
  VIEW_NAME,
  TABLE_NAME