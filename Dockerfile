FROM openjdk:17

WORKDIR /

COPY jsonb-integration-1.0.0-SNAPSHOT.jar jsonb-integration.jar

CMD ["java", "-jar", "jsonb-integration.jar"]