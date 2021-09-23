FROM gradle:7-jre11 AS app-build

# Go into the directory where we can run 'gradle' command
WORKDIR /home/gradle/src

# We can copy everything without worrying about trash, this image is temporary anyway
COPY . .

RUN gradle build --no-daemon


FROM eclipse-temurin:17-focal AS jre-build

WORKDIR /usr/src/setup

# Get only final fat jar from gradle step
COPY --from=app-build /home/gradle/src/build/libs/mbot.jar .

# Build JRE with only necessary modules
RUN $JAVA_HOME/bin/jlink \
    --add-modules $($JAVA_HOME/bin/jdeps --list-deps --ignore-missing-deps /usr/src/setup/mbot.jar | xargs | tr ' ' ',') \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /javaruntime


FROM debian:11-slim

WORKDIR /usr/src/app

# Get JRE from last step and setup env vars for it
ENV JAVA_HOME=/usr/src/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME

COPY --from=jre-build /usr/src/setup/mbot.jar .

CMD ["java", "-jar", "mbot.jar"]
