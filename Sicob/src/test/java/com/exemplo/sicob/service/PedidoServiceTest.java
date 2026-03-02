package com.exemplo.sicob.service;

import com.exemplo.sicob.entity.Pedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PedidoServiceTest {

    private PedidoService pedidoService;
    private EntityManager em;

    @BeforeEach
    public void setup() {
        pedidoService = new PedidoService();
        em = mock(EntityManager.class);
        // inject via reflection (since field is private)
        try {
            java.lang.reflect.Field f = PedidoService.class.getDeclaredField("em");
            f.setAccessible(true);
            f.set(pedidoService, em);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCriarPedido() {
        Pedido p = new Pedido();
        p.setCliente("Cliente A");
        p.setProduto("Produto X");
        p.setQuantidade(2);

        // call
        Pedido criado = pedidoService.criar(p);

        // verify persist called
        verify(em, times(1)).persist(p);
        verify(em, times(1)).flush();
        assertEquals(p, criado);
    }
}
