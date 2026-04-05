from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession
from app.db.models import DriverProfile
from app.core.config import settings


class GeoEngine:

    @staticmethod
    async def find_nearby_available_drivers(
            session: AsyncSession,
            lat: float,
            lng: float,
            radius_km: float = settings.SEARCH_RADIUS_KM
    ) -> list:
        """
        Busca conductores disponibles en un radio específico usando PostGIS.
        Devuelve una lista de IDs de conductores ordenados por cercanía.
        """
        # PostGIS trabaja en metros cuando usa el tipo Geography
        radius_meters = radius_km * 1000

        # Creamos el punto espacial del cliente (SRID 4326)
        client_point = func.ST_SetSRID(func.ST_MakePoint(lng, lat), 4326)

        print(f"PostGIS: Buscando motos a {radius_km}km de Lat: {lat}, Lng: {lng}...")

        # Consulta de SQLAlchemy + GeoAlchemy2
        query = (
            select(DriverProfile.user_id, DriverProfile.current_location)
            .where(
                # 1. El conductor debe estar disponible
                DriverProfile.is_available == True,
                # 2. La ubicación debe estar dentro del radio de 2500 metros
                func.ST_DWithin(DriverProfile.current_location, client_point, radius_meters)
            )
            # 3. Ordenamos para que el primero de la lista sea el más cercano
            .order_by(func.ST_Distance(DriverProfile.current_location, client_point))
            # 4. Limitamos a los 10 más cercanos para no saturar memoria
            .limit(10)
        )

        result = await session.execute(query)
        drivers = result.all()

        if not drivers:
            print("No se encontraron conductores disponibles en esta zona.")
            return []

        print(f"Se encontraron {len(drivers)} conductores cerca. Iniciando algoritmo en cascada...")

        # Devolvemos solo los UUIDs de los conductores
        return [str(driver.user_id) for driver in drivers]


geo_engine = GeoEngine()