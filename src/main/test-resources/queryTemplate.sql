SELECT
  a.*,
  b.int_strs,
  b.varchar_strs,
  b.double_strs,
  b.feature_strs
from
(
  SELECT
    *
  FROM
    ${left_table}
  <#if left_operation_str ??>
  where
    $left_operation_str
  </#if>

) a
${connect_type} join
(
	select
	      left_id,
        group_concat(concat(dd_ref_id, ':::', int_value) separator '|||') as int_strs,
        group_concat(concat(dd_ref_id, ':::', str_value) separator '|||') as vachar_strs,
        group_concat(concat(dd_ref_id, ':::', double_value) separator '|||') as double_strs,
        group_concat(concat(dd_ref_id, ':::', feature_value) separator '|||') as feature_strs
	from
		${right_table}
	where
		${operation_str}
	group BY
	  left_id
	limit ${start_index}, ${size}
)b
on
	a.${primary_id}=b.left_id