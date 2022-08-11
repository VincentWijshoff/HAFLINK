## We start from a recent Python release based on a stripped-down Debian
FROM python:3-slim-bullseye
## let's add the source code in here
COPY pythonProject/ /pydata/
## Upgrade the pip package manager, as it complains otherwise
RUN pip3 install --upgrade pip
## Now let's install the requirements of our little application
WORKDIR /pydata
RUN pip3 install -e .
RUN pip3 install kafka-python
## And let's specify a startup command for docker containers based on this image.
CMD ["python3", "main.py"]
