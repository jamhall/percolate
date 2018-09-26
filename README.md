# Percolate

> WIP

Percolate is an abstraction on top of [cqengine](https://github.com/npgall/cqengine) that allows you to filter collections using SQL style where queries.

It has a custom grammar for filtering collections using only expressions. 
All expressions that work in cqengine should work in percolate.

## Example use case

We wanted to give users the ability to filter a GraphQL collection using an expressive syntax.

Example:
 ```
 {
    customers(filter: "age > 20 AND country IN ('UK', 'FRANCE')", orderBy: "name") {
        id
        name
        country
    }
}
```

##  Installation

Maven

> Not currently deployed

```xml
<dependency>
    <groupId>eu.jamiehall</groupId>
    <artifactId>percolate</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

```java
import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.resultset.ResultSet;
import eu.jamiehall.percolate.FilterQuery;

// Create a new query for a given POJO
final FilterQuery<Customer> query = new FilterQuery<>(Customer.class);

// Associate attributes to the parser. Only attributes that are defined can be queried
query.registerAttribute(attribute("name", Customer::getName));
query.registerAttribute(attribute("age", Customer::getAge));
query.registerAttribute(attribute("country", Customer::getCountry));

// Execute a query
final ResultSet<Customer> results = query.execute(customers, "age > 20 AND country IN ('UK', 'FRANCE')");
results.forEach(System.out::println);
```

For more examples, please look at the tests. Also checkout the [cqengine](https://github.com/npgall/cqengine)  documentation.


## Value parsers

You can create a new value parser by extending the `ValueParser` class and then registering it to the query using the `registerValueParser` method.
For an example of a custom value parser please look at the `ByteParser` class.
 
 ## Development
 
 ### Building
 
 ```
mvn clean package
 ```
 
 ### Tests

The tests are written in Kotlin using kotlin-spec.
 