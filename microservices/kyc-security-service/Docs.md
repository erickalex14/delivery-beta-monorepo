kyc-security-service/
│
├── app/
│   ├── api/
│   │   └── routes.py             # Endpoints para recibir las fotos de la app
│   ├── core/
│   │   └── config.py             # Lector de variables de entorno
│   ├── services/
│   │   ├── ocr_engine.py         # Motor OpenCV + Tesseract (Cédula/Matrícula)
│   │   ├── biometric_engine.py   # Motor DeepFace (Match Selfie vs Cédula)
│   │   └── scraper_engine.py     # Playwright (Antecedentes Judiciales)
│   └── main.py                   # Punto de entrada de FastAPI
│
├── .env
└── requirements.txt