#!/usr/bin/env python3
import subprocess
import os
import sys

# Path to the venv
venv_dir = os.path.join(os.path.dirname(__file__), 'venv')
python_in_venv = os.path.join(venv_dir, 'bin', 'python')
pip_in_venv = os.path.join(venv_dir, 'bin', 'pip')

# Check if venv exists
if not os.path.exists(venv_dir):
    print("Virtual environment not found. Creating one...")
    subprocess.run([sys.executable, '-m', 'venv', venv_dir], check=True)

    # Install requirements
    requirements_path = os.path.join(os.path.dirname(__file__), 'requirements.txt')
    print("Installing dependencies...")
    subprocess.run([pip_in_venv, 'install', '-r', requirements_path], check=True)

# Run the server with the venv's Python
main_script = os.path.join(os.path.dirname(__file__), 'server', 'main.py')
print(f"Starting MCP server from {main_script}...")
subprocess.run([python_in_venv, main_script], check=True)
