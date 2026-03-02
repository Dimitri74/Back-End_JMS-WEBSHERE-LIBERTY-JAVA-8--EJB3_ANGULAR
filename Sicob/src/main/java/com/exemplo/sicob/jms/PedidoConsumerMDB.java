package com.exemplo.sicob.jms;

import com.exemplo.sicob.entity.Pedido;
import com.exemplo.sicob.service.PedidoService;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/queue/filaPedidos"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "maxPoolSize", propertyValue = "10")
})
public class PedidoConsumerMDB implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(PedidoConsumerMDB.class.getName());

    @EJB
    private PedidoService pedidoService;

    @Resource(lookup = "jms/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public void onMessage(Message message) {
        LOGGER.info("[MDB_CONSUMER] ===== MENSAGEM RECEBIDA NO MDB! =====");
        System.out.println("[MDB_CONSUMER] ===== MENSAGEM RECEBIDA NO MDB! =====");

        Connection conn = null;
        Session session = null;

        try {
            if (message instanceof TextMessage) {
                TextMessage textMsg = (TextMessage) message;
                String text = textMsg.getText();
                String correlationId = textMsg.getJMSCorrelationID();
                Destination replyTo = textMsg.getJMSReplyTo();

                LOGGER.info("[MDB_CONSUMER] ID recebido: " + text);
                LOGGER.info("[MDB_CONSUMER] CorrelationID: " + correlationId);
                LOGGER.info("[MDB_CONSUMER] ReplyTo: " + (replyTo != null ? replyTo.toString() : "NÃO DEFINIDO"));

                System.out.println("[MDB_CONSUMER] ID recebido: " + text);
                System.out.println("[MDB_CONSUMER] CorrelationID: " + correlationId);
                System.out.println("[MDB_CONSUMER] ReplyTo: " + (replyTo != null ? replyTo.toString() : "NÃO DEFINIDO"));

                try {
                    Long pedidoId = Long.valueOf(text);

                    // Processar o pedido
                    Pedido p = pedidoService.atualizarStatus(pedidoId, "PROCESSADO");

                    if (p != null) {
                        LOGGER.info("[MDB_CONSUMER] Pedido processado com sucesso: " + pedidoId + ", status: " + p.getStatus());
                        System.out.println("[MDB_CONSUMER] Pedido processado com sucesso: " + pedidoId + ", status: " + p.getStatus());

                        // Enviar resposta se ReplyTo foi definido
                        if (replyTo != null) {
                            enviarResposta(replyTo, "SUCESSO", correlationId, pedidoId, p.getStatus());
                        }
                    } else {
                        LOGGER.warning("[MDB_CONSUMER] Pedido não encontrado: " + pedidoId);
                        System.err.println("[MDB_CONSUMER] Pedido não encontrado: " + pedidoId);

                        if (replyTo != null) {
                            enviarResposta(replyTo, "ERRO", correlationId, pedidoId, "Pedido não encontrado");
                        }
                    }
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.SEVERE, "[MDB_CONSUMER] Erro ao converter ID: " + text, e);
                    System.err.println("[MDB_CONSUMER] Erro ao converter ID: " + text);

                    if (replyTo != null) {
                        enviarResposta(replyTo, "ERRO", correlationId, null, "Erro ao converter ID");
                    }
                    throw e;
                }
            } else {
                LOGGER.warning("[MDB_CONSUMER] Mensagem JMS inesperada: tipo=" + message.getClass().getName());
                System.err.println("[MDB_CONSUMER] Mensagem JMS inesperada: tipo=" + message.getClass().getName());
            }
        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "[MDB_CONSUMER] Erro JMS ao processar mensagem", e);
            System.err.println("[MDB_CONSUMER] Erro JMS ao processar mensagem: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[MDB_CONSUMER] Erro inesperado no processamento do MDB", e);
            System.err.println("[MDB_CONSUMER] Erro inesperado no processamento do MDB: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void enviarResposta(Destination replyTo, String status, String correlationId, Long pedidoId, String mensagem) {
        LOGGER.info("[MDB_CONSUMER] Preparando envio de resposta...");
        System.out.println("[MDB_CONSUMER] Preparando envio de resposta...");

        Connection conn = null;
        Session session = null;
        try {
            if (connectionFactory == null) {
                LOGGER.warning("[MDB_CONSUMER] ConnectionFactory não disponível para enviar resposta");
                System.err.println("[MDB_CONSUMER] ConnectionFactory não disponível para enviar resposta");
                return;
            }

            conn = connectionFactory.createConnection();
            session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(replyTo);

            TextMessage resposta = session.createTextMessage();
            resposta.setStringProperty("status", status);
            resposta.setStringProperty("mensagem", mensagem);
            resposta.setLongProperty("pedidoId", pedidoId != null ? pedidoId : -1L);
            resposta.setJMSCorrelationID(correlationId);
            resposta.setText("Processamento do pedido " + pedidoId + " finalizado com status: " + status);

            producer.send(resposta);

            // Ler e logar o JMSMessageID atribuído ao envio
            try {
                String sentMessageId = resposta.getJMSMessageID();
                LOGGER.info("[MDB_CONSUMER] Resposta enviada com JMSMessageID: " + sentMessageId + " e correlationID: " + correlationId);
                System.out.println("[MDB_CONSUMER] Resposta enviada com JMSMessageID: " + sentMessageId + " e correlationID: " + correlationId);
            } catch (JMSException jme) {
                LOGGER.log(Level.WARNING, "[MDB_CONSUMER] Não foi possível ler JMSMessageID após envio", jme);
            }

            LOGGER.info("[MDB_CONSUMER] Resposta enviada com sucesso!");
            System.out.println("[MDB_CONSUMER] Resposta enviada com sucesso!");

        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "[MDB_CONSUMER] Erro ao enviar resposta", e);
            System.err.println("[MDB_CONSUMER] Erro ao enviar resposta: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (session != null) session.close();
                if (conn != null) conn.close();
            } catch (JMSException ignore) {
                LOGGER.log(Level.WARNING, "[MDB_CONSUMER] Erro ao fechar conexão de resposta", ignore);
            }
        }
    }
}
