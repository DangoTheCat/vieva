#!/bin/sh
set -eu
# Bootstrap a NEW database only, using the existing SQL scripts in version order.
# Existing databases are not modified automatically.
for version in $(seq 0 14); do
    for script in /opt/vieva-schema/V${version}__*.sql; do
        [ -f "$script" ] || continue
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -f "$script"
    done
done
