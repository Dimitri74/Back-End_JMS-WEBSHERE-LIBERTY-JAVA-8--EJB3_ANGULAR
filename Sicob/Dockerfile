# Estágio final usando a imagem oficial do WebSphere Liberty
FROM icr.io/appcafe/websphere-liberty:kernel-java8-openj9-ubi

# Copia a configuração do servidor
COPY --chown=1001:0 src/main/liberty/config/server.xml /config/

# Baixa o driver MySQL para o diretório de recursos compartilhados do Liberty
USER root
RUN curl -L https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar -o /opt/ibm/wlp/usr/shared/resources/mysql-connector-j-8.0.33.jar
USER 1001

# Copia o artefato WAR já compilado localmente
COPY --chown=1001:0 target/pedidos-backend-1.0-SNAPSHOT.war /config/apps/

# Roda o script de instalação de features
RUN features.sh

EXPOSE 9080 9443
