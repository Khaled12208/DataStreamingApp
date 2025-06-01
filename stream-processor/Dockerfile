FROM flink:1.18.1-java17

WORKDIR /opt/flink

COPY target/stream-processor-1.0.0-SNAPSHOT.jar /opt/flink/usrlib/stream-processor.jar
COPY src/main/resources/log4j2.xml /opt/flink/conf/log4j2.xml

# Submit the job to the Flink cluster
CMD ["flink", "run", "-m", "flink-jobmanager:8081", "-c", "com.example.streamprocessor.StreamProcessorJob", "/opt/flink/usrlib/stream-processor.jar"] 