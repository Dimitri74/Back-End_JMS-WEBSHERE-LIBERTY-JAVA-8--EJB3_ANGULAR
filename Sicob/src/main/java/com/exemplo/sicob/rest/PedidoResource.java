package com.exemplo.sicob.rest;

import com.exemplo.sicob.entity.Pedido;
import com.exemplo.sicob.jms.PedidoProducer;
import com.exemplo.sicob.jms.RespostaConsumer;
import com.exemplo.sicob.service.PedidoService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.logging.Logger;

@Path("/pedidos")
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PedidoResource {

    private static final Logger LOGGER = Logger.getLogger(PedidoResource.class.getName());

    @Inject
    private PedidoService pedidoService;

    @Inject
    private PedidoProducer pedidoProducer;

    @Inject
    private RespostaConsumer respostaConsumer;

    @POST
    public Response criarPedido(Pedido pedido) {
        if (pedido.getCliente() == null || pedido.getProduto() == null || pedido.getQuantidade() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Campos obrigatórios ausentes").build();
        }

        LOGGER.info("[REST] Criando novo pedido...");
        System.out.println("[REST] Criando novo pedido...");

        Pedido criado = pedidoService.criar(pedido);
        LOGGER.info("[REST] Pedido criado com ID: " + criado.getId());
        System.out.println("[REST] Pedido criado com ID: " + criado.getId());

        // Enviar para fila e aguardar resposta
        String correlationId = pedidoProducer.enviarPedidoId(criado.getId());

        // Aguardar processamento do MDB (timeout de 30 segundos)
        LOGGER.info("[REST] Aguardando resposta do MDB...");
        System.out.println("[REST] Aguardando resposta do MDB...");

        RespostaConsumer.RespostaMessage resposta = respostaConsumer.aguardarResposta(correlationId, 30);

        if (resposta != null) {
            LOGGER.info("[REST] Resposta recebida: " + resposta.status);
            System.out.println("[REST] Resposta recebida: " + resposta.status);

            // Recarregar o pedido para pegar o status atualizado
            Pedido pedidoAtualizado = pedidoService.find(criado.getId());
            return Response.status(Response.Status.CREATED)
                    .entity(pedidoAtualizado)
                    .header("X-MDB-Status", resposta.status)
                    .build();
        } else {
            LOGGER.warning("[REST] Timeout aguardando resposta do MDB");
            System.err.println("[REST] Timeout aguardando resposta do MDB");

            // Retornar o pedido mesmo que não tenha recebido resposta
            // (ele está processando em background)
            return Response.status(Response.Status.CREATED)
                    .entity(criado)
                    .header("X-Warning", "Pedido criado mas processamento em background")
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getPedido(@PathParam("id") Long id) {
        if (pedidoService == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("PedidoService não injetado").build();
        }
        Pedido p = pedidoService.find(id);
        if (p == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(p).build();
    }

    @GET
    public Response listarTodos() {
        if (pedidoService == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("PedidoService não injetado").build();
        }
        return Response.ok(pedidoService.listAll()).build();
    }

    @PUT
    @Path("/{id}")
    public Response atualizarPedido(@PathParam("id") Long id, Pedido pedido) {
        Pedido atualizado = pedidoService.atualizar(id, pedido);
        if (atualizado == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(atualizado).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deletarPedido(@PathParam("id") Long id) {
        boolean deletado = pedidoService.deletar(id);
        if (!deletado) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
