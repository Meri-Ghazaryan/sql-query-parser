package com.lightspeed;

import static com.lightspeed.SQLParser.parseQuery;

public class Main {
    public static void main(String[] args) {

        var subquerySql = """
                    SELECT author.name, (SELECT COUNT(*) FROM book WHERE book.author_id = author.id) AS book_count
                    FROM author
                    WHERE EXISTS (SELECT 1 FROM book WHERE book.author_id = author.id)
                    LIMIT 10 OFFSET 5;
                """;

        var joinsSql = """
                SELECT author.name, count(book.id), sum(book.cost)
                FROM author
                LEFT JOIN book ON (author.id = book.author_id)
                GROUP BY author.name
                HAVING COUNT(*) > 1 AND SUM(book.cost) > 500
                LIMIT 10;
                """;

        System.out.println("************* SQL QUERY WITH SUBQUERY *************");
        System.out.println(parseQuery(subquerySql));
        System.out.println("************* SQL QUERY WITH JOINS *************");
        System.out.println(parseQuery(joinsSql));

    }
}