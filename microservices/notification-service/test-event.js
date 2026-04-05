const amqp = require('amqplib');
require('dotenv').config();

async function fireEvent() {
  try {
    // Nos conectamos usando la misma URL de tu .env
    const connection = await amqp.connect(process.env.RABBITMQ_URL);
    const channel = await connection.createChannel();

    const queue = 'notifications_queue';
    await channel.assertQueue(queue, { durable: true });

    // MAGIA DE NESTJS: Este es el formato exacto que Nest espera recibir
    const nestJsMessage = {
      pattern: 'user.created', // Tiene que coincidir con el @EventPattern de tu controller
      data: {
        email: 'tuemail@gmail.com',
        fullName: 'Nuevo Conductor Élite',
      },
    };

    // Convertimos el JSON a Buffer y lo disparamos a la cola
    channel.sendToQueue(queue, Buffer.from(JSON.stringify(nestJsMessage)));
    console.log(`¡PUM! Evento 'user.created' inyectado en RabbitMQ.`);

    // Cerramos la conexión después de medio segundo
    setTimeout(() => {
      connection.close();
      process.exit(0);
    }, 500);
  } catch (error) {
    console.error('Error inyectando el evento:', error.message);
  }
}

fireEvent();
