package com.exemplo.sicob.jms;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.*;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Enumeration;
import java.util.logging.Logger;

@Stateless
@Path("/inspect-queues")
public class QueueInspectResource {

    private static final Logger LOGGER = Logger.getLogger(QueueInspectResource.class.getName());

    @Resource(lookup = "jms/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(lookup = "jms/queue/filaPedidos")
    private Queue filaPedidos;

    @Resource(lookup = "jms/queue/filaResposta")
    private Queue filaResposta;

    @GET
    @Path("/pedidos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response inspectPedidos() {
        List<Map<String, String>> msgs = browseQueue(filaPedidos);
        return Response.ok(msgs).build();
    }

    @GET
    @Path("/respostas")
    @Produces(MediaType.APPLICATION_JSON)
    public Response inspectRespostas() {
        List<Map<String, String>> msgs = browseQueue(filaResposta);
        return Response.ok(msgs).build();
    }

    private List<Map<String, String>> browseQueue(Queue queue) {
        List<Map<String, String>> result = new ArrayList<>();
        Connection conn = null;
        Session session = null;
        QueueBrowser browser = null;
        try {
            conn = connectionFactory.createConnection();
            session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            browser = session.createBrowser(queue);
            conn.start();
            Enumeration<?> e = browser.getEnumeration();
            while (e.hasMoreElements()) {
                Message m = (Message) e.nextElement();
                Map<String, String> info = new HashMap<>();
                try {
                    info.put("JMSMessageID", m.getJMSMessageID());
                    info.put("JMSCorrelationID", m.getJMSCorrelationID());
                    if (m instanceof TextMessage) {
                        info.put("Text", ((TextMessage) m).getText());
                    }
                } catch (JMSException ex) {
                    Map<String, String> err = new HashMap<>();
                    err.put("error", ex.getMessage());
                    result.add(err);
                }
                result.add(info);
            }
        } catch (Exception ex) {
            Map<String, String> err = new HashMap<>();
            err.put("error", ex.getMessage());
            result.add(err);
        } finally {
            try {
                if (browser != null) browser.close();
                if (session != null) session.close();
                if (conn != null) conn.close();
            } catch (JMSException ignored) {
            }
        }
        return result;
    }
}
