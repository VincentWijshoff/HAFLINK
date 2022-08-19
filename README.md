# HA Flink example

This self contained example runs apache flink jobs on kubernetes with the [new flink kubernetes operator](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-stable/). It runs kubernetes in HA mode with a kafka source and jdbc sink, enabling tests with both the HA mode as flink and the full exactly-once semantics of the kafka to jdbc pipeline.

# How to run
## Requirements
- 16 GB RAM
- k3s installed (follow [k3s helm setup](https://github.com/Kapernikov/skaffold-helm-tutorial/blob/main/chapters/01-install-k3s.md))
- k9s (optional, but very usefull)

## Installation
run
```
skaffold run -d <registry> -n <namespace>
```
with the registry being the one the docker images are stored in and the namespace the kubernetes namespace where the application will run.
You can check the progress of the installation by running:
```
k9s
```
and going into the specified namespace. These things should happen:

- the flink kubernetes operator should be created
- the postgres operator should be created
- the postgres clusters should be created
- a minio instance should be created
- the pyapp instances should apear and error because not all instances have been created yet (this will fix itself later on)
- the kafka zookeepers should be created
- the kafka clusters should be created
- the kafka cluster operator should be created
- the strimzi cluster operator should be created

The flink application itself should not be created just yet, it depends on the flink operator. There are some credentials that should be set to make sure the application does not create any errors. The login to the postgres database requires a username and a password, these an be created but a testuser can be found under secrets in k9s. Put these credentials in the flink app .env file under:
```
PG_USERNAME=
PG_PASSWORD=
```
The application can now acces the DB, but a table should be created to store al data in, the easyest way to do this is to use a tool like DBeaver. To connect to the DB a port forward needs to set inside the kubernetes cluster, Log into the tool with the aformentioned credentials and create a table. In this tbable create a column and add both table name and column name into the .env of the application as follows:
```
PG_TABLE=
PG_COLUMN=
```
Now the aplication can be deployed, just run
```
skaffold run -d <registry> -n <namespace>
```
again. For every update to the application this same command can be run to upgrade the deployment. Aditionaly, there now should be:

- the flink application instances
- the flink taskmanager instances

## checking functionality

The application will push data into a kafka topic and into the postgres DB, both can be checked.
### kafka output
To check the output to the kafka topic (default: vincent-output), open a shel on any of the kafka instances via k9s and type:
```
cd bin/
bash kafka-console-consumer.sh --topic vincent-output --bootstrap-server yalii-cluster-kafka-bootstrap:9092 --from-beginning --isolation-level read_committed
```
You will get the full output and it will live update on every commit.
### postgres output
The easyest way to check the postgres DB output is to use a tool like DBeaver. Log into the tool with the username and password set earlyer in the flink application and navigate to the created table.

### Flink operation
To use the visual tool for flink a port forward needs to be set up to the REST service for flink. Once this is setup just navigate to the setup port.
# TODO
 - alembic gebruiken voor postgres migratie naar nieuwere versie te streamlinen (extra)
 - presentatie maken (klant) ([done](https://docs.google.com/presentation/d/1j7TialmcedZ_3gS8Wd0wO4aqUCmGtcR9hBZp2Nxiac0/edit?usp=sharing))
 - pr op official micrelec repo
 - custom exactly once sink (extra)
