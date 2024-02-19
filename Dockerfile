FROM eclipse-temurin:8-jre-alpine
LABEL authors="Jan Schwalbe - jan.schwalbe@martaflex.de, Julius Bischoff - julius.bischoff@martaflex.de"

RUN apk add --no-cache msttcorefonts-installer fontconfig font-liberation  font-dejavu font-noto font-noto-cjk font-awesome font-noto-extra
RUN fc-cache -f -v

ADD build/distributions/nanoPdf-0.0.7.tar /
ENTRYPOINT ["/nanoPdf-0.0.7/bin/nanoPdf"]
EXPOSE 9091