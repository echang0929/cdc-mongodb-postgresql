
---
## 1 Preparation

```yaml
# docker-compose.yml
networks:
  mongo-net:
    name: mongo-net
services:
  mongo-replica-1:
    hostname: mongo-replica-1
    container_name: mongo-replica-1
    image: mongo:latest
    command: mongod --replSet rs --bind_ip localhost,mongo-replica-1
    ports:
      - "27018:27017"
    restart: always
  mongo-replica-2:
    hostname: mongo-replica-2
    container_name: mongo-replica-2
    image: mongo:latest
    command: mongod --replSet rs --bind_ip localhost,mongo-replica-2
    ports:
      - "27019:27017"
    restart: always
  mongo-primary:
    hostname: mongo-primary
    container_name: mongo-primary
    depends_on:
      - mongo-replica-1
      - mongo-replica-2
    image: mongo:latest
    command: mongod --replSet rs --bind_ip localhost,mongo-primary
    ports:
      - "27017:27017"
    restart: always
    healthcheck:
      test: echo 'rs.initiate({_id:"rs",members:[{_id:0,host:"mongo-primary:27017",priority:2},{_id:1,host:"mongo-replica-1:27017",priority:0},{_id:2,host:"mongo-replica-2:27017",priority:0}]}).ok || rs.status().ok' | mongosh --port 27017 --quiet
      interval: 10s
      start_period: 30s
    volumes:
      - ./src/main/resources/mongo-init.js:/docker-entrypoint-initdb.d/mongo-init.js
  postgres-db:
    image: postgres:latest
    container_name: postgres-db
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: root
```

```shell
docker-compose up -d --wait
```

```
➜ docker exec mongo-primary mongosh /docker-entrypoint-initdb.d/mongo-init.js
```

```
➜ docker exec mongo-primary mongosh --eval 'show collections'

books
computers
phones
```

---
## Running the Debezium embedded project
```shell
jenv shell 21
mvn clean package
java -jar target/cdc-0.0.1-SNAPSHOT.jar
```

---
## 2 Next Operations
### 2.1 READ
```
➜ docker exec -u postgres postgres-db psql -c \
'select * from public.product;'

price | id |                             description                              |         mongo_id         |         name          | source_collection
-------+----+----------------------------------------------------------------------+--------------------------+-----------------------+-------------------
 12.99 |  1 | Classic novel by Harper Lee exploring themes of racial injustice.    | 66770447e735590f088db601 | To Kill a Mockingbird | books
  9.99 |  2 | Dystopian novel by George Orwell depicting a totalitarian society.   | 66770447e735590f088db602 | 1984                  | books
 14.99 |  3 | F. Scott Fitzgerald's masterpiece capturing the Jazz Age in America. | 66770447e735590f088db603 | The Great Gatsby      | books
   999 |  4 | Premium Apple smartphone with powerful features.                     | 66770447e735590f088db5fb | IPhone 15             | phones
   899 |  5 | Premium Android smartphone with powerful features.                   | 66770447e735590f088db5fc | Samsung Galaxy S23    | phones
   799 |  6 | Flagship phone with top-notch camera and performance.                | 66770447e735590f088db5fd | Google Pixel 6        | phones
  1499 |  7 | High-performance laptop for professionals.                           | 66770447e735590f088db5fe | MacBook Pro           | computers
  1299 |  8 | Powerful laptop with stunning display and long battery life.         | 66770447e735590f088db5ff | Dell XPS 15           | computers
  1099 |  9 | Versatile 2-in-1 laptop with impressive design and performance.      | 66770447e735590f088db600 | HP Spectre x360       | computers
(9 rows)
```

### 2.2 INSERT/CREATE
```
➜ docker exec -it mongo-primary mongosh --eval '
db.phones.insertOne({
  "name": "OnePlus 10 Pro",
  "price": 800,
  "description": "Flagship OnePlus smartphone with advanced features and Hasselblad camera technology."
})
'
    
{
  acknowledged: true,
  insertedId: ObjectId('667704d5b44e4f1c1e8db5fb')
}
```

