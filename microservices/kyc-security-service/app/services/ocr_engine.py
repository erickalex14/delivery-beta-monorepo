import cv2
import pytesseract
import logging
import numpy as np
from app.core.config import settings

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

# Es obligatorio indicar la ruta del binario de Tesseract para que pytesseract funcione correctamente
pytesseract.pytesseract.tesseract_cmd = settings.TESSERACT_CMD_PATH


class OCREngine:
    @staticmethod
    def extract_text_from_image(image_bytes: bytes) -> str:
        """
        Procesa una imagen en formato binario y extrae el texto utilizando Tesseract OCR.
        Aplica filtros de escala de grises y umbralizacion para maximizar la precision de lectura.
        """
        try:
            # 1. Convertir la secuencia de bytes a un arreglo matricial de Numpy para OpenCV
            nparr = np.frombuffer(image_bytes, np.uint8)
            image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

            if image is None:
                logger.error("Fallo critico: No se pudo decodificar la estructura de la imagen entrante.")
                return ""

            logger.info("Iniciando preprocesamiento de imagen para extraccion de texto.")

            # 2. Preprocesamiento: Convertir a escala de grises
            gray_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

            # 3. Preprocesamiento: Filtro de umbralizacion (Thresholding)
            # Esto convierte los grises en un contraste binario (blanco puro y negro puro)
            # facilitando la deteccion de caracteres por parte de la red neuronal de Tesseract.
            _, threshold_image = cv2.threshold(gray_image, 150, 255, cv2.THRESH_BINARY)

            logger.info("Ejecutando motor de Tesseract OCR sobre la imagen procesada.")

            # 4. Extraccion de texto configurando el idioma a espanol ('spa')
            extracted_text = pytesseract.image_to_string(threshold_image, lang='spa')

            logger.info("Extraccion de texto completada exitosamente.")

            return extracted_text.strip()

        except Exception as e:
            logger.error(f"Error inesperado durante el procesamiento OCR: {str(e)}")
            return ""


ocr_engine = OCREngine()