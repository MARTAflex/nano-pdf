FROM eclipse-temurin:8-jre-alpine
LABEL authors="Jan Schwalbe - jan.schwalbe@martaflex.de, Julius Bischoff - julius.bischoff@martaflex.de"
LABEL org.opencontainers.image.source=https://github.com/martaflex/nano-pdf

RUN apk add --no-cache msttcorefonts-installer fontconfig font-liberation  font-dejavu 
RUN fc-cache -f -v

ADD build/distributions/nano-pdf-0.1.2.tar /
ENTRYPOINT ["/nano-pdf-0.1.2/bin/nano-pdf"]
EXPOSE 9091
