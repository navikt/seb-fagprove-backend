FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21

WORKDIR /app

COPY build/libs/app.jar ./app.jar

EXPOSE 8080
ENV env=dev

ENTRYPOINT ["java", "-jar", "app.jar"]
