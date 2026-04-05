import { Controller } from '@nestjs/common';
import { AppService } from './app.service';
import { EventPattern, Payload, Ctx, RmqContext } from '@nestjs/microservices';

@Controller()
export class AppController {
  constructor(private readonly appService: AppService) {}

  // 🎧 Este decorador le dice a Nest: "Cuando escuches el evento 'order.created', ejecuta esto"
  @EventPattern('order.created')
  async handleOrderCreated(@Payload() data: any) {
    console.log('Orden creada y enviada a RabbitMQ', data);
    // Aquí luego llamaremos a Resend o Firebase
  }

  @EventPattern('payment.success')
  async handlePaymentSuccess(@Payload() data: any) {
    console.log('Pago confirmado! Enviando factura a:', data.userEmail);
  }

  // Cuando Java grite "user.created", este método se dispara
  @EventPattern('user.created')
  async handleUserCreated(@Payload() data: any, @Ctx() context: RmqContext) {
    console.log('¡🔔 Nuevo usuario registrado en el sistema!', data);

    // Suponiendo que el JSON que manda Java tiene { email: "...", fullName: "..." }
    if (data.email && data.fullName) {
      await this.appService.sendWelcomeEmail(data.email, data.fullName);
    }

    // Confirmamos a RabbitMQ que ya procesamos el mensaje
    const channel = context.getChannelRef();
    const originalMsg = context.getMessage();
    channel.ack(originalMsg);
  }
}
