# Atlas Search Unit Test Latency
This demonstrates a problem with using Atlas Search locally for unit tests.

After data is written to a collection with an Atlas Search index, there is always 1 second of latency before the data is queryable using an Atlas Search index query.




### Testcontainers
Note that I am using a Testcontainers adaptation of Atlas Local CLI's Podman containers for use in unit tests. 

Ref: Go Atlas CLI project:  
https://github.com/mongodb/mongodb-atlas-cli/blob/master/internal/cli/deployments/setup.go

Full detailed description of the code:  
https://medium.com/@luketn/mongodb-local-atlas-deployments-under-the-hood-225b1b685fb7

You can run the code directly against Atlas Search CLI instead of and see the same behaviour.
