FROM up-registry.ft.com/coco/dropwizardbase

RUN git clone http://git.svc.ft.com/scm/cp/api-policy-component.git 
RUN cd api-policy-component && mvn install

RUN cp /api-policy-component/target/api-policy-component*.jar /app.jar
RUN cp /api-policy-component/api-policy-component-service/config-local.yaml  /config.yaml

CMD java -Ddw.server.applicationConnectors[0].port=8080 -Ddw.server.adminConnectors[0].port=8081 -jar app.jar server config.yaml

