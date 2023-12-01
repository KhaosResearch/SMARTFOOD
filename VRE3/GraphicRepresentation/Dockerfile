FROM python:3.11.5-bullseye

LABEL Khaos Research Group <khaos.uma.es>

COPY LICENSE ACKNOWLEDGEMENTS /mnt/shared/

WORKDIR /usr/src/app

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ENTRYPOINT ["python", "main.py"]
