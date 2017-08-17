FROM coco/dropwizardbase:0.7.x-mvn333
ADD .git/ /.git/
ADD api-policy-component-service/ /api-policy-component-service/
ADD pom.xml /
RUN apk --update add git \
  && cd api-policy-component-service \
  && HASH=$(git log -1 --pretty=format:%H) \
  && mvn clean install -Dbuild.git.revision=$HASH -Djava.net.preferIPv4Stack=true \
  && rm target/api-policy-component-service-*-sources.jar \
  && mv target/api-policy-component-service-*.jar /api-policy-component-service.jar \
  && mv config-local.yml /config.yml \
  && apk del git \
  && rm -rf /var/cache/apk/* \
  && rm -rf /root/.m2/*

EXPOSE 8080 8081

CMD exec java $JAVA_OPTS \
         -Ddw.server.applicationConnectors[0].port=8080 \
         -Ddw.server.adminConnectors[0].port=8081 \
         -Ddw.varnish.endpointConfiguration.primaryNodes=$READ_ENDPOINT \
         -Ddw.varnish.endpointConfiguration.jerseyClient.timeout=$JERSEY_TIMEOUT_DURATION \
         -Ddw.checkingVulcanHealth=$CHECKING_VULCAN_HEALTH \
         -Ddw.metrics.reporters[0].host=$GRAPHITE_HOST \
         -Ddw.metrics.reporters[0].port=$GRAPHITE_PORT \
         -Ddw.metrics.reporters[0].prefix=$GRAPHITE_PREFIX \
         -jar api-policy-component-service.jar server config.yml
