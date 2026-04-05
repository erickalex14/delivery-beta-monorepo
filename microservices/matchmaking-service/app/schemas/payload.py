from pydantic import BaseModel
from typing import Optional

#Coordenadas geograficas
class Location(BaseModel):
    lat: float
    lng: float
