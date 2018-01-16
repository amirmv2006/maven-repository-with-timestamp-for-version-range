FROM openjdk:8-jre-alpine

ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    JHIPSTER_SLEEP=0 \
    JAVA_OPTS=""

ADD *.jar /app.jar
ENV artifactory_base_url=http://artifactory/artifactory
ENV artf_checkCreateDate=false
ENV jenkins_master_url=http://jenkins/job/jobname/job/master
ENV apps_user_name=username
ENV apps_user_password=pass
ENV initialBuildCacheCount=10
ENV git_local_dir=/tmp

EXPOSE 8080
CMD echo "The application will start in ${JHIPSTER_SLEEP}s..." && \
    sleep ${JHIPSTER_SLEEP} && \
    java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app.jar
