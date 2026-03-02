import { Component, OnDestroy, OnInit } from '@angular/core';
import { Pedido, PedidoService } from './pedido.service';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-pedido-list',
  templateUrl: './pedido-list.component.html',
  styleUrls: ['./pedido-list.component.css']
})
export class PedidoListComponent implements OnInit, OnDestroy {
  pedidos: Pedido[] = [];
  loading = false;
  refreshSub: Subscription | null = null;

  constructor(private pedidoService: PedidoService) {}

  ngOnInit() {
    this.load();
    this.refreshSub = interval(7000).subscribe(() => this.load());
  }

  ngOnDestroy() {
    this.refreshSub?.unsubscribe();
  }

  load() {
    this.loading = true;
    this.pedidoService.getPedidos().subscribe({
      next: (res: Pedido[]) => { this.pedidos = res || []; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
}
