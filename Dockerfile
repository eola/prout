FROM ibm-semeru-runtimes:open-21-jdk
RUN apt update -y && apt install -y gnupg2
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN apt update -y && apt install -y sbt

# This Java image can replace all of the above, but uses much more RAM
# FROM sbtscala/scala-sbt:eclipse-temurin-21.0.5_11_1.10.7_2.13.16

COPY . /app
WORKDIR /app

RUN sbt compile
RUN sbt stage

CMD ["sbt", "run"]

# docker build -t prout .
# sbt/play/pekko always seems to be running on port 9000
# docker run --init -it -p 9000:9000 -e PROUT_GITHUB_ACCESS_TOKEN=test prout
