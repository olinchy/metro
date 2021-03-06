#### create module based on Beryllium
```
mvn archetype:generate -DarchetypeGroupId=org.opendaylight.controller -DarchetypeArtifactId=opendaylight-startup-archetype -DarchetypeVersion=1.1.4-Beryllium-SR4
```
--- on Boron
```
mvn archetype:generate -DarchetypeGroupId=org.opendaylight.controller -DarchetypeArtifactId=opendaylight-startup-archetype -DarchetypeVersion=1.2.1-Boron-SR1
```

#### deploy feature
```
export ODL_KARAF_HOME=~/Downloads/ipsdn-all-karaf-2.2.7.R3B07
mkdir -p $ODL_KARAF_HOME/system/com/zte/sdn/mw/e2e

cp -R ~/.m2/repository/com/zte/ngip/ipsdn/ $ODL_KARAF_HOME/system/com/zte/ngip

cp -R ~/.m2/repository/com/zte/sdn/mw $ODL_KARAF_HOME/system/com/zte/sdn
```
--- start odl
```
cd $ODL_KARAF_HOME
./bin/karaf
```
--- install feature
```
feature:repo-add mvn:com.zte.sdn.mw.e2e/metro-features/1.0.0-SNAPSHOT/xml/features
feature:install mw-metro
```
--- update feature
```
feature:uninstall mw-metro

feature:repo-refresh mvn:com.zte.sdn.mw.e2e/metro-features/1.0.0-SNAPSHOT/xml/features
feature:install mw-metro

```
