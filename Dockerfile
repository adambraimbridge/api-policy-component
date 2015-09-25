FROM up-registry.ft.com/coco/dropwizardbase
ADD .git/ /.git/
ADD api-policy-component-service/ /api-policy-component-service/
ADD pom.xml /
RUN apk --update add git \ 
  && cd api-policy-component-service \
  && HASH=$(git log -1 --pretty=format:%H) \
  && mvn clean install -Dbuild.git.revision=$HASH -Djava.net.preferIPv4Stack=true \
  && JARNAME=target/api-policy-component-service-1.0-SNAPSHOT.jar \
  && mv $JARNAME /app.jar \
  && mv config-local.yml /config.yaml \
  && apk del git \
  && rm -rf /var/cache/apk/* \
  && rm -rf /root/.m2/*

EXPOSE 8080 8081



CMD java -Ddw.server.applicationConnectors[0].port=8080 -Ddw.server.adminConnectors[0].port=8081 -jar app.jar server config.yaml

