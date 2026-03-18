#!/bin/bash
# 1. Crear el entorno virtual
python3 -m venv venv

# 2. Activarlo (Linux/Mac)
source venv/bin/activate

# 3. Instalar dependencias
pip install -r requirements.txt

echo "Setup complete. The virtual environment is active and dependencies are installed."
