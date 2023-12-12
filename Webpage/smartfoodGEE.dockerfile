FROM python:3.10.2-bullseye

LABEL Khaos Research Group <khaos.uma.es>

RUN mkdir -p credentials
RUN pip install --upgrade pip
ENV PIP_ROOT_USER_ACTION=ignore
RUN curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-cli-410.0.0-linux-x86_64.tar.gz
RUN tar -xf google-cloud-cli-410.0.0-linux-x86_64.tar.gz
RUN ./google-cloud-sdk/install.sh --quiet --usage-reporting=False
ENV PATH=$PATH:/google-cloud-sdk/bin
ENV SCRIPT_NAME=/smartfood-GEE

COPY requirements.txt . 

RUN pip install -r requirements.txt
RUN pip install gunicorn

COPY ./static static
COPY ./templates templates
COPY config.py config.py 
COPY main.py main.py 
COPY utils.py utils.py 
COPY oauth.py oauth.py 

ENTRYPOINT ["gunicorn", "--bind=0.0.0.0:8083", "--timeout=0", "main:app"]

