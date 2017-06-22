FROM openjdk:8-jre-alpine
MAINTAINER Jan Schwalbe "jan.schwalbe@martaflex.de"
ADD build/distributions/nano-pdf.tar /
ENTRYPOINT ["/nano-pdf/bin/nano-pdf"]
EXPOSE 9091


