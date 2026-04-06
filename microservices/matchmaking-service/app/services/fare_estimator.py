import logging
from sqlalchemy import select, func
from sqlalchemy.engine import result
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import query

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

class FareEstimator:
    # Estos valores podrían venir del .env o de la base de datos en el futuro
    BASE_FARE_USD = 1.25
    RATE_PER_KM_USD = 0.50

    @staticmethod
    async def calculate_trip_fare(session: AsyncSession, origin_lat: float, origin_lng: float, dest_lat: float, dest_lng: float) -> float:
        """
        Calcula la tarifa usando postgis
        """
        try:
            logger.info(f"Calculando dare para las coordenadas: Origin({origin_lat},{origin_lng}) al destino({dest_lat},{dest_lng})")

            # Construimos los puntos geográficos en formato WKT (Well-Known Text) para PostGIS
            origin_wkt = f'SRID=4326;POINT({origin_lng} {origin_lat})'
            dest_wkt = f'SRID=4326;POINT({dest_lng} {dest_lat})'

            # Ejecutamos ST_Distance casteando los puntos a tipo Geography para obtener metros precisos
            query = select(func.ST_Distance(
                func.ST_GeographyFromText(origin_wkt),
                func.ST_GeographyFromText(dest_wkt)
            ))

            result = await session.execute(query)
            distance_meters = result.scalar()

            if distance_meters is None:
                logger.warning("PostGIS Devolvio una distancia nula. Volviendo a la tarifa base")
                return FareEstimator.BASE_FARE_USD

            distance_km = distance_meters / 1000.0
            total_fare = FareEstimator.BASE_FARE_USD + (distance_km * FareEstimator.RATE_PER_KM_USD)

            #Redondea a 2 decimales
            final_fare = round(total_fare, 2)

            logger.info(f"Calculo exitoso. Distancia: {distance_km:.2f}km, Precio estimado: ${final_fare}")
            return final_fare

        except Exception as e:
            logger.error(f"Error calculando fare: {str(e)}")
            # Fallback de seguridad en caso de error en la base de datos
            return FareEstimator.BASE_FARE_USD

fare_estimator = FareEstimator()