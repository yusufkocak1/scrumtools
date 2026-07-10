#!/bin/sh
# Ilk acilista uygulama semasini olusturur (docker-entrypoint-initdb.d).
# Sadece bos data volume ile ilk baslatmada calisir.
set -e

psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" <<EOF
CREATE SCHEMA IF NOT EXISTS ${DB_SCHEMA:-scrumtools} AUTHORIZATION $POSTGRES_USER;
EOF
