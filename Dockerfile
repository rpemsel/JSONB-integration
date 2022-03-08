FROM openjdk:17

WORKDIR /

COPY jsonb-integration.jar jsonb-integration.jar

CMD ["java", "-jar", "jsonb-integration.jar"]