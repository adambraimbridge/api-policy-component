#!/usr/bin/env bash

#POD_NAME=$(kubectl get pods | grep path-routing-varnish | tail -1 | awk '{print $1}')

# kbc port-forward path-routing-varnish-56bbb56b9c-tb2zg 8003:80

export JAVA_OPTS="-Xms450m -Xmx450m -XX:+UseG1GC -server"
export CHECKING_VULCAN_HEALTH="false"
export JERSEY_TIMEOUT_DURATION="100000ms"
#export READ_ENDPOINT="path-routing-varnish:80"
export READ_ENDPOINT="localhost:8003"

java $JAVA_OPTS \
  -Ddw.server.applicationConnectors[0].port=8080 \
  -Ddw.server.adminConnectors[0].port=8081 \
  -Dsun.net.http.allowRestrictedHeaders=true \
  -Ddw.varnish.primaryNodes=$READ_ENDPOINT \
  -Ddw.varnish.timeout=$JERSEY_TIMEOUT_DURATION \
  -Ddw.checkingVulcanHealth=$CHECKING_VULCAN_HEALTH \
  -Ddw.logging.appenders[0].logFormat="%-5p [%d{ISO8601, GMT}] %c: %X{transaction_id} %replace(%m%n[%thread]%xEx){'\n', '|'}%nopex%n" \
  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5009 \
  -jar ./api-policy-component-service/target/api-policy-component-service-1.0-SNAPSHOT.jar server ./api-policy-component-service/config-local.yml
