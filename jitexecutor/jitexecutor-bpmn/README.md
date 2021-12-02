# BPMN Runner

Required a kafka server running in 9092 to execute StatelessProcessResourceTest

bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.properties

create topics

bin/kafka-topics.sh --create --topic applicants --bootstrap-server localhost:9092
bin/kafka-topics.sh --create --topic decisions --bootstrap-server localhost:9092

