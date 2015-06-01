FROM coco/dropwizardbase

RUN git clone http://git.svc.ft.com:8080/scm/cp/api-policy-component.git
RUN cd api-policy-component && mvn install

RUN cp /api-policy-component/api-policy-component-service/target/api-policy-component-service-1.0-SNAPSHOT.jar /app.jar
RUN cp /api-policy-component/api-policy-component-service/config-local.yml /config.yaml

CMD java -Ddw.varnish.primaryNodes=$READ_ENDPOINT -Ddw.server.applicationConnectors[0].port=8080 -Ddw.server.adminConnectors[0].port=8081 -jar app.jar server config.yaml
