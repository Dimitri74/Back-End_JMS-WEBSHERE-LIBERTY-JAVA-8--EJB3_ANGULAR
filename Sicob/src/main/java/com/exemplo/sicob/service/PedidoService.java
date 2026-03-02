package com.exemplo.sicob.service;

import com.exemplo.sicob.entity.Pedido;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class PedidoService {

    @PersistenceContext(unitName = "SicobPU")
    private EntityManager em;

    public Pedido criar(Pedido pedido) {
        em.persist(pedido);
        em.flush();
        return pedido;
    }

    public Pedido find(Long id) {
        return em.find(Pedido.class, id);
    }

    public List<Pedido> listAll() {
        return em.createQuery("SELECT p FROM Pedido p", Pedido.class).getResultList();
    }

    public Pedido atualizar(Long id, Pedido dadosAtualizados) {
        Pedido p = em.find(Pedido.class, id);
        if (p != null) {
            p.setCliente(dadosAtualizados.getCliente());
            p.setProduto(dadosAtualizados.getProduto());
            p.setQuantidade(dadosAtualizados.getQuantidade());
            p.setStatus(dadosAtualizados.getStatus());
            return em.merge(p);
        }
        return null;
    }

    public boolean deletar(Long id) {
        Pedido p = em.find(Pedido.class, id);
        if (p != null) {
            em.remove(p);
            return true;
        }
        return false;
    }

    public Pedido atualizarStatus(Long id, String status) {
        Pedido p = em.find(Pedido.class, id);
        if (p != null) {
            p.setStatus(status);
            em.merge(p);
        }
        return p;
    }
}