```
➜ docker exec mongo-primary mongosh --eval 'db.phones.find()'

[
  {
    _id: ObjectId('66770447e735590f088db5fb'),
    name: 'IPhone 15',
    price: 999,
    description: 'Premium Apple smartphone with powerful features.'
  },
  {
    _id: ObjectId('66770447e735590f088db5fc'),
    name: 'Samsung Galaxy S23',
    price: 899,
    description: 'Premium Android smartphone with powerful features.'
  },
  {
    _id: ObjectId('66770447e735590f088db5fd'),
    name: 'Google Pixel 6',
    price: 799,
    description: 'Flagship phone with top-notch camera and performance.'
  },
  {
    _id: ObjectId('667704d5b44e4f1c1e8db5fb'),
    name: 'OnePlus 10 Pro',
    price: 800,
    description: 'Flagship OnePlus smartphone with advanced features and Hasselblad camera technology.'
  }
]
```

```
➜ docker exec -u postgres postgres-db psql -c \
'select * from public.product;'

price | id |                                     description                                      |         mongo_id         |         name          | source_collection
-------+----+--------------------------------------------------------------------------------------+--------------------------+-----------------------+-------------------
 12.99 |  1 | Classic novel by Harper Lee exploring themes of racial injustice.                    | 66770447e735590f088db601 | To Kill a Mockingbird | books
  9.99 |  2 | Dystopian novel by George Orwell depicting a totalitarian society.                   | 66770447e735590f088db602 | 1984                  | books
 14.99 |  3 | F. Scott Fitzgerald's masterpiece capturing the Jazz Age in America.                 | 66770447e735590f088db603 | The Great Gatsby      | books
   999 |  4 | Premium Apple smartphone with powerful features.                                     | 66770447e735590f088db5fb | IPhone 15             | phones
   899 |  5 | Premium Android smartphone with powerful features.                                   | 66770447e735590f088db5fc | Samsung Galaxy S23    | phones
   799 |  6 | Flagship phone with top-notch camera and performance.                                | 66770447e735590f088db5fd | Google Pixel 6        | phones
  1499 |  7 | High-performance laptop for professionals.                                           | 66770447e735590f088db5fe | MacBook Pro           | computers
  1299 |  8 | Powerful laptop with stunning display and long battery life.                         | 66770447e735590f088db5ff | Dell XPS 15           | computers
  1099 |  9 | Versatile 2-in-1 laptop with impressive design and performance.                      | 66770447e735590f088db600 | HP Spectre x360       | computers
   800 | 10 | Flagship OnePlus smartphone with advanced features and Hasselblad camera technology. | 667704d5b44e4f1c1e8db5fb | OnePlus 10 Pro        | phones
(10 rows)
```

### 2.3 UPDATE
```
➜ docker exec -it mongo-primary mongosh --eval '
db.phones.updateOne( 
  { "_id" : ObjectId("667704d5b44e4f1c1e8db5fb") }, 
  { $set: { "price" : 1700 } } 
)
'
    
{
  acknowledged: true,
  insertedId: null,
  matchedCount: 1,
  modifiedCount: 1,
  upsertedCount: 0
}
```

```
docker exec mongo-primary mongosh --eval 'db.phones.find()'

[
  {
    _id: ObjectId('66770447e735590f088db5fb'),
    name: 'IPhone 15',
    price: 999,
    description: 'Premium Apple smartphone with powerful features.'
  },
  {
    _id: ObjectId('66770447e735590f088db5fc'),
    name: 'Samsung Galaxy S23',
    price: 899,
    description: 'Premium Android smartphone with powerful features.'
  },
  {
    _id: ObjectId('66770447e735590f088db5fd'),
    name: 'Google Pixel 6',
    price: 799,
    description: 'Flagship phone with top-notch camera and performance.'
  },
  {
    _id: ObjectId('667704d5b44e4f1c1e8db5fb'),
    name: 'OnePlus 10 Pro',
    price: 1700,
    description: 'Flagship OnePlus smartphone with advanced features and Hasselblad camera technology.'
  }
]
```

