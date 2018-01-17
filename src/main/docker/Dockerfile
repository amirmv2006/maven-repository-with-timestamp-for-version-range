FROM openjdk:8-jre-alpine

ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    JHIPSTER_SLEEP=0 \
    JAVA_OPTS="" \
    artifactory_base_url= \
    artf_checkCreateDate=false \
    jenkins_master_url= \
    apps_user_name= \
    apps_user_password= \
    initialBuildCacheCount=10 \
    git_local_dir=/tmp

ADD *.jar /app.jar


EXPOSE 8080
CMD echo "The application will start in ${JHIPSTER_SLEEP}s..." && \
    sleep ${JHIPSTER_SLEEP} && \
    java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app.jar
