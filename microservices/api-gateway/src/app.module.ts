import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config'; // 🔐 Importamos el módulo
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { ThrottlerModule, ThrottlerGuard } from '@nestjs/throttler';
import { APP_GUARD } from '@nestjs/core';

@Module({
  imports: [
    // 1. Cargamos las variables de entorno de forma global
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    // 2. Nuestro Rate Limit
    ThrottlerModule.forRoot([
      {
        ttl: 60000,
        limit: 100,
      },
    ]),
  ],
  controllers: [AppController],
  providers: [
    AppService,
    {
      provide: APP_GUARD,
      useClass: ThrottlerGuard,
    },
  ],
})
export class AppModule {}
