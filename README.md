SQL query parser

In SQL, the most syntactically complex and tricky query is probably the SELECT query. It has explicit and implicit joins, groupings,
subqueries, sorting and truncation of selects - all this beauty can occur repeatedly even in one single
select query.

For example, like this:
```sql
SELECT * FROM book
```
or like this:

```sql
SELECT author.name, count(book.id), sum(book.cost) 
FROM author 
LEFT JOIN book ON (author.id = book.author_id) 
GROUP BY author.name 
HAVING COUNT(*) > 1 AND SUM(book.cost) > 500
LIMIT 10;
```

This is a SELECT query, representing it as a class of approximately this structure:
```java
class Query {
	private List<String> columns;
	private List<Source> fromSources;
	private List<Join> joins;
	private List<WhereClause> whereClauses;
	private List<String> groupByColumns;
    private List<HavingClause> havingClauses;
	private List<Sort> sortColumns;
	private Integer limit;
	private Integer offset;
}
```

The parser supports these constructs in a mandatory way:
- Enumeration of sample fields explicitly (with aliases) or *
- Implicit join of several tables (select * from A,B,C)
- Explicit join of tables (inner, left, right, full join)
- Filter conditions (where a = 1 and b > 100)
- Subqueries (select * from (select * from A) a_alias)
- Grouping by one or several fields (group by)
- Sorting by one or more fields (order by)
- Selection truncation (limit, offset)
