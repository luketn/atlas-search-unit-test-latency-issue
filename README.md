# Atlas Search Unit Test Latency
This demonstrates a problem with using Atlas Search locally for unit tests.

After data is written to a collection with an Atlas Search index, there is always 1 second of latency before the data is queryable using an Atlas Search index query.

```shell
mvn test
```

## Feedback for MongoDB
This feedback form describes the issue in more detail:
https://feedback.mongodb.com/forums/924868-atlas-search/suggestions/48502157-atlas-search-local-deployment-lucene-indexing-late


## Output of unit test demonstrating latency:
```
Search index AtlasSearchIndex is ready
Inserting document 0
Found document 0 using findOne query
Searching for document 0 using Atlas Search query
Time taken for search query to find document: 1053ms
Inserting document 1
Found document 1 using findOne query
Searching for document 1 using Atlas Search query
Time taken for search query to find document: 1005ms
...
Inserting document 59
Found document 59 using findOne query
Searching for document 59 using Atlas Search query
Time taken for search query to find document: 1028ms
Time taken for test to run: 60s
Average time taken for search query to find document (and therefore implied indexing latency): 1007ms
```
