from pydantic import BaseModel
from typing import Optional

#Coordenadas geograficas
class Location(BaseModel):
    lat: float
    lng: float

#El JSON que enviara el core transaccional cuando se cree un pedido
class OrderCreatedPayload(BaseModel):
    order_id: str
    client_id: str
    order_type: str  # "RIDE" o "DELIVERY"
    origin: Location
    destination: Optional[Location] = None # Puede ser nulo si el cliente aún no decide dónde bajarse
    # Para despachos diferidos de comida
    merchant_id: Optional[str] = None
    estimated_prep_time_minutes: Optional[int] = 0