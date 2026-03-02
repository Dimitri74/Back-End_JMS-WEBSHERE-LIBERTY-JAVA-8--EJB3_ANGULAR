import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { AppComponent } from './app.component';
import { PedidoFormComponent } from './pedido-form.component';
import { PedidoListComponent } from './pedido-list.component';

@NgModule({
  declarations: [AppComponent, PedidoFormComponent, PedidoListComponent],
  imports: [BrowserModule, FormsModule, HttpClientModule],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule {}