```
➜ docker exec -u postgres postgres-db psql -c \
'select * from public.product;'

price | id |                                     description                                      |         mongo_id         |         name          | source_collection
-------+----+--------------------------------------------------------------------------------------+--------------------------+-----------------------+-------------------
 12.99 |  1 | Classic novel by Harper Lee exploring themes of racial injustice.                    | 66770447e735590f088db601 | To Kill a Mockingbird | books
  9.99 |  2 | Dystopian novel by George Orwell depicting a totalitarian society.                   | 66770447e735590f088db602 | 1984                  | books
 14.99 |  3 | F. Scott Fitzgerald's masterpiece capturing the Jazz Age in America.                 | 66770447e735590f088db603 | The Great Gatsby      | books
   999 |  4 | Premium Apple smartphone with powerful features.                                     | 66770447e735590f088db5fb | IPhone 15             | phones
   899 |  5 | Premium Android smartphone with powerful features.                                   | 66770447e735590f088db5fc | Samsung Galaxy S23    | phones
   799 |  6 | Flagship phone with top-notch camera and performance.                                | 66770447e735590f088db5fd | Google Pixel 6        | phones
  1499 |  7 | High-performance laptop for professionals.                                           | 66770447e735590f088db5fe | MacBook Pro           | computers
  1299 |  8 | Powerful laptop with stunning display and long battery life.                         | 66770447e735590f088db5ff | Dell XPS 15           | computers
  1099 |  9 | Versatile 2-in-1 laptop with impressive design and performance.                      | 66770447e735590f088db600 | HP Spectre x360       | computers
  1700 | 10 | Flagship OnePlus smartphone with advanced features and Hasselblad camera technology. | 667704d5b44e4f1c1e8db5fb | OnePlus 10 Pro        | phones
(10 rows)
```

### 2.4 DELETE
```
➜ docker exec -it mongo-primary mongosh --eval '
db.phones.deleteOne({ "_id": ObjectId("667704d5b44e4f1c1e8db5fb") })
'

{ acknowledged: true, deletedCount: 1 }
```

```
➜ docker exec mongo-primary mongosh --eval 'db.phones.find()'

[
  {
    _id: ObjectId('66770447e735590f088db5fb'),
    name: 'IPhone 15',
    price: 999,
    description: 'Premium Apple smartphone with powerful features.'
  },
  {
    _id: ObjectId('66770447e735590f088db5fc'),
    name: 'Samsung Galaxy S23',
    price: 899,
    description: 'Premium Android smartphone with powerful features.'
  },
  {
    _id: ObjectId('66770447e735590f088db5fd'),
    name: 'Google Pixel 6',
    price: 799,
    description: 'Flagship phone with top-notch camera and performance.'
  }
]
```

```
➜ docker exec -u postgres postgres-db psql -c \
'select * from public.product;'

price | id |                             description                              |         mongo_id         |         name          | source_collection
-------+----+----------------------------------------------------------------------+--------------------------+-----------------------+-------------------
 12.99 |  1 | Classic novel by Harper Lee exploring themes of racial injustice.    | 66770447e735590f088db601 | To Kill a Mockingbird | books
  9.99 |  2 | Dystopian novel by George Orwell depicting a totalitarian society.   | 66770447e735590f088db602 | 1984                  | books
 14.99 |  3 | F. Scott Fitzgerald's masterpiece capturing the Jazz Age in America. | 66770447e735590f088db603 | The Great Gatsby      | books
   999 |  4 | Premium Apple smartphone with powerful features.                     | 66770447e735590f088db5fb | IPhone 15             | phones
   899 |  5 | Premium Android smartphone with powerful features.                   | 66770447e735590f088db5fc | Samsung Galaxy S23    | phones
   799 |  6 | Flagship phone with top-notch camera and performance.                | 66770447e735590f088db5fd | Google Pixel 6        | phones
  1499 |  7 | High-performance laptop for professionals.                           | 66770447e735590f088db5fe | MacBook Pro           | computers
  1299 |  8 | Powerful laptop with stunning display and long battery life.         | 66770447e735590f088db5ff | Dell XPS 15           | computers
  1099 |  9 | Versatile 2-in-1 laptop with impressive design and performance.      | 66770447e735590f088db600 | HP Spectre x360       | computers
(9 rows)
```
