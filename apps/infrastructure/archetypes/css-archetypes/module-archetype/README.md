#### Generate archetype
```
mvn clean install
```

#### create module with it
```
mvn archetype:generate -DarchetypeGroupId=com.zte.sdn.mw \
-DarchetypeArtifactId=module-archetype -DarchetypeCatalog=local \
-DarchetypeVersion=1.0.0-SNAPSHOT

```