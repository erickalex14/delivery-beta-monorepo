from sqlalchemy import Column, String, Boolean, ForeignKey
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import declarative_base
from geoalchemy2 import Geography
import uuid

Base = declarative_base()

class DriverProfile(Base):
    __tablename__ = "driver_profiles"
    #Busca en el esquema identity
    __table_args__ = {"schema": "identity"}

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), nullable=False) # Ref: identity.users.id
    identification_number = Column(String)
    verification_status = Column(String) # Ej: PENDING, APPROVED

    #POSTGIS
    current_location = Column(Geography(geometry_type='POINT', srid=4326))

    #tABLA EXTRA PA SABER SI ESTA DISPONIBLE Y NO ASIGNAR PEDIDOS A ALGUIEN OCUPADO
    is_available = Column(Boolean, default=False)
