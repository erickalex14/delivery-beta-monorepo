import { Injectable, Logger } from '@nestjs/common';
import { Resend } from 'resend';

@Injectable()
export class AppService {
  private resend: Resend;
  private readonly logger = new Logger(AppService.name);

  constructor() {
    // Inicializamos Resend con la llave de tu .env
    this.resend = new Resend(process.env.RESEND_API_KEY);
  }

  // Corregido: la "s" inicial en minúscula (camelCase)
  async sendWelcomeEmail(userEmail: string, userName: string) {
    try {
      this.logger.log(`Intentando enviar correo de bienvenida a: ${userEmail}`);

      // Corregido: Extraemos { data, error } según la nueva versión del SDK de Resend
      const { data, error } = await this.resend.emails.send({
        from: 'Nebula App <onboarding@resend.dev>', // Correo de prueba de Resend
        to: userEmail,
        subject: '¡Bienvenido a la SuperApp, conductor!',
        html: `
          <div>
            <h2>¡Hola ${userName}! </h2>
            <p>Tu cuenta ha sido creada exitosamente. Tu billetera virtual está lista para recibir recargas.</p>
            <p>Prepárate para dominar las calles de Manabí.</p>
          </div>
        `,
      });

      // Si Resend nos devuelve un error interno, lo atrapamos aquí
      if (error) {
        this.logger.error(`Error de la API de Resend: ${error.message}`);
        return;
      }

      // Si todo sale bien, leemos el ID desde 'data' usando el operador de seguridad (?)
      this.logger.log(`Correo enviado exitosamente. ID: ${data?.id}`);
      return data;
    } catch (error) {
      // Esto atrapa errores a nivel de red o caída del servidor
      this.logger.error(`Excepción crítica al enviar correo: ${error.message}`);
    }
  }
}
