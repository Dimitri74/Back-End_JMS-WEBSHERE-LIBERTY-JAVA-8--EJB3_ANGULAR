package com.exemplo.sicob.jms;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class PedidoProducer {

    private static final Logger LOGGER = Logger.getLogger(PedidoProducer.class.getName());

    @Resource(lookup = "jms/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(lookup = "jms/queue/filaPedidos")
    private Queue filaPedidos;

    @Resource(lookup = "jms/queue/filaResposta")
    private Queue filaResposta;

    public String enviarPedidoId(Long pedidoId) {
        String correlationId = "PEDIDO-" + pedidoId + "-" + System.currentTimeMillis();

        LOGGER.info("[PRODUCER] Preparando para enviar pedido ID: " + pedidoId);
        System.out.println("[PRODUCER] Preparando para enviar pedido ID: " + pedidoId);

        Connection conn = null;
        Session session = null;
        try {
            if (connectionFactory == null) {
                LOGGER.severe("[PRODUCER] connectionFactory está NULO!");
                System.err.println("[PRODUCER] connectionFactory está NULO!");
                throw new RuntimeException("ConnectionFactory não injetada");
            }
            if (filaPedidos == null) {
                LOGGER.severe("[PRODUCER] filaPedidos está NULA!");
                System.err.println("[PRODUCER] filaPedidos está NULA!");
                throw new RuntimeException("Queue filaPedidos não injetada");
            }
            
            conn = connectionFactory.createConnection();
            session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(filaPedidos);

            TextMessage msg = session.createTextMessage(String.valueOf(pedidoId));

            // Adicionar ReplyTo para permitir que o MDB responda
            msg.setJMSReplyTo(filaResposta);

            // Adicionar correlationID para rastreamento
            msg.setJMSCorrelationID(correlationId);

            LOGGER.info("[PRODUCER] Enviando mensagem com correlationID: " + correlationId);
            System.out.println("[PRODUCER] Enviando mensagem com correlationID: " + correlationId);

            producer.send(msg);

            // Log do JMSMessageID atribuído pelo broker
            try {
                String messageId = msg.getJMSMessageID();
                LOGGER.info("[PRODUCER] Mensagem enviada com JMSMessageID: " + messageId + " e correlationID: " + correlationId);
                System.out.println("[PRODUCER] Mensagem enviada com JMSMessageID: " + messageId + " e correlationID: " + correlationId);
            } catch (JMSException je) {
                LOGGER.log(Level.WARNING, "[PRODUCER] Não foi possível obter JMSMessageID", je);
            }

            LOGGER.info("[PRODUCER] Mensagem enviada com sucesso para a fila.");
            System.out.println("[PRODUCER] Mensagem enviada com sucesso para a fila.");

            return correlationId;

        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "[PRODUCER] Erro ao enviar mensagem JMS", e);
            System.err.println("[PRODUCER] Erro ao enviar mensagem JMS: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao enviar mensagem JMS", e);
        } finally {
            try {
                if (session != null) session.close();
                if (conn != null) conn.close();
            } catch (JMSException ignore) {
                LOGGER.log(Level.WARNING, "[PRODUCER] Erro ao fechar conexão", ignore);
            }
        }
    }
}
