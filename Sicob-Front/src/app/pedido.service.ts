import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../environments/environment';

/** modelo simples para remover o uso de `any` */
export interface Pedido {
  id?: number;
  cliente: string;
  produto: string;
  quantidade: number;
  status?: string;
  criadoEm?: string | Date | any;
  data?: string;
}

@Injectable({ providedIn: 'root' })
export class PedidoService {
  private base = `${environment.apiUrl}/pedidos`;

  constructor(private http: HttpClient) { }

  getPedidos(): Observable<Pedido[]> {
    return this.http.get<any>(this.base).pipe(
      map((res: any) => this.extractPedidos(res)),
      map((pedidos: any[]) => pedidos.map((pedido) => this.normalizePedido(pedido)))
    );
  }

  createPedido(payload: Omit<Pedido, 'id'|'status'|'data'>): Observable<Pedido> {
    return this.http.post<Pedido>(this.base, payload);
  }

  getPedido(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`${this.base}/${id}`);
  }

  private extractPedidos(res: any): any[] {
    if (Array.isArray(res)) {
      return res;
    }

    if (Array.isArray(res?.content)) {
      return res.content;
    }

    if (Array.isArray(res?.data)) {
      return res.data;
    }

    if (Array.isArray(res?.items)) {
      return res.items;
    }

    if (Array.isArray(res?.value)) {
      return res.value;
    }

    if (res?._embedded && typeof res._embedded === 'object') {
      const embeddedValues = Object.values(res._embedded);
      for (const value of embeddedValues) {
        if (Array.isArray(value)) {
          return value;
        }
      }
    }

    return [];
  }

  private normalizePedido(pedido: any): Pedido {
    const criadoEm = this.normalizeDateValue(pedido?.criadoEm ?? pedido?.data);

    return {
      ...pedido,
      criadoEm,
      data: criadoEm
    };
  }

  private normalizeDateValue(value: any): string {
    if (!value) {
      return '';
    }

    if (typeof value === 'string') {
      return value;
    }

    if (value instanceof Date) {
      return value.toISOString();
    }

    if (typeof value === 'object' && value.year && value.monthValue && value.dayOfMonth) {
      const date = new Date(
        value.year,
        value.monthValue - 1,
        value.dayOfMonth,
        value.hour ?? 0,
        value.minute ?? 0,
        value.second ?? 0,
        Math.floor((value.nano ?? 0) / 1000000)
      );
      return date.toISOString();
    }

    return '';
  }
}