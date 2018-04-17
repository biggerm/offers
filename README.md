### Example Akka Cluster Offer Server

##### Run
Run the server with
```sbt run```

##### Test
Run tests with
```sbt test```

##### Enhance
Enhancements to the server could include
* Scale to multiple nodes by configuring multinode Clusters
* Use Google Guice to enable better modularity and dependency management.
* Use Akka persistence and event sourcing to enable storage of offer data that can be loaded on demand.


##### Configuration
The server listens on all interfaces on port 8080 for http requests.

The Server is configured to run as a cluster but with one node. To use multiple nodes
edit the `application.conf` file, and configure the cluster seeds setting.

e.g. 
```hocon
cluster {
    seed-nodes = ["akka.tcp://Offers@10.0.0.1:2551", "akka.tcp://Offers@10.0.0.2:2551"]
  }

```

###  API

#### The HTTP REST API has four main operations


#### Create an Offer

Request:
```
curl -X POST -H "Content-Type: application/json" http://127.0.0.1:8080/offers -d '{
"description": "hello", 
"currency":"GBP", 
"amount":"1245.75", 
"expiry":"2018-04-30T22:30:00Z"
}'

```
 Response:
```json
{ 
    "id":"f2ea869e-093c-4ecf-8fbd-11c538212738",
    "state":"ENABLED",
    "data":{
        "description":"hello",
        "currency":"GBP",
        "amount":"1245.75",
        "expiry":"2018-04-30T22:30:00Z"
    }
}
```

#### Query an Offer

Request:
```
curl -X GET -H "Content-Type: application/json" http://127.0.0.1:8080/offers/f2ea869e-093c-4ecf-8fbd-11c538212738
```

Response:
```json
{ 
    "id":"f2ea869e-093c-4ecf-8fbd-11c538212738",
    "state":"ENABLED",
    "data":{
        "description":"hello",
        "currency":"GBP",
        "amount":"1245.75",
        "expiry":"2018-04-30T22:30:00Z"
    }
}
```

#### List all offers

Request:
```aidl
curl -X GET -H "Content-Type: application/json" http://127.0.0.1:8080/offers
```

Response:
```json
[
    {
        "shardId":"1",
        "entityId":"f2ea869e-093c-4ecf-8fbd-11c538212738"
    },
    {
        "shardId":"-9",
        "entityId":"48ea1231-0407-4d91-bdc9-2304280bbd06"
    },
    {
        "shardId":"-4",
        "entityId":"48990668-7d2c-4c30-b394-8714d486d8bf"
    }
]
```

#### Cancel an Offer

Request:
```
curl -X DELETE -H "Content-Type: application/json" http://127.0.0.1:8080/offers/f2ea869e-093c-4ecf-8fbd-11c538212738
```

Response:
```json
{
  "id":"f2ea869e-093c-4ecf-8fbd-11c538212738",
  "state":"CANCELLED",
  "data":{
    "description":"hello",
    "currency":"GBP",
    "amount":"1245.75",
    "expiry":"2018-04-30T22:30:00Z"
    }
}
```