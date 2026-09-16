FROM python:3.13-alpine
WORKDIR /app
COPY incident_bot.py /app/incident_bot.py
USER 65534
ENTRYPOINT ["python", "/app/incident_bot.py"]
