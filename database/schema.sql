-- HardTrack - esquema de la base de datos (MySQL 8.0)
-- Crea la base y todas las tablas en blanco. Uso:
--   mysql -u root -p -e "source database/schema.sql"
-- Es equivalente a lo que genera Hibernate (spring.jpa.hibernate.ddl-auto=update)
-- en el primer arranque; con este script la base puede crearse sin ejecutar la aplicacion.
-- Los datos iniciales (roles, estados, categorias, tipos de equipo y configuracion)
-- los carga la aplicacion al arrancar (DatosInicialesConfig).

CREATE DATABASE IF NOT EXISTS gestion_hardware CHARACTER SET utf8mb4;
USE gestion_hardware;

CREATE TABLE IF NOT EXISTS `rol` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `descripcion` varchar(200) DEFAULT NULL,
  `nombre` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK43kr6s7bts1wqfv43f7jd87kp` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sede` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activo` bit(1) NOT NULL,
  `ciudad` varchar(80) NOT NULL,
  `direccion` varchar(200) DEFAULT NULL,
  `nombre` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `area` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activo` bit(1) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `sede_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKcmofdi9vrhos8d7rk4fx7k3hh` (`sede_id`,`nombre`),
  CONSTRAINT `FK95bhvd5oq7pj3b6cyfyrmoveu` FOREIGN KEY (`sede_id`) REFERENCES `sede` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `estado_equipo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `color_hex` varchar(7) DEFAULT NULL,
  `descripcion` varchar(200) DEFAULT NULL,
  `nombre` varchar(50) NOT NULL,
  `orden` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKx3y7y5jivqwmtcf1d1ps4907` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tipo_equipo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activo` bit(1) NOT NULL DEFAULT b'1',
  `especificaciones_basicas` bit(1) NOT NULL DEFAULT b'1',
  `nombre` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKach6k5hor8u9wkr9jkd6l826b` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `categoria_falla` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(80) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKdrvr8eq2s61a908wnxl8hy5nl` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `configuracion_sistema` (
  `id` bigint NOT NULL,
  `firma_ultimas_alertas` text,
  `frecuencia_notificacion` varchar(20) NOT NULL,
  `ultimo_envio_automatico` datetime(6) DEFAULT NULL,
  `umbral_antiguedad_anios` int NOT NULL,
  `umbral_fallas_mes` int NOT NULL,
  `umbral_horas_indisponibilidad` decimal(8,2) NOT NULL,
  `umbral_meses_actualizacion` int NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `configuracion_sistema_chk_1` CHECK ((`umbral_antiguedad_anios` >= 1)),
  CONSTRAINT `configuracion_sistema_chk_2` CHECK ((`umbral_fallas_mes` >= 1)),
  CONSTRAINT `configuracion_sistema_chk_3` CHECK ((`umbral_meses_actualizacion` >= 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `usuario` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activo` bit(1) NOT NULL,
  `documento` varchar(20) DEFAULT NULL,
  `email` varchar(120) NOT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `nombre_completo` varchar(150) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `pendiente_aprobacion` bit(1) DEFAULT NULL,
  `reset_token` varchar(100) DEFAULT NULL,
  `reset_token_expira` datetime(6) DEFAULT NULL,
  `terminos_aceptados_en` datetime(6) DEFAULT NULL,
  `terminos_ip` varchar(45) DEFAULT NULL,
  `terminos_version` varchar(10) DEFAULT NULL,
  `area_id` bigint DEFAULT NULL,
  `rol_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5171l57faosmj8myawaucatdw` (`email`),
  UNIQUE KEY `UKsqdsrgo7yd5nlfxh382v44rj9` (`documento`),
  KEY `FKq69b03obyc0tcb84yv48lh62` (`area_id`),
  KEY `FKshkwj12wg6vkm6iuwhvcfpct8` (`rol_id`),
  CONSTRAINT `FKq69b03obyc0tcb84yv48lh62` FOREIGN KEY (`area_id`) REFERENCES `area` (`id`),
  CONSTRAINT `FKshkwj12wg6vkm6iuwhvcfpct8` FOREIGN KEY (`rol_id`) REFERENCES `rol` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `equipo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activo` bit(1) NOT NULL,
  `capacidad_gb` int DEFAULT NULL,
  `codigo_interno` varchar(30) NOT NULL,
  `fecha_compra` date NOT NULL,
  `fecha_puesta_operacion` date DEFAULT NULL,
  `fecha_ultima_actualizacion_so` date DEFAULT NULL,
  `marca` varchar(60) NOT NULL,
  `modelo` varchar(80) NOT NULL,
  `observaciones` text,
  `procesador` varchar(100) DEFAULT NULL,
  `ram_gb` int DEFAULT NULL,
  `serial` varchar(80) NOT NULL,
  `sistema_operativo` varchar(80) DEFAULT NULL,
  `tipo_almacenamiento` varchar(20) DEFAULT NULL,
  `area_id` bigint NOT NULL,
  `estado_equipo_id` bigint NOT NULL,
  `tipo_equipo_id` bigint DEFAULT NULL,
  `usuario_asignado_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgfvtls332u0b8txp05t9v8squ` (`codigo_interno`),
  UNIQUE KEY `UKri5kkdacjsmotaxj77cd6ckap` (`serial`),
  KEY `FKpmc74a4rbbthq0dm0926372yb` (`area_id`),
  KEY `FKigwtdv9j49tjf7m7n6u7dti21` (`estado_equipo_id`),
  KEY `FKtdseewpyhkudv1ft4hgrbbwtv` (`tipo_equipo_id`),
  KEY `FK3w5mvtyymur9myua0g7d4n4w5` (`usuario_asignado_id`),
  CONSTRAINT `FK3w5mvtyymur9myua0g7d4n4w5` FOREIGN KEY (`usuario_asignado_id`) REFERENCES `usuario` (`id`),
  CONSTRAINT `FKigwtdv9j49tjf7m7n6u7dti21` FOREIGN KEY (`estado_equipo_id`) REFERENCES `estado_equipo` (`id`),
  CONSTRAINT `FKpmc74a4rbbthq0dm0926372yb` FOREIGN KEY (`area_id`) REFERENCES `area` (`id`),
  CONSTRAINT `FKtdseewpyhkudv1ft4hgrbbwtv` FOREIGN KEY (`tipo_equipo_id`) REFERENCES `tipo_equipo` (`id`),
  CONSTRAINT `equipo_chk_1` CHECK ((`capacidad_gb` >= 1)),
  CONSTRAINT `equipo_chk_2` CHECK ((`ram_gb` >= 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `campo_tipo_equipo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activo` bit(1) NOT NULL,
  `nombre` varchar(60) NOT NULL,
  `obligatorio` bit(1) NOT NULL,
  `opciones` varchar(500) DEFAULT NULL,
  `tipo_dato` enum('FECHA','LISTA','NUMERO','TEXTO') NOT NULL,
  `tipo_equipo_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKg5xwskj1owx7xec3cb9apk9fk` (`tipo_equipo_id`,`nombre`),
  CONSTRAINT `FKowumrffvsodp0sric1xt6nnbs` FOREIGN KEY (`tipo_equipo_id`) REFERENCES `tipo_equipo` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `valor_campo_equipo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `valor` varchar(255) DEFAULT NULL,
  `campo_id` bigint NOT NULL,
  `equipo_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKajx57w1b49t3yfsgnmm7mywfa` (`equipo_id`,`campo_id`),
  KEY `FKpa7y2qp0vuntpusy8v2dxd6p2` (`campo_id`),
  CONSTRAINT `FKpa7y2qp0vuntpusy8v2dxd6p2` FOREIGN KEY (`campo_id`) REFERENCES `campo_tipo_equipo` (`id`),
  CONSTRAINT `FKpaqi3ov4ufjt6rlweu0mo2hi8` FOREIGN KEY (`equipo_id`) REFERENCES `equipo` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `incidencia` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comentario_usuario` text,
  `confirmacion_usuario` varchar(20) DEFAULT NULL,
  `descripcion` text NOT NULL,
  `estado` varchar(20) NOT NULL,
  `fecha_cierre` datetime(6) DEFAULT NULL,
  `fecha_fin_indisponibilidad` datetime(6) DEFAULT NULL,
  `fecha_inicio_indisponibilidad` datetime(6) DEFAULT NULL,
  `fecha_reporte` datetime(6) NOT NULL,
  `genera_indisponibilidad` bit(1) NOT NULL,
  `horas_indisponibilidad` decimal(8,2) DEFAULT NULL,
  `prioridad` varchar(20) NOT NULL,
  `solucion` text,
  `veces_rechazada` int NOT NULL,
  `categoria_falla_id` bigint NOT NULL,
  `equipo_id` bigint NOT NULL,
  `reportado_por_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK4ugayp0i95uh3o3r4f2by9h66` (`categoria_falla_id`),
  KEY `FKhkcb6uhvp3vcmrd4bjvnya8oc` (`equipo_id`),
  KEY `FKcc7pk4lxqxhchs1tak9f40voj` (`reportado_por_id`),
  CONSTRAINT `FK4ugayp0i95uh3o3r4f2by9h66` FOREIGN KEY (`categoria_falla_id`) REFERENCES `categoria_falla` (`id`),
  CONSTRAINT `FKcc7pk4lxqxhchs1tak9f40voj` FOREIGN KEY (`reportado_por_id`) REFERENCES `usuario` (`id`),
  CONSTRAINT `FKhkcb6uhvp3vcmrd4bjvnya8oc` FOREIGN KEY (`equipo_id`) REFERENCES `equipo` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `mantenimiento` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actividades_realizadas` text,
  `estado` varchar(20) NOT NULL,
  `fecha_ejecucion` date DEFAULT NULL,
  `fecha_programada` date DEFAULT NULL,
  `observaciones` text,
  `resultado` varchar(30) DEFAULT NULL,
  `tipo` varchar(20) NOT NULL,
  `equipo_id` bigint NOT NULL,
  `incidencia_id` bigint DEFAULT NULL,
  `tecnico_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKb3mw9re5hillel959ddlhjwy2` (`equipo_id`),
  KEY `FK8ss53vx8wu0ufhke9i2jni8c7` (`incidencia_id`),
  KEY `FKd0fkvt72ktuvf7ohs8tt99cmf` (`tecnico_id`),
  CONSTRAINT `FK8ss53vx8wu0ufhke9i2jni8c7` FOREIGN KEY (`incidencia_id`) REFERENCES `incidencia` (`id`),
  CONSTRAINT `FKb3mw9re5hillel959ddlhjwy2` FOREIGN KEY (`equipo_id`) REFERENCES `equipo` (`id`),
  CONSTRAINT `FKd0fkvt72ktuvf7ohs8tt99cmf` FOREIGN KEY (`tecnico_id`) REFERENCES `usuario` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
