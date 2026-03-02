package com.exemplo.sicob.jms;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.jms.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consumer singleton que monitora a fila de resposta do MDB.
 * Permite que o cliente aguarde pela resposta do processamento do pedido.
 */
@Singleton
@Startup
public class RespostaConsumer {

    private static final Logger LOGGER = Logger.getLogger(RespostaConsumer.class.getName());

    @Resource(lookup = "jms/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(lookup = "jms/queue/filaResposta")
    private Queue filaResposta;

    private Connection connection;
    private Session session;
    private MessageConsumer consumer;
    private Thread listenerThread;

    // Cache de respostas: correlationID -> RespostaMessage
    private final ConcurrentHashMap<String, RespostaMessage> respostas = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        LOGGER.info("[RESPOSTA_CONSUMER] Inicializando consumer de resposta...");
        System.out.println("[RESPOSTA_CONSUMER] Inicializando consumer de resposta...");

        try {
            if (connectionFactory == null || filaResposta == null) {
                LOGGER.severe("[RESPOSTA_CONSUMER] ConnectionFactory ou fila de resposta não injetadas!");
                System.err.println("[RESPOSTA_CONSUMER] ConnectionFactory ou fila de resposta não injetadas!");
                return;
            }

            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            consumer = session.createConsumer(filaResposta);

            // Iniciar thread de escuta
            listenerThread = new Thread(() -> {
                LOGGER.info("[RESPOSTA_CONSUMER] Thread de escuta iniciada");
                System.out.println("[RESPOSTA_CONSUMER] Thread de escuta iniciada");

                try {
                    connection.start();
                    Message msg;
                    while ((msg = consumer.receive()) != null) {
                        processarResposta(msg);
                    }
                } catch (JMSException e) {
                    LOGGER.log(Level.WARNING, "[RESPOSTA_CONSUMER] Conexão encerrada", e);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "[RESPOSTA_CONSUMER] Erro na thread de escuta", e);
                    System.err.println("[RESPOSTA_CONSUMER] Erro na thread de escuta: " + e.getMessage());
                }
            }, "RespostaConsumer-Thread");

            listenerThread.setDaemon(true);
            listenerThread.start();

            LOGGER.info("[RESPOSTA_CONSUMER] Consumer de resposta inicializado com sucesso");
            System.out.println("[RESPOSTA_CONSUMER] Consumer de resposta inicializado com sucesso");

        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "[RESPOSTA_CONSUMER] Falha ao inicializar consumer de resposta", e);
            System.err.println("[RESPOSTA_CONSUMER] Falha ao inicializar: " + e.getMessage());
        }
    }

    private void processarResposta(Message msg) {
        try {
            if (msg instanceof TextMessage) {
                TextMessage textMsg = (TextMessage) msg;
                String correlationId = textMsg.getJMSCorrelationID();
                String status = textMsg.getStringProperty("status");
                String mensagem = textMsg.getStringProperty("mensagem");
                Long pedidoId = textMsg.getLongProperty("pedidoId");
                String messageId = null;
                try {
                    messageId = textMsg.getJMSMessageID();
                } catch (JMSException jme) {
                    LOGGER.log(Level.WARNING, "[RESPOSTA_CONSUMER] Não foi possível ler JMSMessageID", jme);
                }

                LOGGER.info("[RESPOSTA_CONSUMER] Resposta recebida - JMSMessageID: " + messageId + " CorrelationID: " + correlationId + ", Status: " + status);
                System.out.println("[RESPOSTA_CONSUMER] Resposta recebida - JMSMessageID: " + messageId + " CorrelationID: " + correlationId + ", Status: " + status);

                RespostaMessage resposta = new RespostaMessage(status, mensagem, pedidoId, textMsg.getText());
                respostas.put(correlationId, resposta);

                LOGGER.info("[RESPOSTA_CONSUMER] Resposta armazenada no cache");
                System.out.println("[RESPOSTA_CONSUMER] Resposta armazenada no cache");
            }
        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "[RESPOSTA_CONSUMER] Erro ao processar resposta", e);
            System.err.println("[RESPOSTA_CONSUMER] Erro ao processar resposta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Aguarda pela resposta de um pedido processado.
     * @param correlationId ID de correlação da mensagem
     * @param timeoutSegundos Tempo máximo de espera em segundos
     * @return RespostaMessage ou null se timeout
     */
    public RespostaMessage aguardarResposta(String correlationId, int timeoutSegundos) {
        LOGGER.info("[RESPOSTA_CONSUMER] Aguardando resposta para: " + correlationId + " (timeout: " + timeoutSegundos + "s)");
        System.out.println("[RESPOSTA_CONSUMER] Aguardando resposta para: " + correlationId + " (timeout: " + timeoutSegundos + "s)");

        long inicioWait = System.currentTimeMillis();
        long timeoutMs = timeoutSegundos * 1000L;

        while (true) {
            RespostaMessage resposta = respostas.get(correlationId);
            if (resposta != null) {
                respostas.remove(correlationId); // Limpar do cache após recuperar
                LOGGER.info("[RESPOSTA_CONSUMER] Resposta encontrada!");
                System.out.println("[RESPOSTA_CONSUMER] Resposta encontrada!");
                return resposta;
            }

            long tempoDecorrido = System.currentTimeMillis() - inicioWait;
            if (tempoDecorrido >= timeoutMs) {
                break;
            }

            try {
                long tempoEspera = Math.min(100, timeoutMs - tempoDecorrido);
                Thread.sleep(tempoEspera);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warning("[RESPOSTA_CONSUMER] Aguarda interrompido");
                System.err.println("[RESPOSTA_CONSUMER] Aguarda interrompido");
                break;
            }
        }

        LOGGER.warning("[RESPOSTA_CONSUMER] Timeout aguardando resposta para: " + correlationId);
        System.err.println("[RESPOSTA_CONSUMER] TIMEOUT aguardando resposta para: " + correlationId);
        return null;
    }

    @PreDestroy
    public void cleanup() {
        LOGGER.info("[RESPOSTA_CONSUMER] Desligando consumer de resposta...");
        System.out.println("[RESPOSTA_CONSUMER] Desligando consumer de resposta...");

        try {
            if (consumer != null) consumer.close();
            if (session != null) session.close();
            if (connection != null) connection.close();
        } catch (JMSException e) {
            LOGGER.log(Level.WARNING, "[RESPOSTA_CONSUMER] Erro ao desligar", e);
        }

        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    /**
     * Classe interna para armazenar dados de resposta
     */
    public static class RespostaMessage {
        public final String status;
        public final String mensagem;
        public final Long pedidoId;
        public final String texto;

        public RespostaMessage(String status, String mensagem, Long pedidoId, String texto) {
            this.status = status;
            this.mensagem = mensagem;
            this.pedidoId = pedidoId;
            this.texto = texto;
        }

        @Override
        public String toString() {
            return "RespostaMessage{" +
                    "status='" + status + '\'' +
                    ", mensagem='" + mensagem + '\'' +
                    ", pedidoId=" + pedidoId +
                    ", texto='" + texto + '\'' +
                    '}';
        }
    }
}

