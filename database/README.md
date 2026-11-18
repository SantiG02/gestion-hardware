# Base de datos

## Crear la base desde cero

Desde la raiz del proyecto:

    mysql -u root -p -e "source database/schema.sql"

Crea la base `gestion_hardware` con sus 13 tablas vacias. Al arrancar, la aplicacion
carga los datos iniciales (roles, estados, categorias, tipos de equipo y configuracion).
Las cuentas de prueba solo se crean con el perfil `demo`; en `prod` el primer
administrador se crea desde la pantalla de configuracion inicial.

La aplicacion usa `spring.jpa.hibernate.ddl-auto=update`: si la base ya existe, agrega las
tablas y columnas nuevas al arrancar, pero no elimina ni modifica las existentes. Un
cambio destructivo (borrar o renombrar una columna) se hace a mano con `ALTER TABLE`.

## Respaldo

    mysqldump -u root -p --single-transaction --routines --result-file=respaldo_AAAA-MM-DD.sql gestion_hardware

(`--result-file` evita el problema de codificacion de `>` en PowerShell.)

## Restaurar un respaldo

    mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS gestion_hardware CHARACTER SET utf8mb4"
    mysql -u root -p gestion_hardware -e "source respaldo_AAAA-MM-DD.sql"

Detener la aplicacion antes de restaurar sobre una base con datos. Comprobar despues
con `SELECT COUNT(*) FROM equipo;` y `SELECT COUNT(*) FROM usuario;`.
