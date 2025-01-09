FROM eclipse-temurin:8-jre-alpine
LABEL authors="Jan Schwalbe - jan.schwalbe@martaflex.de, Julius Bischoff - julius.bischoff@martaflex.de"
LABEL org.opencontainers.image.source https://github.com/maritaflex/nano-pdf

RUN apk add --no-cache msttcorefonts-installer fontconfig font-liberation  font-dejavu 
RUN fc-cache -f -v

ADD build/distributions/nano-pdf-0.1.0.tar /
ENTRYPOINT ["/nano-pdf-0.1.0/bin/nano-pdf"]
EXPOSE 9091
