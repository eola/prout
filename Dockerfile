# FROM graalvm-community-21.0.2_1.10.2_2.13.15
FROM sbtscala/scala-sbt:graalvm-community-21.0.2_1.10.2_2.13.15

COPY . /app
WORKDIR /app

RUN sbt compile
RUN sbt stage

CMD ["sbt", "run"]

# docker build -t prout .
# sbt/play/pekko always seems to be running on port 9000
# docker run -p 9000:9000 -e PORT=9000 prout
