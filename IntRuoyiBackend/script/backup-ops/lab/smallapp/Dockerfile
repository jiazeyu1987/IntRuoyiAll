# W05 supplies an observed immutable task-approved base image and the actual
# relocatable Linux private CPython runtime/dependencies; no network/global pip.
ARG BASE_IMAGE
FROM ${BASE_IMAGE}
COPY runtime/ /opt/smallapp/runtime/
COPY source/ /opt/smallapp/source/
USER 10001:10001
WORKDIR /opt/smallapp/source
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
ENTRYPOINT ["/opt/smallapp/runtime/bin/python", "-B", "/opt/smallapp/source/launch.py"]
CMD ["backend"]
