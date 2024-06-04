package com.mycodefu;

import com.mongodb.client.*;
import org.bson.BsonDocument;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.AtlasMongoDBTest;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.Assert.*;

public class MainTest extends AtlasMongoDBTest {
    public record TestData(String test, int test2, boolean test3) {}

    MongoClient mongoClient;
    MongoCollection<TestData> testCollection;

    /**
     * Set up a test database and a test atlas search index.
     * Note: MongoDB support suggested adding a majority write concern, which I did using &w=majority in the connection string. This did not affect the latency at all.
     * As an alternative to using Testcontainers, you can also run this test with any other Atlas Local CLI instance with the same result.
     */
    @Before
    public void setup() {
        String connectionString = super.connectionString();
        System.out.println("Connecting to " + connectionString);
        mongoClient = MongoClients.create(connectionString);
        MongoDatabase testDB = mongoClient.getDatabase("test");
        testDB.createCollection("test");
        testCollection = testDB.getCollection("test", TestData.class);
        testCollection.createSearchIndex("AtlasSearchIndex",
                BsonDocument.parse("""
                        {
                          "mappings": {
                            "dynamic": false,
                            "fields": {
                              "test2": {
                                "type": "number",
                                "representation": "int64",
                                "indexDoubles": false
                              },
                              "test": {
                                "type": "string"
                              },
                              "test3": {
                                "type": "boolean"
                              }
                            }
                          }
                        }""")
        );

        //wait for the search index to be ready
        boolean ready = false;
        while (!ready) {
            ListSearchIndexesIterable<Document> searchIndexes = testCollection.listSearchIndexes();
            for (Document searchIndex : searchIndexes) {
                if (searchIndex.get("name").equals("AtlasSearchIndex")) {
                    ready = searchIndex.get("status").equals("READY");
                    if (ready) {
                        System.out.println("Search index AtlasSearchIndex is ready");
                        break;
                    }
                }
            }
        }
    }

    @After
    public void tearDown() {
        mongoClient.close();
    }

    /**
     * This should take a few milliseconds to run, but it will take almost exactly a minute.
     * Each iteration of the loop will take almost exactly 1s.
     * The first iteration will usually be shorter, because we start somewhere in the middle of the 1s refresh period:
     *   Time taken for search query to find document: 556ms
     * Example output of iterations:
     *   Time taken for search query to find document: 1009ms
     *   Time taken for search query to find document: 989ms
     *   Time taken for search query to find document: 1018ms
     * Output at the end:
     *   Time taken for test to run: 60s
     */
    @Test
    public void testAtlasSearchLatency() throws InterruptedException {
        Instant startTest = Instant.now();
        for (int i = 0; i < 60; i++) {
            System.out.println("Inserting document " + i);

            TestData testData = new TestData("test", i, i % 2 == 0);
            testCollection.insertOne(testData);

            //find using a findOne query
            TestData foundRegular = testCollection.find(eq("test2", i)).first();
            assertNotNull(foundRegular);

            System.out.println("Found document " + i + " using findOne query");

            //Record the time
            long start = System.currentTimeMillis();

            System.out.println("Searching for document " + i + " using Atlas Search query");

            //find using a search query
            TestData foundSearch = null;
            while(foundSearch == null) {
                List<Document> query = Arrays.asList(new Document("$search",
                                new Document("index", "AtlasSearchIndex")
                                        .append("equals",
                                                new Document()
                                                        .append("path", "test2")
                                                        .append("value", i)
                                        )
                        )
                );
                foundSearch = testCollection.aggregate(query).first();

                if (System.currentTimeMillis() - start > 10_000) {
                    fail("Search query took too long");
                }
                Thread.sleep(10);
            }
            assertNotNull(foundSearch);

            //log time taken for the Atlas Search index to have the document
            System.out.println("Time taken for search query to find document: " + (System.currentTimeMillis() - start) + "ms");
        }
        System.out.println("Time taken for test to run: " + Duration.between(startTest, Instant.now()).getSeconds() + "s");
    }
}