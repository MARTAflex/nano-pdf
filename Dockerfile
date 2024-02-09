FROM eclipse-temurin:8-jre-alpine
LABEL authors="Jan Schwalbe - jan.schwalbe@martaflex.de, Julius Bischoff - julius.bischoff@martaflex.de"

RUN apk add --no-cache msttcorefonts-installer fontconfig font-liberation  font-dejavu font-noto font-noto-cjk font-awesome font-noto-extra
RUN fc-cache -f -v

ADD build/distributions/nano-pdf.tar /
ENTRYPOINT ["/nano-pdf/bin/nano-pdf"]
EXPOSE 9091