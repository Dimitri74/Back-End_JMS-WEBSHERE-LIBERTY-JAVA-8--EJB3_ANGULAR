import { Component } from '@angular/core';
import { PedidoService } from './pedido.service';

@Component({
  selector: 'app-pedido-form',
  templateUrl: './pedido-form.component.html',
  styleUrls: ['./pedido-form.component.css']
})
export class PedidoFormComponent {
  cliente = '';
  produto = '';
  quantidade: number | null = null;
  loading = false;
  message: { type: 'success' | 'error' | 'info', text: string } | null = null;

  constructor(private pedidoService: PedidoService) {}

  criarPedido() {
    if (!this.cliente || !this.produto || !this.quantidade || this.quantidade <= 0) {
      this.message = { type: 'error', text: 'Validação: preencha todos os campos corretamente.' };
      return;
    }
    this.loading = true;
    this.message = null;
    const payload = { cliente: this.cliente, produto: this.produto, quantidade: this.quantidade };
    this.pedidoService.createPedido(payload).subscribe({
      next: (res: any) => {
        this.loading = false;
        this.message = { type: 'success', text: `Pedido #${res.id || '—'} criado! Status: ${res.status || 'PENDENTE'}` };
        this.cliente = ''; this.produto = ''; this.quantidade = null;
      },
      error: (err) => {
        this.loading = false;
        const detail = err?.error?.message || err?.message || 'Erro inesperado';
        this.message = { type: 'error', text: `Erro ao criar pedido: ${detail}` };
      }
    });
  }
}
