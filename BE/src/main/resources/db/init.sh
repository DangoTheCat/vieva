#!/bin/sh
set -eu
# Bootstrap a NEW database only, using the existing SQL scripts in version order.
# Existing databases are not modified automatically.
find /opt/vieva-schema -name 'V*__*.sql' | sort -V | while IFS= read -r script; do
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -f "$script"
done
